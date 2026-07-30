package it.cnr.si.service;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Membership;
import it.cnr.si.domain.Relationship;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.MembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service Implementation for managing Membership.
 */
@Service
@Transactional
public class MembershipService {

    private final Logger log = LoggerFactory.getLogger(MembershipService.class);

    @Inject
    private MembershipRepository membershipRepository;
    @Autowired(required = false)
    private AceBridgeService aceBridgeService;
    @Inject
    private RelationshipService relationshipService;
    @Inject
    private Environment env;


    public Membership save(Membership membership) {
        log.debug("Request to save Membership : {}", membership);
        return membershipRepository.save(membership);
    }


    @Transactional(readOnly = true)
    public Page<Membership> findAll(Pageable pageable) {
        log.debug("Request to get all Memberships");
        return membershipRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Membership findOne(Long id) {
        log.debug("Request to get Membership : {}", id);
        return membershipRepository.findById(id).get();
    }

    /*
     *  delete the  membership by id.
     */
    public void delete(Long id) {
        log.debug("Request to delete Membership : {}", id);
        membershipRepository.deleteById(id);
    }

    /**
     * Get one membership by username and groupname.
     *
     * @param username  the username of the entity
     * @param groupname the groupname of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Membership findOneByUsernameAndGroupname(String username, String groupname) {
        log.debug("Request to get Membership with username {} and groupname {}", username, groupname);
        return membershipRepository.findOneByUsernameAndGroupname(username, groupname);
    }


    public Page<Membership> getGroupsWithRole(Pageable pageable, String user, String role) {
        return membershipRepository.getGroupsWithRole(role, user, pageable);
    }


    public List<Membership> getMembershipByGroupName(String groupName) {
        return membershipRepository.getMembershipByGroupName(groupName);
    }

    /* --- */

    // Se a qualcuno dovesse servire puo' rendere questo metodo public, ma non credo - martin 4/9/19
    private Set<String> getLocalRolesForUser(String username) {
        return membershipRepository.findGroupNamesForUser(username);
    }

    // Se a qualcuno dovesse servire puo' rendere questo metodo public, ma non credo - martin 4/9/19
    private Set<String> getAceRolessForUser(String username) {
    	try {
	        return Optional.ofNullable(aceBridgeService)
	                .map(aceBridgeService -> aceBridgeService.getAceRolesForUser(username))
	                .map(strings -> strings.stream())
	                .orElse(Stream.empty())
	                .collect(Collectors.toSet());
    	} catch (Exception e) {
    		if (!username.contentEquals("admin"))
    				log.debug(e.getMessage(), e); // Succede se admin chiede i gruppi da ace
    		return new HashSet<String>();
    	}
    }

    /**
     * Dato uno username restituisce tutti suoi gruppi, sia di ACE che di Membership,
     * compresi quelli ereditati
     *
     * I nomi dei gruppi NON hanno il ROLE_
     *
     * @param username per chi cercare i gruppi
     * @return Set di tutti i gruppi dell'utente
     */
    public Set<String> getAllRolesForUser(String username) {

        Set<String> roles = new HashSet<>();

        roles.addAll( getAceRolessForUser(username) );
        roles.addAll( getLocalRolesForUser(username) );
        roles.add("USER");

        roles.addAll( getAllChildRolesRecursively(roles, new HashSet<>()) );

        log.debug("Ruoli ace e locali per l'utente {}: {}", username, roles);

        return roles;
    }

    /* --- */

    @SuppressWarnings("deprecation")
    private List<String> getUsersInGroup(String groupName) {
        List<String> result = membershipRepository.findMembersInGroup(groupName);
        Set<String> users = Optional.ofNullable(aceBridgeService)
                .map(aceBridgeService -> aceBridgeService.getUsersInAceGroup(groupName))
                .filter(strings -> !strings.isEmpty())
                .map(strings -> strings.stream())
                .orElse(Stream.empty())
                .collect(Collectors.toSet());
        result.addAll(users);
        return result;
    }

    public Set<String> getAllUsersInGroup(String groupName) {

        Set<String> groups = new HashSet<>();
        groups.add(groupName);
        groups.addAll( getAllParentGroupsRecursively(groups, new HashSet<>()) );

        return groups.stream()
                .map(this::getUsersInGroup)
                .flatMap(list -> list.stream())
                .collect(Collectors.toSet());
    }


    @Timed
    public Set<String> getUsersInMyGroups(String username) {

        // puo' capitare che un'utente ha *molti* gruppi
        // cerco di velocizzare le cose con un parallelStream

        ForkJoinPool forkJoinPool = new ForkJoinPool(3);
        Set<String> otherUsers = null;
        try {
            otherUsers = forkJoinPool.submit(
                    () -> getAllRolesForUser(username).parallelStream()     // recupero tutti i gruppi per l'utente richiesto
                            .filter(myGroup -> !myGroup.startsWith("abilitati#") && !myGroup.startsWith("USER")) //escludo i gruppi del tipo "abilitati#covid19" e "USER"
                            .map(myGroup -> getAllUsersInGroup(myGroup)) // per ogni gruppo recupero i suoi membri
                            .flatMap(list -> list.stream())           // ho uno stream di liste di stringhe che trasformo in uno stream di stringhe
                            .filter(user -> !user.equals(username))   // non mi interessa includere l'utente con cui ho chiamato
                            .collect(Collectors.toSet())
            ).get();
            return otherUsers;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getAllChildRolesRecursively(Set<String> resultSoFar, Set<String> visited) {

        log.trace("resultsSoFar {}, visited {}", resultSoFar, visited);
        Set<String> buffer = new HashSet<>();

        for (String group : resultSoFar) {

            Set<Relationship> children = relationshipService.getAllRelationshipForGroup(group);
            for (Relationship child : children) {
                if (!visited.contains(child.getGroupRelationship())) {
                    buffer.add(child.getGroupRelationship());
                }
            }

            if (group.contains("@")) {
                String role = group.substring(0, group.indexOf('@'));
                children = relationshipService.findRelationshipForStructure(role);

                for (Relationship child : children) {
                    if (!visited.contains(child.getGroupRelationship())) {
                        visited.add(group);
                        buffer.add(Utils.replaceStruttura(child.getGroupRelationship(), group.substring(group.indexOf('@'))));
                    }
                }
            }
        }

        if (!buffer.isEmpty())
            resultSoFar.addAll(getAllChildRolesRecursively(buffer, visited));

        return buffer;

    }


    private Set<String> getAllParentGroupsRecursively(Set<String> resultSoFar, Set<String> visited) {

        log.trace("resultsSoFar {}, visited {}", resultSoFar, visited);
        Set<String> buffer = new HashSet<>();

        for (String group : resultSoFar) {

            Set<Relationship> parents = relationshipService.getRelationshipsForGroupRelationship(group);
            for (Relationship parent : parents) {
                if (!visited.contains(parent.getGroupName())) {
                    buffer.add(parent.getGroupName());
                }
            }

            if (group.contains("@")) {
                String role = group.substring(0, group.indexOf('@'));
                parents = relationshipService.findRelationshipForStructureByGroupRelationship(role);

                for (Relationship parent : parents) {
                    if (!visited.contains(parent.getGroupName())) {
                        visited.add(group);
                        buffer.add(Utils.replaceStruttura(parent.getGroupName(), group.substring(group.indexOf('@'))));
                    }
                }
            }
        }

        if (!buffer.isEmpty())
            getAllParentGroupsRecursively(buffer, visited);

        return buffer;

    }

}
