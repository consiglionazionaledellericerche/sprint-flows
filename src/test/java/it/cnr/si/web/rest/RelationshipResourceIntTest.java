package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Relationship;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.RelationshipRepository;
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
 * Test class for the RelationshipResource REST controller.
 *
 * @see RelationshipResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class RelationshipResourceIntTest {
    private static final String DEFAULT_GROUP_NAME = "AAAAA";
    private static final String UPDATED_GROUP_NAME = "BBBBB";
    private static final String DEFAULT_GROUP_RELATIONSHIP = "AAAAA";
    private static final String UPDATED_GROUP_RELATIONSHIP = "BBBBB";
    private static final String DEFAULT_GROUP_ROLE = "AAAAA";
    private static final String UPDATED_GROUP_ROLE = "BBBBB";

    @Inject
    private RelationshipRepository relationshipRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restRelationshipMockMvc;

    private Relationship relationship;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Relationship createEntity(EntityManager em) {
        Relationship relationship = new Relationship();
        relationship = new Relationship()
                .groupName(DEFAULT_GROUP_NAME)
                .groupRelationship(DEFAULT_GROUP_RELATIONSHIP)
                .groupRole(DEFAULT_GROUP_ROLE);
        return relationship;
    }

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        RelationshipResource relationshipResource = new RelationshipResource();
        ReflectionTestUtils.setField(relationshipResource, "relationshipRepository", relationshipRepository);
        this.restRelationshipMockMvc = MockMvcBuilders.standaloneSetup(relationshipResource)
                .setCustomArgumentResolvers(pageableArgumentResolver)
                .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        relationship = createEntity(em);
    }

    @Test
    @Transactional
    public void createRelationship() throws Exception {
        int databaseSizeBeforeCreate = relationshipRepository.findAll().size();

        // Create the Relationship

        restRelationshipMockMvc.perform(post("/api/relationships")
                                                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                                .content(TestUtil.convertObjectToJsonBytes(relationship)))
                .andExpect(status().isCreated());

        // Validate the Relationship in the database
        List<Relationship> relationships = relationshipRepository.findAll();
        assertThat(relationships).hasSize(databaseSizeBeforeCreate + 1);
        Relationship testRelationship = relationships.get(relationships.size() - 1);
        assertThat(testRelationship.getGroupName()).isEqualTo(DEFAULT_GROUP_NAME);
        assertThat(testRelationship.getGroupRelationship()).isEqualTo(DEFAULT_GROUP_RELATIONSHIP);
        assertThat(testRelationship.getGroupRole()).isEqualTo(DEFAULT_GROUP_ROLE);
    }

    @Test
    @Transactional
    public void checkGroupNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = relationshipRepository.findAll().size();
        // set the field null
        relationship.setGroupName(null);

        // Create the Relationship, which fails.

        restRelationshipMockMvc.perform(post("/api/relationships")
                                                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                                .content(TestUtil.convertObjectToJsonBytes(relationship)))
                .andExpect(status().isBadRequest());

        List<Relationship> relationships = relationshipRepository.findAll();
        assertThat(relationships).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGroupRelationshipIsRequired() throws Exception {
        int databaseSizeBeforeTest = relationshipRepository.findAll().size();
        // set the field null
        relationship.setGroupRelationship(null);

        // Create the Relationship, which fails.

        restRelationshipMockMvc.perform(post("/api/relationships")
                                                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                                .content(TestUtil.convertObjectToJsonBytes(relationship)))
                .andExpect(status().isBadRequest());

        List<Relationship> relationships = relationshipRepository.findAll();
        assertThat(relationships).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllRelationships() throws Exception {
        // Initialize the database
        relationshipRepository.saveAndFlush(relationship);

        // Get all the relationships
        restRelationshipMockMvc.perform(get("/api/relationships?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(relationship.getId().intValue())))
                .andExpect(jsonPath("$.[*].groupName").value(hasItem(DEFAULT_GROUP_NAME.toString())))
                .andExpect(jsonPath("$.[*].groupRelationship").value(hasItem(DEFAULT_GROUP_RELATIONSHIP.toString())))
                .andExpect(jsonPath("$.[*].groupRole").value(hasItem(DEFAULT_GROUP_ROLE.toString())));
    }

    @Test
    @Transactional
    public void getRelationship() throws Exception {
        // Initialize the database
        relationshipRepository.saveAndFlush(relationship);

        // Get the relationship
        restRelationshipMockMvc.perform(get("/api/relationships/{id}", relationship.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(relationship.getId().intValue()))
                .andExpect(jsonPath("$.groupName").value(DEFAULT_GROUP_NAME.toString()))
                .andExpect(jsonPath("$.groupRelationship").value(DEFAULT_GROUP_RELATIONSHIP.toString()))
                .andExpect(jsonPath("$.groupRole").value(DEFAULT_GROUP_ROLE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingRelationship() throws Exception {
        // Get the relationship
        restRelationshipMockMvc.perform(get("/api/relationships/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateRelationship() throws Exception {
        // Initialize the database
        relationshipRepository.saveAndFlush(relationship);
        int databaseSizeBeforeUpdate = relationshipRepository.findAll().size();

        // Update the relationship
        Relationship updatedRelationship = relationshipRepository.findById(relationship.getId()).get();
        updatedRelationship
                .groupName(UPDATED_GROUP_NAME)
                .groupRelationship(UPDATED_GROUP_RELATIONSHIP)
                .groupRole(UPDATED_GROUP_ROLE);

        restRelationshipMockMvc.perform(put("/api/relationships")
                                                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                                .content(TestUtil.convertObjectToJsonBytes(updatedRelationship)))
                .andExpect(status().isOk());

        // Validate the Relationship in the database
        List<Relationship> relationships = relationshipRepository.findAll();
        assertThat(relationships).hasSize(databaseSizeBeforeUpdate);
        Relationship testRelationship = relationships.get(relationships.size() - 1);
        assertThat(testRelationship.getGroupName()).isEqualTo(UPDATED_GROUP_NAME);
        assertThat(testRelationship.getGroupRelationship()).isEqualTo(UPDATED_GROUP_RELATIONSHIP);
        assertThat(testRelationship.getGroupRole()).isEqualTo(UPDATED_GROUP_ROLE);
    }

    @Test
    @Transactional
    public void deleteRelationship() throws Exception {
        // Initialize the database
        relationshipRepository.saveAndFlush(relationship);
        int databaseSizeBeforeDelete = relationshipRepository.findAll().size();

        // Get the relationship
        restRelationshipMockMvc.perform(delete("/api/relationships/{id}", relationship.getId())
                                                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Relationship> relationships = relationshipRepository.findAll();
        assertThat(relationships).hasSize(databaseSizeBeforeDelete - 1);
    }
}
