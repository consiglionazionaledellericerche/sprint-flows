package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Dynamiclist;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.DynamiclistRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the DynamiclistResource REST controller.
 *
 * @see DynamiclistResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class DynamiclistResourceIntTest {
    private static final String DEFAULT_NAME = "AAAAA";
    private static final String UPDATED_NAME = "BBBBB";
    private static final String DEFAULT_LISTJSON = "AAAAA";
    private static final String UPDATED_LISTJSON = "BBBBB";

    @Inject
    private DynamiclistRepository dynamiclistRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restDynamiclistMockMvc;

    private Dynamiclist dynamiclist;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        DynamiclistResource dynamiclistResource = new DynamiclistResource();
        ReflectionTestUtils.setField(dynamiclistResource, "dynamiclistRepository", dynamiclistRepository);
        this.restDynamiclistMockMvc = MockMvcBuilders.standaloneSetup(dynamiclistResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Dynamiclist createEntity(EntityManager em) {
        Dynamiclist dynamiclist = new Dynamiclist();
        dynamiclist = new Dynamiclist()
                .name(DEFAULT_NAME)
                .listjson(DEFAULT_LISTJSON);
        return dynamiclist;
    }

    @Before
    public void initTest() {
        dynamiclist = createEntity(em);
    }

    @Test
    @Transactional
    public void createDynamiclist() throws Exception {
        int databaseSizeBeforeCreate = dynamiclistRepository.findAll().size();

        // Create the Dynamiclist

        restDynamiclistMockMvc.perform(post("/api/dynamiclists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dynamiclist)))
                .andExpect(status().isCreated());

        // Validate the Dynamiclist in the database
        List<Dynamiclist> dynamiclists = dynamiclistRepository.findAll();
        assertThat(dynamiclists).hasSize(databaseSizeBeforeCreate + 1);
        Dynamiclist testDynamiclist = dynamiclists.get(dynamiclists.size() - 1);
        assertThat(testDynamiclist.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testDynamiclist.getListjson()).isEqualTo(DEFAULT_LISTJSON);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = dynamiclistRepository.findAll().size();
        // set the field null
        dynamiclist.setName(null);

        // Create the Dynamiclist, which fails.

        restDynamiclistMockMvc.perform(post("/api/dynamiclists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dynamiclist)))
                .andExpect(status().isBadRequest());

        List<Dynamiclist> dynamiclists = dynamiclistRepository.findAll();
        assertThat(dynamiclists).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkListjsonIsRequired() throws Exception {
        int databaseSizeBeforeTest = dynamiclistRepository.findAll().size();
        // set the field null
        dynamiclist.setListjson(null);

        // Create the Dynamiclist, which fails.

        restDynamiclistMockMvc.perform(post("/api/dynamiclists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dynamiclist)))
                .andExpect(status().isBadRequest());

        List<Dynamiclist> dynamiclists = dynamiclistRepository.findAll();
        assertThat(dynamiclists).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllDynamiclists() throws Exception {
        // Initialize the database
        dynamiclistRepository.saveAndFlush(dynamiclist);

        // Get all the dynamiclists
        restDynamiclistMockMvc.perform(get("/api/dynamiclists?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(dynamiclist.getId().intValue())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
                .andExpect(jsonPath("$.[*].listjson").value(hasItem(DEFAULT_LISTJSON.toString())));
    }

    @Test
    @Transactional
    public void getDynamiclist() throws Exception {
        // Initialize the database
        dynamiclistRepository.saveAndFlush(dynamiclist);

        // Get the dynamiclist
        restDynamiclistMockMvc.perform(get("/api/dynamiclists/{id}", dynamiclist.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(dynamiclist.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.listjson").value(DEFAULT_LISTJSON.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingDynamiclist() throws Exception {
        // Get the dynamiclist
        restDynamiclistMockMvc.perform(get("/api/dynamiclists/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateDynamiclist() throws Exception {
        // Initialize the database
        dynamiclistRepository.saveAndFlush(dynamiclist);
        int databaseSizeBeforeUpdate = dynamiclistRepository.findAll().size();

        // Update the dynamiclist
        Dynamiclist updatedDynamiclist = dynamiclistRepository.findById(dynamiclist.getId()).get();
        updatedDynamiclist
                .name(UPDATED_NAME)
                .listjson(UPDATED_LISTJSON);

        restDynamiclistMockMvc.perform(put("/api/dynamiclists")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedDynamiclist)))
                .andExpect(status().isOk());

        // Validate the Dynamiclist in the database
        List<Dynamiclist> dynamiclists = dynamiclistRepository.findAll();
        assertThat(dynamiclists).hasSize(databaseSizeBeforeUpdate);
        Dynamiclist testDynamiclist = dynamiclists.get(dynamiclists.size() - 1);
        assertThat(testDynamiclist.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testDynamiclist.getListjson()).isEqualTo(UPDATED_LISTJSON);
    }

    @Test
    @Transactional
    public void deleteDynamiclist() throws Exception {
        // Initialize the database
        dynamiclistRepository.saveAndFlush(dynamiclist);
        int databaseSizeBeforeDelete = dynamiclistRepository.findAll().size();

        // Get the dynamiclist
        restDynamiclistMockMvc.perform(delete("/api/dynamiclists/{id}", dynamiclist.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Dynamiclist> dynamiclists = dynamiclistRepository.findAll();
        assertThat(dynamiclists).hasSize(databaseSizeBeforeDelete - 1);
    }
}
