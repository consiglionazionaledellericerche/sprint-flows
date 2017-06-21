package it.cnr.si.web.rest;

import it.cnr.si.SprintApp;
import it.cnr.si.domain.NotificationRule;
import it.cnr.si.repository.NotificationRuleRepository;

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
 * Test class for the NotificationRuleResource REST controller.
 *
 * @see NotificationRuleResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SprintApp.class)
public class NotificationRuleResourceIntTest {
    private static final String DEFAULT_PROCESS_ID = "AAAAA";
    private static final String UPDATED_PROCESS_ID = "BBBBB";
    private static final String DEFAULT_TASK_NAME = "AAAAA";
    private static final String UPDATED_TASK_NAME = "BBBBB";
    private static final String DEFAULT_GROUPS = "AAAAA";
    private static final String UPDATED_GROUPS = "BBBBB";
    private static final String DEFAULT_EVENT_TYPE = "AAAAA";
    private static final String UPDATED_EVENT_TYPE = "BBBBB";

    @Inject
    private NotificationRuleRepository notificationRuleRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restNotificationRuleMockMvc;

    private NotificationRule notificationRule;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        NotificationRuleResource notificationRuleResource = new NotificationRuleResource();
        ReflectionTestUtils.setField(notificationRuleResource, "notificationRuleRepository", notificationRuleRepository);
        this.restNotificationRuleMockMvc = MockMvcBuilders.standaloneSetup(notificationRuleResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static NotificationRule createEntity(EntityManager em) {
        NotificationRule notificationRule = new NotificationRule();
        notificationRule = new NotificationRule()
                .processId(DEFAULT_PROCESS_ID)
                .taskName(DEFAULT_TASK_NAME)
                .groups(DEFAULT_GROUPS)
                .eventType(DEFAULT_EVENT_TYPE);
        return notificationRule;
    }

    @Before
    public void initTest() {
        notificationRule = createEntity(em);
    }

    @Test
    @Transactional
    public void createNotificationRule() throws Exception {
        int databaseSizeBeforeCreate = notificationRuleRepository.findAll().size();

        // Create the NotificationRule

        restNotificationRuleMockMvc.perform(post("/api/notification-rules")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(notificationRule)))
                .andExpect(status().isCreated());

        // Validate the NotificationRule in the database
        List<NotificationRule> notificationRules = notificationRuleRepository.findAll();
        assertThat(notificationRules).hasSize(databaseSizeBeforeCreate + 1);
        NotificationRule testNotificationRule = notificationRules.get(notificationRules.size() - 1);
        assertThat(testNotificationRule.getProcessId()).isEqualTo(DEFAULT_PROCESS_ID);
        assertThat(testNotificationRule.getTaskName()).isEqualTo(DEFAULT_TASK_NAME);
        assertThat(testNotificationRule.getGroups()).isEqualTo(DEFAULT_GROUPS);
        assertThat(testNotificationRule.getEventType()).isEqualTo(DEFAULT_EVENT_TYPE);
    }

    @Test
    @Transactional
    public void checkProcessIdIsRequired() throws Exception {
        int databaseSizeBeforeTest = notificationRuleRepository.findAll().size();
        // set the field null
        notificationRule.setProcessId(null);

        // Create the NotificationRule, which fails.

        restNotificationRuleMockMvc.perform(post("/api/notification-rules")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(notificationRule)))
                .andExpect(status().isBadRequest());

        List<NotificationRule> notificationRules = notificationRuleRepository.findAll();
        assertThat(notificationRules).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTaskNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = notificationRuleRepository.findAll().size();
        // set the field null
        notificationRule.setTaskName(null);

        // Create the NotificationRule, which fails.

