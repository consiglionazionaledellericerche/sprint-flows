package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.ViewRepository;
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
 * Test class for the ViewResource REST controller.
 *
 * @see ViewResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class ViewResourceIntTest {
    private static final String DEFAULT_PROCESS_ID = "AAAAA";
    private static final String UPDATED_PROCESS_ID = "BBBBB";
    private static final String DEFAULT_TYPE = "AAAAA";
    private static final String UPDATED_TYPE = "BBBBB";
    private static final String DEFAULT_VIEW = "AAAAA";
    private static final String UPDATED_VIEW = "BBBBB";
    private static final String DEFAULT_VERSION = "AAAAA";
    private static final String UPDATED_VERSION = "BBBBB";

    @Inject
    private ViewRepository viewRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restViewMockMvc;

    private View view;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ViewResource viewResource = new ViewResource();
        ReflectionTestUtils.setField(viewResource, "viewRepository", viewRepository);
        this.restViewMockMvc = MockMvcBuilders.standaloneSetup(viewResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static View createEntity(EntityManager em) {
        View view = new View();
        view = new View()
                .processId(DEFAULT_PROCESS_ID)
                .type(DEFAULT_TYPE)
                .view(DEFAULT_VIEW)
                .version(DEFAULT_VERSION);
        return view;
    }

    @Before
    public void initTest() {
        view = createEntity(em);
    }

    @Test
    @Transactional
    public void createView() throws Exception {
        int databaseSizeBeforeCreate = viewRepository.findAll().size();

        // Create the View

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isCreated());

        // Validate the View in the database
        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeCreate + 1);
        View testView = views.get(views.size() - 1);
        assertThat(testView.getProcessId()).isEqualTo(DEFAULT_PROCESS_ID);
        assertThat(testView.getType()).isEqualTo(DEFAULT_TYPE);
        assertThat(testView.getView()).isEqualTo(DEFAULT_VIEW);
        assertThat(testView.getVersion()).isEqualTo(DEFAULT_VERSION);
    }

    @Test
    @Transactional
    public void checkProcessIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = viewRepository.findAll().size();
        // set the field null
        view.setProcessId(null);

        // Create the View, which fails.

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isBadRequest());

        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeIsRequired() throws Exception {
        int databaseSizeBeforeTest = viewRepository.findAll().size();
        // set the field null
        view.setType(null);

        // Create the View, which fails.

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isBadRequest());

        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkViewIsRequired() throws Exception {
        int databaseSizeBeforeTest = viewRepository.findAll().size();
        // set the field null
        view.setView(null);

        // Create the View, which fails.

        restViewMockMvc.perform(post("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(view)))
                .andExpect(status().isBadRequest());

        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllViews() throws Exception {
        // Initialize the database
        viewRepository.saveAndFlush(view);

        // Get all the views
        restViewMockMvc.perform(get("/api/views?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(view.getId().intValue())))
                .andExpect(jsonPath("$.[*].processId").value(hasItem(DEFAULT_PROCESS_ID.toString())))
                .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
                .andExpect(jsonPath("$.[*].view").value(hasItem(DEFAULT_VIEW.toString())))
                .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION.toString())));
    }

    @Test
    @Transactional
    public void getView() throws Exception {
        // Initialize the database
        viewRepository.saveAndFlush(view);

        // Get the view
        restViewMockMvc.perform(get("/api/views/{id}", view.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(view.getId().intValue()))
            .andExpect(jsonPath("$.processId").value(DEFAULT_PROCESS_ID.toString()))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.view").value(DEFAULT_VIEW.toString()))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingView() throws Exception {
        // Get the view
        restViewMockMvc.perform(get("/api/views/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateView() throws Exception {
        // Initialize the database
        viewRepository.saveAndFlush(view);
        int databaseSizeBeforeUpdate = viewRepository.findAll().size();

        // Update the view
        View updatedView = viewRepository.findById(view.getId()).get();
        updatedView
                .processId(UPDATED_PROCESS_ID)
                .type(UPDATED_TYPE)
                .view(UPDATED_VIEW)
                .version(UPDATED_VERSION);

        restViewMockMvc.perform(put("/api/views")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedView)))
                .andExpect(status().isOk());

        // Validate the View in the database
        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeUpdate);
        View testView = views.get(views.size() - 1);
        assertThat(testView.getProcessId()).isEqualTo(UPDATED_PROCESS_ID);
        assertThat(testView.getType()).isEqualTo(UPDATED_TYPE);
        assertThat(testView.getView()).isEqualTo(UPDATED_VIEW);
        assertThat(testView.getVersion()).isEqualTo(UPDATED_VERSION);
    }

    @Test
    @Transactional
    public void deleteView() throws Exception {
        // Initialize the database
        viewRepository.saveAndFlush(view);
        int databaseSizeBeforeDelete = viewRepository.findAll().size();

        // Get the view
        restViewMockMvc.perform(delete("/api/views/{id}", view.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<View> views = viewRepository.findAll();
        assertThat(views).hasSize(databaseSizeBeforeDelete - 1);
    }
}
