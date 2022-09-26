package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Cnrgroup;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.CnrgroupRepository;
import it.cnr.si.service.CnrgroupService;
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
 * Test class for the CnrgroupResource REST controller.
 *
 * @see CnrgroupResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
//@Ignore
public class CnrgroupResourceIntTest {
    private static final String DEFAULT_NAME = "AAAAA";
    private static final String UPDATED_NAME = "BBBBB";
    private static final String DEFAULT_DISPLAY_NAME = "AAAAA";
    private static final String UPDATED_DISPLAY_NAME = "BBBBB";

    @Inject
    private CnrgroupRepository cnrgroupRepository;

    @Inject
    private CnrgroupService cnrgroupService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restCnrgroupMockMvc;

    private Cnrgroup cnrgroup;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        CnrgroupResource cnrgroupResource = new CnrgroupResource();
        ReflectionTestUtils.setField(cnrgroupResource, "cnrgroupService", cnrgroupService);
        this.restCnrgroupMockMvc = MockMvcBuilders.standaloneSetup(cnrgroupResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Cnrgroup createEntity(EntityManager em) {
        Cnrgroup cnrgroup = new Cnrgroup();
        cnrgroup = new Cnrgroup()
                .name(DEFAULT_NAME)
                .displayName(DEFAULT_DISPLAY_NAME);
        return cnrgroup;
    }

    @Before
    public void initTest() {
        cnrgroup = createEntity(em);
    }

    @Test
    @Transactional
    public void createCnrgroup() throws Exception {
        int databaseSizeBeforeCreate = cnrgroupRepository.findAll().size();

        // Create the Cnrgroup

        restCnrgroupMockMvc.perform(post("/api/cnrgroups")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(cnrgroup)))
                .andExpect(status().isCreated());

        // Validate the Cnrgroup in the database
        List<Cnrgroup> cnrgroups = cnrgroupRepository.findAll();
        assertThat(cnrgroups).hasSize(databaseSizeBeforeCreate + 1);
        Cnrgroup testCnrgroup = cnrgroups.get(cnrgroups.size() - 1);
        assertThat(testCnrgroup.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testCnrgroup.getDisplayName()).isEqualTo(DEFAULT_DISPLAY_NAME);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = cnrgroupRepository.findAll().size();
        // set the field null
        cnrgroup.setName(null);

        // Create the Cnrgroup, which fails.

        restCnrgroupMockMvc.perform(post("/api/cnrgroups")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(cnrgroup)))
                .andExpect(status().isBadRequest());

        List<Cnrgroup> cnrgroups = cnrgroupRepository.findAll();
        assertThat(cnrgroups).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDisplayNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = cnrgroupRepository.findAll().size();
        // set the field null
        cnrgroup.setDisplayName(null);

        // Create the Cnrgroup, which fails.

        restCnrgroupMockMvc.perform(post("/api/cnrgroups")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(cnrgroup)))
                .andExpect(status().isBadRequest());

        List<Cnrgroup> cnrgroups = cnrgroupRepository.findAll();
        assertThat(cnrgroups).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllCnrgroups() throws Exception {
        // Initialize the database
        cnrgroupRepository.saveAndFlush(cnrgroup);

        // Get all the cnrgroups
        restCnrgroupMockMvc.perform(get("/api/cnrgroups?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(cnrgroup.getId().intValue())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
                .andExpect(jsonPath("$.[*].displayName").value(hasItem(DEFAULT_DISPLAY_NAME.toString())));
    }

    @Test
    @Transactional
    public void getCnrgroup() throws Exception {
        // Initialize the database
        cnrgroupRepository.saveAndFlush(cnrgroup);

        // Get the cnrgroup
        restCnrgroupMockMvc.perform(get("/api/cnrgroups/{id}", cnrgroup.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(cnrgroup.getId().intValue()))
                .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
                .andExpect(jsonPath("$.displayName").value(DEFAULT_DISPLAY_NAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingCnrgroup() throws Exception {
        // Get the cnrgroup
        restCnrgroupMockMvc.perform(get("/api/cnrgroups/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCnrgroup() throws Exception {
        // Initialize the database
        cnrgroupService.save(cnrgroup);

        int databaseSizeBeforeUpdate = cnrgroupRepository.findAll().size();

        // Update the cnrgroup
        Cnrgroup updatedCnrgroup = cnrgroupRepository.findById(cnrgroup.getId()).get();
        updatedCnrgroup
                .name(UPDATED_NAME)
                .displayName(UPDATED_DISPLAY_NAME);

        restCnrgroupMockMvc.perform(put("/api/cnrgroups")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedCnrgroup)))
                .andExpect(status().isOk());

        // Validate the Cnrgroup in the database
        List<Cnrgroup> cnrgroups = cnrgroupRepository.findAll();
        assertThat(cnrgroups).hasSize(databaseSizeBeforeUpdate);
        Cnrgroup testCnrgroup = cnrgroups.get(cnrgroups.size() - 1);
        assertThat(testCnrgroup.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testCnrgroup.getDisplayName()).isEqualTo(UPDATED_DISPLAY_NAME);
    }

    @Test
    @Transactional
    public void deleteCnrgroup() throws Exception {
        // Initialize the database
        cnrgroupService.save(cnrgroup);

        int databaseSizeBeforeDelete = cnrgroupRepository.findAll().size();

        // Get the cnrgroup
        restCnrgroupMockMvc.perform(delete("/api/cnrgroups/{id}", cnrgroup.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Cnrgroup> cnrgroups = cnrgroupRepository.findAll();
        assertThat(cnrgroups).hasSize(databaseSizeBeforeDelete - 1);
    }
}
