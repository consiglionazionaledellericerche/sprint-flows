package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Form;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.FormRepository;
import org.junit.Before;
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
 * Test class for the FormResource REST controller.
 *
 * @see FormResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
//@Ignore
public class FormResourceIntTest {
    private static final String DEFAULT_PROCESS_DEFINITION_KEY = "AAAAA";
    private static final String UPDATED_PROCESS_DEFINITION_KEY = "BBBBB";
    private static final String DEFAULT_VERSION = "AAAAA";
    private static final String UPDATED_VERSION = "BBBBB";
    private static final String DEFAULT_TASK_ID = "AAAAA";
    private static final String UPDATED_TASK_ID = "BBBBB";
    private static final String DEFAULT_FORM = "AAAAA";
    private static final String UPDATED_FORM = "BBBBB";

    @Inject
    private FormRepository formRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restFormMockMvc;

    private Form form;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Form createEntity(EntityManager em) {
        Form form = new Form();
        form = new Form()
                .processDefinitionKey(DEFAULT_PROCESS_DEFINITION_KEY)
                .version(DEFAULT_VERSION)
                .taskId(DEFAULT_TASK_ID)
                .form(DEFAULT_FORM);
        return form;
    }

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        FormResource formResource = new FormResource();
        ReflectionTestUtils.setField(formResource, "formRepository", formRepository);
        this.restFormMockMvc = MockMvcBuilders.standaloneSetup(formResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        form = createEntity(em);
    }

    @Test
    @Transactional
    public void createForm() throws Exception {
        int databaseSizeBeforeCreate = formRepository.findAll().size();

        // Create the Form

        restFormMockMvc.perform(post("/api/forms")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(form)))
                .andExpect(status().isCreated());

        // Validate the Form in the database
        List<Form> forms = formRepository.findAll();
        assertThat(forms).hasSize(databaseSizeBeforeCreate + 1);
        Form testForm = forms.get(forms.size() - 1);
        assertThat(testForm.getProcessDefinitionKey()).isEqualTo(DEFAULT_PROCESS_DEFINITION_KEY);
        assertThat(testForm.getVersion()).isEqualTo(DEFAULT_VERSION);
        assertThat(testForm.getTaskId()).isEqualTo(DEFAULT_TASK_ID);
        assertThat(testForm.getForm()).isEqualTo(DEFAULT_FORM);
    }

    @Test
    @Transactional
    public void checkProcessDefinitionKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = formRepository.findAll().size();
        // set the field null
        form.setProcessDefinitionKey(null);

        // Create the Form, which fails.

        restFormMockMvc.perform(post("/api/forms")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(form)))
                .andExpect(status().isBadRequest());

        List<Form> forms = formRepository.findAll();
        assertThat(forms).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkVersionIsRequired() throws Exception {
        int databaseSizeBeforeTest = formRepository.findAll().size();
        // set the field null
        form.setVersion(null);

        // Create the Form, which fails.

        restFormMockMvc.perform(post("/api/forms")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(form)))
                .andExpect(status().isBadRequest());

        List<Form> forms = formRepository.findAll();
        assertThat(forms).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTaskIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = formRepository.findAll().size();
        // set the field null
        form.setTaskId(null);

        // Create the Form, which fails.

        restFormMockMvc.perform(post("/api/forms")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(form)))
                .andExpect(status().isBadRequest());

        List<Form> forms = formRepository.findAll();
        assertThat(forms).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkFormIsRequired() throws Exception {
        int databaseSizeBeforeTest = formRepository.findAll().size();
        // set the field null
        form.setForm(null);

        // Create the Form, which fails.

        restFormMockMvc.perform(post("/api/forms")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(form)))
                .andExpect(status().isBadRequest());

        List<Form> forms = formRepository.findAll();
        assertThat(forms).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllForms() throws Exception {
        // Initialize the database
        formRepository.saveAndFlush(form);

        // Get all the forms
        restFormMockMvc.perform(get("/api/forms?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(form.getId().intValue())))
                .andExpect(jsonPath("$.[*].processDefinitionKey").value(hasItem(DEFAULT_PROCESS_DEFINITION_KEY.toString())))
                .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION.toString())))
                .andExpect(jsonPath("$.[*].taskId").value(hasItem(DEFAULT_TASK_ID.toString())))
                .andExpect(jsonPath("$.[*].form").value(hasItem(DEFAULT_FORM.toString())));
    }

    @Test
    @Transactional
    public void getForm() throws Exception {
        // Initialize the database
        formRepository.saveAndFlush(form);

        // Get the form
        restFormMockMvc.perform(get("/api/forms/{id}", form.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(form.getId().intValue()))
            .andExpect(jsonPath("$.processDefinitionKey").value(DEFAULT_PROCESS_DEFINITION_KEY.toString()))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION.toString()))
            .andExpect(jsonPath("$.taskId").value(DEFAULT_TASK_ID.toString()))
            .andExpect(jsonPath("$.form").value(DEFAULT_FORM.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingForm() throws Exception {
        // Get the form
        restFormMockMvc.perform(get("/api/forms/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateForm() throws Exception {
        // Initialize the database
        formRepository.saveAndFlush(form);
        int databaseSizeBeforeUpdate = formRepository.findAll().size();

        // Update the form
        Form updatedForm = formRepository.findById(form.getId()).get();
        updatedForm
                .processDefinitionKey(UPDATED_PROCESS_DEFINITION_KEY)
                .version(UPDATED_VERSION)
                .taskId(UPDATED_TASK_ID)
                .form(UPDATED_FORM);

        restFormMockMvc.perform(put("/api/forms")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedForm)))
                .andExpect(status().isOk());

        // Validate the Form in the database
        List<Form> forms = formRepository.findAll();
        assertThat(forms).hasSize(databaseSizeBeforeUpdate);
        Form testForm = forms.get(forms.size() - 1);
        assertThat(testForm.getProcessDefinitionKey()).isEqualTo(UPDATED_PROCESS_DEFINITION_KEY);
        assertThat(testForm.getVersion()).isEqualTo(UPDATED_VERSION);
        assertThat(testForm.getTaskId()).isEqualTo(UPDATED_TASK_ID);
        assertThat(testForm.getForm()).isEqualTo(UPDATED_FORM);
    }

    @Test
    @Transactional
    public void deleteForm() throws Exception {
        // Initialize the database
        formRepository.saveAndFlush(form);
        int databaseSizeBeforeDelete = formRepository.findAll().size();

        // Get the form
        restFormMockMvc.perform(delete("/api/forms/{id}", form.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Form> forms = formRepository.findAll();
        assertThat(forms).hasSize(databaseSizeBeforeDelete - 1);
    }
}
