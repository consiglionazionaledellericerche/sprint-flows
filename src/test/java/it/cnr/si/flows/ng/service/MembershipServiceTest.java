package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Relationship;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class MembershipServiceTest {

    private final Logger log = LoggerFactory.getLogger(MembershipServiceTest.class);

    private static final String GROUP_RELATIONSHIP = "aaaaaa";
    @Inject
    private MembershipService membershipService;
    @Inject
    private RelationshipService relationshipService;


    @Test
    public void testGetAllGroupsForUser() throws Exception {

        Set<String> groupsForRa = membershipService.getAllGroupsForUser(TestServices.getRA());

        //aggiungo una nuova relationship
        Relationship relationship = new Relationship();
        relationship.setGroupName(groupsForRa.stream().findAny().get());
        relationship.setGroupRelationship(GROUP_RELATIONSHIP);
        relationship.setGroupRole("member");
        log.info("Inserisco la Relationship "+ relationship);
        relationship = relationshipService.save(relationship);

        //verifico che getAllGroupsForUser prenda la modifica per entrambi gli utenti
        Set<String> groupsForRa2 = membershipService.getAllGroupsForUser(TestServices.getRA());

        assertEquals("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUser "+ groupsForRa + groupsForRa2, groupsForRa.size() + 1, groupsForRa2.size());
        assertTrue("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUser", groupsForRa2.contains(GROUP_RELATIONSHIP));

        //elimino la relazione e verifico che tutto funzioni come prima
        relationshipService.delete(relationship.getId());
        Set<String> groupsForRa3 = membershipService.getAllGroupsForUser(TestServices.getRA());

        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE dopo la cancellazione della relationship", groupsForRa, groupsForRa3);
    }

    @Test
    public void testGetAllUsersInGroup() {

        Set<String> allUsersInGroup = membershipService.getAllUsersInGroup("segreteria");
        assertTrue("Il gruppo segreteria non contiene utente2", allUsersInGroup.contains("utente2"));
        assertTrue("Il gruppo segreteria non contiene utente3", allUsersInGroup.contains("utente3"));
    }

    @Test
    public void testGetUsersInMyGroups() {

        Set<String> usersInMyGroups = membershipService.getUsersInMyGroups("utente2");
        log.info("All groups for user "+ membershipService.getAllGroupsForUser("utente2"));
        assertTrue(usersInMyGroups.contains("utente3"));

    }
}
