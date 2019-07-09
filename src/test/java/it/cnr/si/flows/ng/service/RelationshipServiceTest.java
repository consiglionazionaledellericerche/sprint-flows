package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Relationship;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.RelationshipService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test,cnr")
public class RelationshipServiceTest {

    private static final String GROUP_RELATIONSHIP = "aaaaaa";
    @Inject
    private RelationshipService relationshipService;

    @Test
    public void testGetAllGroupsForUser() throws Exception {
        //in questo modo testo anche il metodo getAllRelationship che viene richiamato in getAllGroupsForUserOLD
        List<GrantedAuthority> groupsForRa = relationshipService.getAllGroupsForUserOLD(TestServices.getRA());
        List<GrantedAuthority> groupsForRa2 = relationshipService.getAllGroupsForUserOLD(TestServices.getRA2());

        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE", groupsForRa, groupsForRa2);

        //aggiungo una nuova relationship
        Relationship relationship = new Relationship();
        relationship.setGroupName(Utils.removeLeadingRole(String.valueOf(groupsForRa.get(0))));
        relationship.setGroupRelationship(GROUP_RELATIONSHIP);
        relationship = relationshipService.save(relationship);

        //verifico che getAllGroupsForUserOLD prenda la modifica per entrambi gli utenti
        List<GrantedAuthority> newGroupsForRa = relationshipService.getAllGroupsForUserOLD(TestServices.getRA());
        List<GrantedAuthority> newGroupsForRa2 = relationshipService.getAllGroupsForUserOLD(TestServices.getRA2());
        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE", newGroupsForRa, newGroupsForRa2);

        assertEquals("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUserOLD", groupsForRa.size() + 1, newGroupsForRa.size());
        assertEquals("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUserOLD", groupsForRa2.size() + 1, newGroupsForRa2.size());

        newGroupsForRa.removeAll(groupsForRa);
        newGroupsForRa2.removeAll(groupsForRa2);
        assertEquals("il gruppo aggiunto con la relationship NON è quello atteso", newGroupsForRa.get(0).equals(GROUP_RELATIONSHIP), newGroupsForRa2.get(0).equals(GROUP_RELATIONSHIP));

        //elimino la relazione e verifico che tutto funzioni come prima
        relationshipService.delete(relationship.getId());
        groupsForRa = relationshipService.getAllGroupsForUserOLD(TestServices.getRA());
        groupsForRa2 = relationshipService.getAllGroupsForUserOLD(TestServices.getRA2());

        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE dopo la cancellazione della relationship", groupsForRa, groupsForRa2);
    }

    @Test
    public void testGetAllGroups() {
        Set<String> allGroups = relationshipService.getAllGroupsForUser("maurizio.lancia");
        System.out.println(allGroups);
    }

    @Test
    public void testGetLanciaByResponsabileStrutture() {

        Set<String> allUsersInGroup = relationshipService.getAllUsersInGroup("responsabile-struttura@34408");
        System.out.println(allUsersInGroup);
    }
}