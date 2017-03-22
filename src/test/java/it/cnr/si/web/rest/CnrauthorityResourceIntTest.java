package it.cnr.si.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Cnrauthority;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.CnrauthorityRepository;
import it.cnr.si.service.CnrauthorityService;

/**
 * Test class for the CnrauthorityResource REST controller.
 *
 * @see CnrauthorityResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class)
public class CnrauthorityResourceIntTest {
    private static final String DEFAULT_DISPLAY_NAME = "AAAAA";
    private static final String UPDATED_DISPLAY_NAME = "BBBBB";
    private static final String DEFAULT_NAME = "AAAAA";
    private static final String UPDATED_NAME = "BBBBB";

    @Inject
    private CnrauthorityRepository cnrauthorityRepository;

    @Inject
    private CnrauthorityService cnrauthorityService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restCnrauthorityMockMvc;

    private Cnrauthority cnrauthority;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Cnrauthority createEntity(EntityManager em) {
        Cnrauthority cnrauthority = new Cnrauthority();
        cnrauthority = new Cnrauthority()
                .display_name(DEFAULT_DISPLAY_NAME)
                .name(DEFAULT_NAME);
        return cnrauthority;
    }

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        CnrauthorityResource cnrauthorityResource = new CnrauthorityResource();
        ReflectionTestUtils.setField(cnrauthorityResource, "cnrauthorityService", cnrauthorityService);
        this.restCnrauthorityMockMvc = MockMvcBuilders.standaloneSetup(cnrauthorityResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        cnrauthority = createEntity(em);
    }

    @Test
    @Transactional
    public void createCnrauthority() throws Exception {
        int databaseSizeBeforeCreate = cnrauthorityRepository.findAll().size();

        // Create the Cnrauthority

        restCnrauthorityMockMvc.perform(post("/api/cnrauthorities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(cnrauthority)))
                .andExpect(status().isCreated());

        // Validate the Cnrauthority in the database
        List<Cnrauthority> cnrauthorities = cnrauthorityRepository.findAll();
        assertThat(cnrauthorities).hasSize(databaseSizeBeforeCreate + 1);
        Cnrauthority testCnrauthority = cnrauthorities.get(cnrauthorities.size() - 1);
        assertThat(testCnrauthority.getDisplay_name()).isEqualTo(DEFAULT_DISPLAY_NAME);
        assertThat(testCnrauthority.getName()).isEqualTo(DEFAULT_NAME);
    }

    @Test
    @Transactional
    public void getAllCnrauthorities() throws Exception {
        // Initialize the database
        cnrauthorityRepository.saveAndFlush(cnrauthority);

        // Get all the cnrauthorities
        restCnrauthorityMockMvc.perform(get("/api/cnrauthorities?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(cnrauthority.getId().intValue())))
                .andExpect(jsonPath("$.[*].display_name").value(hasItem(DEFAULT_DISPLAY_NAME.toString())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())));
    }

    @Test
    @Transactional
    public void getCnrauthority() throws Exception {
        // Initialize the database
        cnrauthorityRepository.saveAndFlush(cnrauthority);

        // Get the cnrauthority
        restCnrauthorityMockMvc.perform(get("/api/cnrauthorities/{id}", cnrauthority.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(cnrauthority.getId().intValue()))
            .andExpect(jsonPath("$.display_name").value(DEFAULT_DISPLAY_NAME.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingCnrauthority() throws Exception {
        // Get the cnrauthority
        restCnrauthorityMockMvc.perform(get("/api/cnrauthorities/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCnrauthority() throws Exception {
        // Initialize the database
        cnrauthorityService.save(cnrauthority);

        int databaseSizeBeforeUpdate = cnrauthorityRepository.findAll().size();

        // Update the cnrauthority
        Cnrauthority updatedCnrauthority = cnrauthorityRepository.findOne(cnrauthority.getId());
        updatedCnrauthority
                .display_name(UPDATED_DISPLAY_NAME)
                .name(UPDATED_NAME);

        restCnrauthorityMockMvc.perform(put("/api/cnrauthorities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedCnrauthority)))
                .andExpect(status().isOk());

        // Validate the Cnrauthority in the database
        List<Cnrauthority> cnrauthorities = cnrauthorityRepository.findAll();
        assertThat(cnrauthorities).hasSize(databaseSizeBeforeUpdate);
        Cnrauthority testCnrauthority = cnrauthorities.get(cnrauthorities.size() - 1);
        assertThat(testCnrauthority.getDisplay_name()).isEqualTo(UPDATED_DISPLAY_NAME);
        assertThat(testCnrauthority.getName()).isEqualTo(UPDATED_NAME);
    }

    @Test
    @Transactional
    public void deleteCnrauthority() throws Exception {
        // Initialize the database
        cnrauthorityService.save(cnrauthority);

        int databaseSizeBeforeDelete = cnrauthorityRepository.findAll().size();

        // Get the cnrauthority
        restCnrauthorityMockMvc.perform(delete("/api/cnrauthorities/{id}", cnrauthority.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Cnrauthority> cnrauthorities = cnrauthorityRepository.findAll();
        assertThat(cnrauthorities).hasSize(databaseSizeBeforeDelete - 1);
    }
}
