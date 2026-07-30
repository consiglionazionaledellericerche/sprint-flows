package it.cnr.si.web.rest;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Membership;
import it.cnr.si.flows.ng.TestUtil;
import it.cnr.si.repository.MembershipRepository;
import it.cnr.si.service.MembershipService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for the MembershipResource REST controller.
 *
 * @see MembershipResource
 */
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class MembershipResourceIntTest {

    private static final String DEFAULT_GROUPROLE = "AAAAA";
    private static final String UPDATED_GROUPROLE = "BBBBB";

    @Inject
    private MembershipRepository membershipRepository;

    @Inject
    private MembershipService membershipService;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

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

    @Before
    public void initTest() {
        membership = new Membership();
        membership.setGrouprole(DEFAULT_GROUPROLE);
    }

    @Test
    @Transactional
    @Ignore // TODO il metodo e' stato personalizzato e i test vanno riscritti
    public void createMembership() throws Exception {
        int databaseSizeBeforeCreate = membershipRepository.findAll().size();

        // Create the Membership

        restMembershipMockMvc.perform(post("/api/createMemberships")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(membership)))
                .andExpect(status().isCreated());

        // Validate the Membership in the database
        List<Membership> memberships = membershipRepository.findAll();
        assertThat(memberships).hasSize(databaseSizeBeforeCreate + 1);
        Membership testMembership = memberships.get(memberships.size() - 1);
        assertThat(testMembership.getGrouprole()).isEqualTo(DEFAULT_GROUPROLE);
    }

    @Test
    @Transactional
    @Ignore // TODO il metodo e' stato personalizzato e i test vanno riscritti
    public void checkGrouproleIsRequired() throws Exception {
        int databaseSizeBeforeTest = membershipRepository.findAll().size();
        // set the field null
        membership.setGrouprole(null);

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
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(membership.getId().intValue())))
                .andExpect(jsonPath("$.[*].grouprole").value(hasItem(DEFAULT_GROUPROLE.toString())));
    }

    @Test
    @Transactional
    public void getMembership() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

        // Get the membership
        restMembershipMockMvc.perform(get("/api/memberships/{id}", membership.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(membership.getId().intValue()))
            .andExpect(jsonPath("$.grouprole").value(DEFAULT_GROUPROLE.toString()));
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
    @Ignore // TODO il metodo e' stato personalizzato e i test vanno riscritti
    public void updateMembership() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

		int databaseSizeBeforeUpdate = membershipRepository.findAll().size();

        // Update the membership
        membership.setGrouprole(UPDATED_GROUPROLE);

        restMembershipMockMvc.perform(put("/api/createMemberships")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(membership)))
                .andExpect(status().isOk());

        // Validate the Membership in the database
        List<Membership> memberships = membershipRepository.findAll();
        assertThat(memberships).hasSize(databaseSizeBeforeUpdate);
        Membership testMembership = memberships.get(memberships.size() - 1);
        assertThat(testMembership.getGrouprole()).isEqualTo(UPDATED_GROUPROLE);
    }

    @Test
    @Transactional
    public void deleteMembership() throws Exception {
        // Initialize the database
        membershipRepository.saveAndFlush(membership);

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
