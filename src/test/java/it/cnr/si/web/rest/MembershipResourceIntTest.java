package it.cnr.si.web.rest;

import it.cnr.si.SprintApp;
import it.cnr.si.domain.Membership;
import it.cnr.si.repository.MembershipRepository;
import it.cnr.si.service.MembershipService;

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
 * Test class for the MembershipResource REST controller.
 *
 * @see MembershipResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SprintApp.class)
public class MembershipResourceIntTest {
    private static final String DEFAULT_USERNAME = "AAAAA";
    private static final String UPDATED_USERNAME = "BBBBB";
    private static final String DEFAULT_GROUPNAME = "AAAAA";
    private static final String UPDATED_GROUPNAME = "BBBBB";

    @Inject
    private MembershipRepository membershipRepository;

    @Inject
    private MembershipService membershipService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restMembershipMockMvc;

    private Membership membership;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        MembershipResource membershipResource = new MembershipResource();
        ReflectionTestUtils.setField(membershipResource, "membershipService", membershipService);
        this.restMembershipMockMvc = MockMvcBuilders.standaloneSetup(membershipResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Membership createEntity(EntityManager em) {
        Membership membership = new Membership();
        membership = new Membership()
                .username(DEFAULT_USERNAME)
                .groupname(DEFAULT_GROUPNAME);
        return membership;
    }

    @Before
    public void initTest() {
        membership = createEntity(em);
    }

    @Test
    @Transactional
    public void createMembership() throws Exception {
        int databaseSizeBeforeCreate = membershipRepository.findAll().size();

        // Create the Membership

        restMembershipMockMvc.perform(post("/api/memberships")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(membership)))
                .andExpect(status().isCreated());

        // Validate the Membership in the database
        List<Membership> memberships = membershipRepository.findAll();
        assertThat(memberships).hasSize(databaseSizeBeforeCreate + 1);
        Membership testMembership = memberships.get(memberships.size() - 1);
        assertThat(testMembership.getUsername()).isEqualTo(DEFAULT_USERNAME);
        assertThat(testMembership.getGroupname()).isEqualTo(DEFAULT_GROUPNAME);
    }

    @Test
    @Transactional
    public void checkUsernameIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setUsername(null);

        // Create the Membership, which fails.

        restMembershipMockMvc.perform(post("/api/memberships")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(membership)))
                .andExpect(status().isBadRequest());

        List<Membership> memberships = membershipRepository.findAll();
        assertThat(memberships).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGroupnameIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setGroupname(null);

        // Create the Membership, which fails.

        restMembershipMockMvc.perform(post("/api/memberships")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(membership)))
                .andExpect(status().isBadRequest());

        List<Membership> memberships = membershipRepository.findAll();
        assertThat(memberships).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllMemberships() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get all the memberships
        restMembershipMockMvc.perform(get("/api/memberships?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.[*].id").value(hasItem(membership.getId().intValue())))
                .andExpect(jsonPath("$.[*].username").value(hasItem(DEFAULT_USERNAME.toString())))
                .andExpect(jsonPath("$.[*].groupname").value(hasItem(DEFAULT_GROUPNAME.toString())));
    }

    @Test
    @Transactional
    public void getMembership() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get the membership
        restMembershipMockMvc.perform(get("/api/memberships/{id}", membership.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(membership.getId().intValue()))
            .andExpect(jsonPath("$.username").value(DEFAULT_USERNAME.toString()))
            .andExpect(jsonPath("$.groupname").value(DEFAULT_GROUPNAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingMembership() throws Exception {
        // Get the membership
        restMembershipMockMvc.perform(get("/api/memberships/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMembership() throws Exception {
        // Initialize the database
        membershipService.save(membership);

        int databaseSizeBeforeUpdate = membershipRepository.findAll().size();

        // Update the membership
        Membership updatedMembership = membershipRepository.findOne(membership.getId());
        updatedMembership
                .username(UPDATED_USERNAME)
                .groupname(UPDATED_GROUPNAME);

        restMembershipMockMvc.perform(put("/api/memberships")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedMembership)))
                .andExpect(status().isOk());

        // Validate the Membership in the database
        List<Membership> memberships = membershipRepository.findAll();
        assertThat(memberships).hasSize(databaseSizeBeforeUpdate);
        Membership testMembership = memberships.get(memberships.size() - 1);
        assertThat(testMembership.getUsername()).isEqualTo(UPDATED_USERNAME);
        assertThat(testMembership.getGroupname()).isEqualTo(UPDATED_GROUPNAME);
    }

    @Test
    @Transactional
    public void deleteMembership() throws Exception {
        // Initialize the database
        membershipService.save(membership);

        int databaseSizeBeforeDelete = membershipRepository.findAll().size();

        // Get the membership
        restMembershipMockMvc.perform(delete("/api/memberships/{id}", membership.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate the database is empty
        List<Membership> memberships = membershipRepository.findAll();
        assertThat(memberships).hasSize(databaseSizeBeforeDelete - 1);
    }
}
