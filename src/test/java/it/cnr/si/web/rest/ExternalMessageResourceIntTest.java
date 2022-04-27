package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.ExternalMessageRepository;
import it.cnr.si.service.ExternalMessageService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.domain.enumeration.ExternalApplication;
/**
 * Test class for the ExternalMessageResource REST controller.
 *
 * @see ExternalMessageResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
public class ExternalMessageResourceIntTest {
    private static final String DEFAULT_URL = "AAAAA";
    private static final String UPDATED_URL = "BBBBB";

    private static final ExternalMessageVerb DEFAULT_VERB = ExternalMessageVerb.POST;
    private static final ExternalMessageVerb UPDATED_VERB = ExternalMessageVerb.GET;
    private static final String DEFAULT_PAYLOAD = "AAAAA";
    private static final String UPDATED_PAYLOAD = "BBBBB";

    private static final ExternalMessageStatus DEFAULT_STATUS = ExternalMessageStatus.NEW;
    private static final ExternalMessageStatus UPDATED_STATUS = ExternalMessageStatus.SENT;

    private static final Integer DEFAULT_RETRIES = 1;
    private static final Integer UPDATED_RETRIES = 2;
    private static final String DEFAULT_LAST_ERROR_MESSAGE = "AAAAA";
    private static final String UPDATED_LAST_ERROR_MESSAGE = "BBBBB";

    private static final ExternalApplication DEFAULT_APPLICATION = ExternalApplication.ABIL;
    private static final ExternalApplication UPDATED_APPLICATION = ExternalApplication.SIGLA;

    private static final LocalDate DEFAULT_CREATION_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_CREATION_DATE = LocalDate.now(ZoneId.systemDefault());

    private static final LocalDate DEFAULT_LAST_SEND_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_LAST_SEND_DATE = LocalDate.now(ZoneId.systemDefault());

    @Inject
    private ExternalMessageRepository externalMessageRepository;

    @Inject
    private ExternalMessageService externalMessageService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restExternalMessageMockMvc;

    private ExternalMessage externalMessage;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ExternalMessageResource externalMessageResource = new ExternalMessageResource();
        ReflectionTestUtils.setField(externalMessageResource, "externalMessageService", externalMessageService);
        this.restExternalMessageMockMvc = MockMvcBuilders.standaloneSetup(externalMessageResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ExternalMessage createEntity(EntityManager em) {
        ExternalMessage externalMessage = new ExternalMessage();
        externalMessage = new ExternalMessage()
                .url(DEFAULT_URL)
                .verb(DEFAULT_VERB)
                .payload(DEFAULT_PAYLOAD)
                .status(DEFAULT_STATUS)
                .retries(DEFAULT_RETRIES)
                .lastErrorMessage(DEFAULT_LAST_ERROR_MESSAGE)
                .application(DEFAULT_APPLICATION)
                .creationDate(DEFAULT_CREATION_DATE)
                .lastSendDate(DEFAULT_LAST_SEND_DATE);
        return externalMessage;
    }

    @Before
    public void initTest() {
        externalMessage = createEntity(em);
    }

    @Test
    @Transactional
    public void createExternalMessage() throws Exception {
        int databaseSizeBeforeCreate = externalMessageRepository.findAll().size();

        // Create the ExternalMessage

        restExternalMessageMockMvc.perform(post("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(externalMessage)))
                .andExpect(status().isCreated());

        // Validate the ExternalMessage in the database
        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeCreate + 1);
        ExternalMessage testExternalMessage = externalMessages.get(externalMessages.size() - 1);
        assertThat(testExternalMessage.getUrl()).isEqualTo(DEFAULT_URL);
        assertThat(testExternalMessage.getVerb()).isEqualTo(DEFAULT_VERB);
        assertThat(testExternalMessage.getPayload()).isEqualTo(DEFAULT_PAYLOAD);
        assertThat(testExternalMessage.getStatus()).isEqualTo(DEFAULT_STATUS);
        assertThat(testExternalMessage.getRetries()).isEqualTo(DEFAULT_RETRIES);
        assertThat(testExternalMessage.getLastErrorMessage()).isEqualTo(DEFAULT_LAST_ERROR_MESSAGE);
        assertThat(testExternalMessage.getApplication()).isEqualTo(DEFAULT_APPLICATION);
        assertThat(testExternalMessage.getCreationDate()).isEqualTo(DEFAULT_CREATION_DATE);
        assertThat(testExternalMessage.getLastSendDate()).isEqualTo(DEFAULT_LAST_SEND_DATE);
    }

    @Test
    @Transactional
    public void checkUrlIsRequired() throws Exception {
        int databaseSizeBeforeTest = externalMessageRepository.findAll().size();
        // set the field null
        externalMessage.setUrl(null);

        // Create the ExternalMessage, which fails.

        restExternalMessageMockMvc.perform(post("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(externalMessage)))
                .andExpect(status().isBadRequest());

        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkVerbIsRequired() throws Exception {
        int databaseSizeBeforeTest = externalMessageRepository.findAll().size();
        // set the field null
        externalMessage.setVerb(null);

        // Create the ExternalMessage, which fails.

        restExternalMessageMockMvc.perform(post("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(externalMessage)))
                .andExpect(status().isBadRequest());

        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkPayloadIsRequired() throws Exception {
        int databaseSizeBeforeTest = externalMessageRepository.findAll().size();
        // set the field null
        externalMessage.setPayload(null);

        // Create the ExternalMessage, which fails.

        restExternalMessageMockMvc.perform(post("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(externalMessage)))
                .andExpect(status().isBadRequest());

        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkStatusIsRequired() throws Exception {
        int databaseSizeBeforeTest = externalMessageRepository.findAll().size();
        // set the field null
        externalMessage.setStatus(null);

        // Create the ExternalMessage, which fails.

        restExternalMessageMockMvc.perform(post("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(externalMessage)))
                .andExpect(status().isBadRequest());

        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkRetriesIsRequired() throws Exception {
        int databaseSizeBeforeTest = externalMessageRepository.findAll().size();
        // set the field null
        externalMessage.setRetries(null);

        // Create the ExternalMessage, which fails.

        restExternalMessageMockMvc.perform(post("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(externalMessage)))
                .andExpect(status().isBadRequest());

        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkApplicationIsRequired() throws Exception {
        int databaseSizeBeforeTest = externalMessageRepository.findAll().size();
        // set the field null
        externalMessage.setApplication(null);

        // Create the ExternalMessage, which fails.

        restExternalMessageMockMvc.perform(post("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(externalMessage)))
                .andExpect(status().isBadRequest());

        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllExternalMessages() throws Exception {
        // Initialize the database
        externalMessageRepository.saveAndFlush(externalMessage);

        // Get all the externalMessages
        restExternalMessageMockMvc.perform(get("/api/external-messages?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(externalMessage.getId().intValue())))
                .andExpect(jsonPath("$.[*].url").value(hasItem(DEFAULT_URL.toString())))
                .andExpect(jsonPath("$.[*].verb").value(hasItem(DEFAULT_VERB.toString())))
                .andExpect(jsonPath("$.[*].payload").value(hasItem(DEFAULT_PAYLOAD.toString())))
                .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
                .andExpect(jsonPath("$.[*].retries").value(hasItem(DEFAULT_RETRIES)))
                .andExpect(jsonPath("$.[*].lastErrorMessage").value(hasItem(DEFAULT_LAST_ERROR_MESSAGE.toString())))
                .andExpect(jsonPath("$.[*].application").value(hasItem(DEFAULT_APPLICATION.toString())))
                .andExpect(jsonPath("$.[*].creationDate").value(hasItem(DEFAULT_CREATION_DATE.toString())))
                .andExpect(jsonPath("$.[*].lastSendDate").value(hasItem(DEFAULT_LAST_SEND_DATE.toString())));
    }

    @Test
    @Transactional
    public void getExternalMessage() throws Exception {
        // Initialize the database
        externalMessageRepository.saveAndFlush(externalMessage);

        // Get the externalMessage
        restExternalMessageMockMvc.perform(get("/api/external-messages/{id}", externalMessage.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(externalMessage.getId().intValue()))
            .andExpect(jsonPath("$.url").value(DEFAULT_URL.toString()))
            .andExpect(jsonPath("$.verb").value(DEFAULT_VERB.toString()))
            .andExpect(jsonPath("$.payload").value(DEFAULT_PAYLOAD.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.retries").value(DEFAULT_RETRIES))
            .andExpect(jsonPath("$.lastErrorMessage").value(DEFAULT_LAST_ERROR_MESSAGE.toString()))
            .andExpect(jsonPath("$.application").value(DEFAULT_APPLICATION.toString()))
            .andExpect(jsonPath("$.creationDate").value(DEFAULT_CREATION_DATE.toString()))
            .andExpect(jsonPath("$.lastSendDate").value(DEFAULT_LAST_SEND_DATE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingExternalMessage() throws Exception {
        // Get the externalMessage
        restExternalMessageMockMvc.perform(get("/api/external-messages/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateExternalMessage() throws Exception {
        // Initialize the database
        externalMessageService.save(externalMessage);

        int databaseSizeBeforeUpdate = externalMessageRepository.findAll().size();

        // Update the externalMessage
        ExternalMessage updatedExternalMessage = externalMessageRepository.findOne(externalMessage.getId());
        updatedExternalMessage
                .url(UPDATED_URL)
                .verb(UPDATED_VERB)
                .payload(UPDATED_PAYLOAD)
                .status(UPDATED_STATUS)
                .retries(UPDATED_RETRIES)
                .lastErrorMessage(UPDATED_LAST_ERROR_MESSAGE)
                .application(UPDATED_APPLICATION)
                .creationDate(UPDATED_CREATION_DATE)
                .lastSendDate(UPDATED_LAST_SEND_DATE);

        restExternalMessageMockMvc.perform(put("/api/external-messages")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedExternalMessage)))
                .andExpect(status().isOk());

        // Validate the ExternalMessage in the database
        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeUpdate);
        ExternalMessage testExternalMessage = externalMessages.get(externalMessages.size() - 1);
        assertThat(testExternalMessage.getUrl()).isEqualTo(UPDATED_URL);
        assertThat(testExternalMessage.getVerb()).isEqualTo(UPDATED_VERB);
        assertThat(testExternalMessage.getPayload()).isEqualTo(UPDATED_PAYLOAD);
        assertThat(testExternalMessage.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testExternalMessage.getRetries()).isEqualTo(UPDATED_RETRIES);
        assertThat(testExternalMessage.getLastErrorMessage()).isEqualTo(UPDATED_LAST_ERROR_MESSAGE);
        assertThat(testExternalMessage.getApplication()).isEqualTo(UPDATED_APPLICATION);
        assertThat(testExternalMessage.getCreationDate()).isEqualTo(UPDATED_CREATION_DATE);
        assertThat(testExternalMessage.getLastSendDate()).isEqualTo(UPDATED_LAST_SEND_DATE);
    }

    @Test
    @Transactional
    public void deleteExternalMessage() throws Exception {
        // Initialize the database
        externalMessageService.save(externalMessage);

        int databaseSizeBeforeDelete = externalMessageRepository.findAll().size();

        // Get the externalMessage
        restExternalMessageMockMvc.perform(delete("/api/external-messages/{id}", externalMessage.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<ExternalMessage> externalMessages = externalMessageRepository.findAll();
        assertThat(externalMessages).hasSize(databaseSizeBeforeDelete - 1);
    }
}
