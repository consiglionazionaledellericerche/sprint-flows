package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Blacklist;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.BlacklistRepository;
import it.cnr.si.service.BlacklistService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the BlacklistResource REST controller.
 *
 * @see BlacklistResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class)
public class BlacklistResourceIntTest {
    private static final String DEFAULT_EMAIL = "AAAAA";
    private static final String UPDATED_EMAIL = "BBBBB";
    private static final String DEFAULT_PROCESS_DEFINITION_KEY = "AAAAA";
    private static final String UPDATED_PROCESS_DEFINITION_KEY = "BBBBB";

    @Inject
    private BlacklistRepository blacklistRepository;

    @Inject
    private BlacklistService blacklistService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restBlacklistMockMvc;

    private Blacklist blacklist;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        BlacklistResource blacklistResource = new BlacklistResource();
        ReflectionTestUtils.setField(blacklistResource, "blacklistService", blacklistService);
        this.restBlacklistMockMvc = MockMvcBuilders.standaloneSetup(blacklistResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Blacklist createEntity(EntityManager em) {
        Blacklist blacklist = new Blacklist();
        blacklist = new Blacklist()
                .email(DEFAULT_EMAIL)
                .processDefinitionKey(DEFAULT_PROCESS_DEFINITION_KEY);
        return blacklist;
    }

    @Before
    public void initTest() {
        blacklist = createEntity(em);
    }

    @Test
    @Transactional
    public void createBlacklist() throws Exception {
        int databaseSizeBeforeCreate = blacklistRepository.findAll().size();

        // Create the Blacklist

        restBlacklistMockMvc.perform(post("/api/blacklists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(blacklist)))
                .andExpect(status().isCreated());

        // Validate the Blacklist in the database
        List<Blacklist> blacklists = blacklistRepository.findAll();
        assertThat(blacklists).hasSize(databaseSizeBeforeCreate + 1);
        Blacklist testBlacklist = blacklists.get(blacklists.size() - 1);
        assertThat(testBlacklist.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testBlacklist.getProcessDefinitionKey()).isEqualTo(DEFAULT_PROCESS_DEFINITION_KEY);
    }

    @Test
    @Transactional
    public void checkEmailIsRequired() throws Exception {
        int databaseSizeBeforeTest = blacklistRepository.findAll().size();
        // set the field null
        blacklist.setEmail(null);

        // Create the Blacklist, which fails.

        restBlacklistMockMvc.perform(post("/api/blacklists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(blacklist)))
                .andExpect(status().isBadRequest());

        List<Blacklist> blacklists = blacklistRepository.findAll();
        assertThat(blacklists).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkProcessDefinitionKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = blacklistRepository.findAll().size();
        // set the field null
        blacklist.setProcessDefinitionKey(null);

        // Create the Blacklist, which fails.

        restBlacklistMockMvc.perform(post("/api/blacklists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(blacklist)))
                .andExpect(status().isBadRequest());

        List<Blacklist> blacklists = blacklistRepository.findAll();
        assertThat(blacklists).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllBlacklists() throws Exception {
        // Initialize the database
        blacklistRepository.saveAndFlush(blacklist);

        // Get all the blacklists
        restBlacklistMockMvc.perform(get("/api/blacklists?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(blacklist.getId().intValue())))
                .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL.toString())))
                .andExpect(jsonPath("$.[*].processDefinitionKey").value(hasItem(DEFAULT_PROCESS_DEFINITION_KEY.toString())));
    }

    @Test
    @Transactional
    public void getBlacklist() throws Exception {
        // Initialize the database
        blacklistRepository.saveAndFlush(blacklist);

        // Get the blacklist
        restBlacklistMockMvc.perform(get("/api/blacklists/{id}", blacklist.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(blacklist.getId().intValue()))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL.toString()))
            .andExpect(jsonPath("$.processDefinitionKey").value(DEFAULT_PROCESS_DEFINITION_KEY.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingBlacklist() throws Exception {
        // Get the blacklist
        restBlacklistMockMvc.perform(get("/api/blacklists/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateBlacklist() throws Exception {
        // Initialize the database
        blacklistService.save(blacklist);

        int databaseSizeBeforeUpdate = blacklistRepository.findAll().size();

        // Update the blacklist
        Blacklist updatedBlacklist = blacklistRepository.findById(blacklist.getId()).get();
        updatedBlacklist
                .email(UPDATED_EMAIL)
                .processDefinitionKey(UPDATED_PROCESS_DEFINITION_KEY);

        restBlacklistMockMvc.perform(put("/api/blacklists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedBlacklist)))
                .andExpect(status().isOk());

        // Validate the Blacklist in the database
        List<Blacklist> blacklists = blacklistRepository.findAll();
        assertThat(blacklists).hasSize(databaseSizeBeforeUpdate);
        Blacklist testBlacklist = blacklists.get(blacklists.size() - 1);
        assertThat(testBlacklist.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testBlacklist.getProcessDefinitionKey()).isEqualTo(UPDATED_PROCESS_DEFINITION_KEY);
    }

    @Test
    @Transactional
    public void deleteBlacklist() throws Exception {
        // Initialize the database
        blacklistService.save(blacklist);

        int databaseSizeBeforeDelete = blacklistRepository.findAll().size();

        // Get the blacklist
        restBlacklistMockMvc.perform(delete("/api/blacklists/{id}", blacklist.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Blacklist> blacklists = blacklistRepository.findAll();
        assertThat(blacklists).hasSize(databaseSizeBeforeDelete - 1);
    }
}
