package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Counter;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.repository.CounterRepository;
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
 * Test class for the CounterResource REST controller.
 *
 * @see CounterResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class CounterResourceIntTest {
    private static final String DEFAULT_NAME = "AAAAA";
    private static final String UPDATED_NAME = "BBBBB";

    private static final Long DEFAULT_VALUE = 1L;
    private static final Long UPDATED_VALUE = 2L;

    @Inject
    private CounterRepository counterRepository;
    @Inject
    private CounterService counterService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restCounterMockMvc;

    private Counter counter;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Counter createEntity(EntityManager em) {
        Counter counter = new Counter();
        counter = new Counter()
                .name(DEFAULT_NAME)
                .value(DEFAULT_VALUE);
        return counter;
    }

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        CounterResource counterResource = new CounterResource();
        ReflectionTestUtils.setField(counterResource, "counterService", counterService);
        this.restCounterMockMvc = MockMvcBuilders.standaloneSetup(counterResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        counter = createEntity(em);
    }

    @Test
    @Transactional
    public void createCounter() throws Exception {
        int databaseSizeBeforeCreate = counterRepository.findAll().size();

        // Create the Counter

        restCounterMockMvc.perform(post("/api/counters")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(counter)))
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Transactional
    public void getAllCounters() throws Exception {
        // Initialize the database
        counterRepository.saveAndFlush(counter);

        // Get all the counters
        restCounterMockMvc.perform(get("/api/counters?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(counter.getId().intValue())))
                .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
                .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE.intValue())));
    }

    @Test
    @Transactional
    public void getCounter() throws Exception {
        // Initialize the database
        counterRepository.saveAndFlush(counter);

        // Get the counter
        restCounterMockMvc.perform(get("/api/counters/{id}", counter.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void getNonExistingCounter() throws Exception {
        // Get the counter
        restCounterMockMvc.perform(get("/api/counters/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCounter() throws Exception {
        // Initialize the database
        counterRepository.saveAndFlush(counter);
        int databaseSizeBeforeUpdate = counterRepository.findAll().size();

        // Update the counter
        Counter updatedCounter = counterRepository.findById(counter.getId()).get();
        updatedCounter
                .name(UPDATED_NAME)
                .value(UPDATED_VALUE);

        restCounterMockMvc.perform(put("/api/counters")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedCounter)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Transactional
    public void deleteCounter() throws Exception {
        // Initialize the database
        counterRepository.saveAndFlush(counter);
        int databaseSizeBeforeDelete = counterRepository.findAll().size();

        // Get the counter
        restCounterMockMvc.perform(delete("/api/counters/{id}", counter.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());

        // Validate the database is empty
        List<Counter> counters = counterRepository.findAll();
        assertThat(counters).hasSize(databaseSizeBeforeDelete);
    }
}
