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
        return membershipRepository.findOne(id);
    }

    /*
     *  delete the  membership by id.
     */
    public void delete(Long id) {
        log.debug("Request to delete Membership : {}", id);
        membershipRepository.delete(id);
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
    private Set<String> getLocalGroupsForUser(String username) {
        return membershipRepository.findGroupNamesForUser(username);
    }

    // Se a qualcuno dovesse servire puo' rendere questo metodo public, ma non credo - martin 4/9/19
    private Set<String> getAceGroupsForUser(String username) {
    	try {
	        return Optional.ofNullable(aceBridgeService)
	                .map(aceBridgeService -> aceBridgeService.getAceGroupsForUser(username))
	                .map(strings -> strings.stream())
	                .orElse(Stream.empty())
	                .collect(Collectors.toSet());
    	} catch (Exception e) {
    		log.debug(e.getMessage()); // Succede se admin chiede i gruppi da ace
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
    public Set<String> getAllGroupsForUser(String username) {

        Set<String> groups = new HashSet<>();

        groups.addAll( getAceGroupsForUser(username) );
        groups.addAll( getLocalGroupsForUser(username) );
        groups.add("USER");

        groups.addAll( getAllChildGroupsRecursively(groups, new HashSet<>()) );

        return groups;
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
                    () -> getAllGroupsForUser(username).parallelStream()     // recupero tutti i gruppi per l'utente richiesto
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

    private Set<String> getAllChildGroupsRecursively(Set<String> resultSoFar, Set<String> visited) {

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
            resultSoFar.addAll(getAllChildGroupsRecursively(buffer, visited));

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
