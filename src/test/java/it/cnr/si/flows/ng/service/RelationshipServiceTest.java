package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Relationship;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.RelationshipService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class RelationshipServiceTest {

    private final Logger log = LoggerFactory.getLogger(RelationshipServiceTest.class);

    private static final String GROUP_RELATIONSHIP = "aaaaaa";
    @Inject
    private RelationshipService relationshipService;

    @Test
    public void testGetAllGroupsForUser() throws Exception {
        //in questo modo testo anche il metodo getAllRelationship che viene richiamato in getAllGroupsForUserOLD
        Set<String> groupsForRa = relationshipService.getAllGroupsForUser(TestServices.getRA());
        Set<String> groupsForRa2 = relationshipService.getAllGroupsForUser(TestServices.getRA2());

        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE", groupsForRa, groupsForRa2);

        //aggiungo una nuova relationship
        Relationship relationship = new Relationship();
        relationship.setGroupName(groupsForRa.stream().findAny().get());
        relationship.setGroupRelationship(GROUP_RELATIONSHIP);
        relationship.setGroupRole("member");
        log.info("Inserisco la Relationship "+ relationship);
        relationship = relationshipService.save(relationship);

        //verifico che getAllGroupsForUserOLD prenda la modifica per entrambi gli utenti
        Set<String> newGroupsForRa = relationshipService.getAllGroupsForUser(TestServices.getRA());
        Set<String> newGroupsForRa2 = relationshipService.getAllGroupsForUser(TestServices.getRA2());
        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE", newGroupsForRa, newGroupsForRa2);


        assertEquals("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUser "+ groupsForRa + newGroupsForRa, groupsForRa.size() + 1, newGroupsForRa.size());
        assertEquals("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUser"+ groupsForRa2 + newGroupsForRa2, groupsForRa2.size() + 1, newGroupsForRa2.size());

        //elimino la relazione e verifico che tutto funzioni come prima
        relationshipService.delete(relationship.getId());
        groupsForRa = relationshipService.getAllGroupsForUser(TestServices.getRA());
        groupsForRa2 = relationshipService.getAllGroupsForUser(TestServices.getRA2());

        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE dopo la cancellazione della relationship", groupsForRa, groupsForRa2);
    }

    @Test
    public void testGetAllGroups() {
        Set<String> allGroups = relationshipService.getAllGroupsForUser("maurizio.lancia");
        System.out.println(allGroups);
        List<String> allGroupsOLD = relationshipService.getAllGroupsForUserOLD("maurizio.lancia").stream().map(GrantedAuthority::getAuthority).map(Utils::removeLeadingRole).collect(Collectors.toList());
        System.out.println(allGroupsOLD);

        Assert.assertTrue(allGroups.containsAll(allGroupsOLD));
        Assert.assertFalse(allGroupsOLD.containsAll(allGroups));


    }

    @Test
    public void testGetLanciaByResponsabileStrutture() {

        Set<String> allUsersInGroup = relationshipService.getAllUsersInGroup("responsabile-struttura@34408");
        System.out.println(allUsersInGroup);
    }
}