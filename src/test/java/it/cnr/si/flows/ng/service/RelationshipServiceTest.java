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
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.assertEquals;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RelationshipServiceTest {

    private static final String GROUP_RELATIONSHIP = "aaaaaa";
    @Inject
    private RelationshipService relationshipService;

    @Test
    public void testGetAllGroupsForUser() throws Exception {
        //in questo modo testo anche il metodo getAllRelationship che viene richiamato in getAllGroupsForUser
        List<GrantedAuthority> groupsForRa = relationshipService.getAllGroupsForUser(TestServices.getRA());
        List<GrantedAuthority> groupsForRa2 = relationshipService.getAllGroupsForUser(TestServices.getRA2());

        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE", groupsForRa, groupsForRa2);

        //aggiungo una nuova relationship
        Relationship relationship = new Relationship();
        relationship.setGroupName(Utils.removeLeadingRole(String.valueOf(groupsForRa.get(0))));
        relationship.setGroupRelationship(GROUP_RELATIONSHIP);
        relationship = relationshipService.save(relationship);

        //verifico che getAllGroupsForUser prenda la modifica per entrambi gli utenti
        List<GrantedAuthority> newGroupsForRa = relationshipService.getAllGroupsForUser(TestServices.getRA());
        List<GrantedAuthority> newGroupsForRa2 = relationshipService.getAllGroupsForUser(TestServices.getRA2());
        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE", newGroupsForRa, newGroupsForRa2);

        assertEquals("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUser", groupsForRa.size() + 1, newGroupsForRa.size());
        assertEquals("Aggiungendo una relationship NON viene rilevata da getAllGroupsForUser", groupsForRa2.size() + 1, newGroupsForRa2.size());

        newGroupsForRa.removeAll(groupsForRa);
        newGroupsForRa2.removeAll(groupsForRa2);
        assertEquals("il gruppo aggiunto con la relationship NON è quello atteso", newGroupsForRa.get(0).equals(GROUP_RELATIONSHIP), newGroupsForRa2.get(0).equals(GROUP_RELATIONSHIP));

        //elimino la relazione e verifico che tutto funzioni come prima
        relationshipService.delete(relationship.getId());
        groupsForRa = relationshipService.getAllGroupsForUser(TestServices.getRA());
        groupsForRa2 = relationshipService.getAllGroupsForUser(TestServices.getRA2());

        assertEquals("Due utenti che appartengono allo stesso gruppo hanno RELAZIONI DIVERSE dopo la cancellazione della relationship", groupsForRa, groupsForRa2);
    }
}