        restNotificationRuleMockMvc.perform(post("/api/notification-rules")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(notificationRule)))
                .andExpect(status().isBadRequest());

        List<NotificationRule> notificationRules = notificationRuleRepository.findAll();
        assertThat(notificationRules).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGroupsIsRequired() throws Exception {
        int databaseSizeBeforeTest = notificationRuleRepository.findAll().size();
        // set the field null
        notificationRule.setGroups(null);

        // Create the NotificationRule, which fails.

        restNotificationRuleMockMvc.perform(post("/api/notification-rules")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(notificationRule)))
                .andExpect(status().isBadRequest());

        List<NotificationRule> notificationRules = notificationRuleRepository.findAll();
        assertThat(notificationRules).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkEventTypeIsRequired() throws Exception {
        int databaseSizeBeforeTest = notificationRuleRepository.findAll().size();
        // set the field null
        notificationRule.setEventType(null);

        // Create the NotificationRule, which fails.

        restNotificationRuleMockMvc.perform(post("/api/notification-rules")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(notificationRule)))
                .andExpect(status().isBadRequest());

        List<NotificationRule> notificationRules = notificationRuleRepository.findAll();
        assertThat(notificationRules).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllNotificationRules() throws Exception {
        // Initialize the database
        notificationRuleRepository.saveAndFlush(notificationRule);

        // Get all the notificationRules
        restNotificationRuleMockMvc.perform(get("/api/notification-rules?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(notificationRule.getId().intValue())))
                .andExpect(jsonPath("$.[*].processId").value(hasItem(DEFAULT_PROCESS_ID.toString())))
                .andExpect(jsonPath("$.[*].taskName").value(hasItem(DEFAULT_TASK_NAME.toString())))
                .andExpect(jsonPath("$.[*].groups").value(hasItem(DEFAULT_GROUPS.toString())))
                .andExpect(jsonPath("$.[*].eventType").value(hasItem(DEFAULT_EVENT_TYPE.toString())));
    }

    @Test
    @Transactional
    public void getNotificationRule() throws Exception {
        // Initialize the database
        notificationRuleRepository.saveAndFlush(notificationRule);

        // Get the notificationRule
        restNotificationRuleMockMvc.perform(get("/api/notification-rules/{id}", notificationRule.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(notificationRule.getId().intValue()))
            .andExpect(jsonPath("$.processId").value(DEFAULT_PROCESS_ID.toString()))
            .andExpect(jsonPath("$.taskName").value(DEFAULT_TASK_NAME.toString()))
            .andExpect(jsonPath("$.groups").value(DEFAULT_GROUPS.toString()))
            .andExpect(jsonPath("$.eventType").value(DEFAULT_EVENT_TYPE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingNotificationRule() throws Exception {
        // Get the notificationRule
        restNotificationRuleMockMvc.perform(get("/api/notification-rules/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateNotificationRule() throws Exception {
        // Initialize the database
        notificationRuleRepository.saveAndFlush(notificationRule);
        int databaseSizeBeforeUpdate = notificationRuleRepository.findAll().size();

        // Update the notificationRule
        NotificationRule updatedNotificationRule = notificationRuleRepository.findOne(notificationRule.getId());
        updatedNotificationRule
                .processId(UPDATED_PROCESS_ID)
                .taskName(UPDATED_TASK_NAME)
                .groups(UPDATED_GROUPS)
                .eventType(UPDATED_EVENT_TYPE);

        restNotificationRuleMockMvc.perform(put("/api/notification-rules")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedNotificationRule)))
                .andExpect(status().isOk());

        // Validate the NotificationRule in the database
        List<NotificationRule> notificationRules = notificationRuleRepository.findAll();
        assertThat(notificationRules).hasSize(databaseSizeBeforeUpdate);
        NotificationRule testNotificationRule = notificationRules.get(notificationRules.size() - 1);
        assertThat(testNotificationRule.getProcessId()).isEqualTo(UPDATED_PROCESS_ID);
        assertThat(testNotificationRule.getTaskName()).isEqualTo(UPDATED_TASK_NAME);
        assertThat(testNotificationRule.getGroups()).isEqualTo(UPDATED_GROUPS);
        assertThat(testNotificationRule.getEventType()).isEqualTo(UPDATED_EVENT_TYPE);
    }

    @Test
    @Transactional
    public void deleteNotificationRule() throws Exception {
        // Initialize the database
        notificationRuleRepository.saveAndFlush(notificationRule);
        int databaseSizeBeforeDelete = notificationRuleRepository.findAll().size();

        // Get the notificationRule
        restNotificationRuleMockMvc.perform(delete("/api/notification-rules/{id}", notificationRule.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<NotificationRule> notificationRules = notificationRuleRepository.findAll();
        assertThat(notificationRules).hasSize(databaseSizeBeforeDelete - 1);
    }
}
