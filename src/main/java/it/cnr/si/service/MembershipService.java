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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.*;
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

    /**
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

    // TODO attenzione: questo metodo, a differenza di getAceGroupsForUser aggiunge i ROLE_
    public Set<String> getLocalGroupsForUser(String username) {
        return membershipRepository.findGroupNamesForUser(username);
    }


    public Set<String> getAceGroupsForUser(String username) {
        return Optional.ofNullable(aceBridgeService)
                .map(aceBridgeService -> aceBridgeService.getAceGroupsForUser(username))
                .map(strings -> strings.stream())
                .orElse(Stream.empty())
                .collect(Collectors.toSet());
    }

    /**
     * Dato uno username restituisce tutti suoi gruppi, sia di ACE che di Membership,
     * compresi quelli ereditati
     *
     * @param username
     * @return
     */
    public Set<String> getAllGroupsForUser(String username) {

        Set<String> groups = new HashSet<>();
        groups.addAll( getAceGroupsForUser(username) );
        groups.addAll( getLocalGroupsForUser(username) );

        groups.addAll( getAllChildGroupsRecursively(groups, new HashSet<>()) );

        return groups;
    }

    /* --- */

    @Deprecated
    public List<String> getUsersInGroup(String groupName) {
        List<String> result = membershipRepository.findMembersInGroup(groupName);
        Set<String> users = getUsersInAceGroup(groupName);
        result.addAll(users);
        return result;
    }

    @SuppressWarnings("deprecation") // Questo e' il modo giusto di usare il metodo aceBridgeService.getUsersInAceGroup
    public Set<String> getUsersInAceGroup(String groupName) {
        return Optional.ofNullable(aceBridgeService)
                .map(aceBridgeService -> aceBridgeService.getUsersInAceGroup(groupName))
                .filter(strings -> !strings.isEmpty())
                .map(strings -> strings.stream())
                .orElse(Stream.empty())
                .collect(Collectors.toSet());
    }

    // TODO fix Optional vs NPE
    // TODO questo metodo va a beccare solo ACE e non Membership
    public Set<String> getUsersInACEGroups(Collection<String> myGroups) {
        Set<String> result = new HashSet<>();
        for (String myGroup : myGroups)
            result.addAll(getUsersInAceGroup(myGroup));
        return result;
    }


    public Set<String> getAllUsersInGroup(String groupName) {

        Set<String> groups = new HashSet<>();
        groups.add(groupName);
        groups.addAll( getAllParentGroupsRecursively(groups, new HashSet<>()) );

        return getUsersInACEGroups(groups);
    }



    @Timed
    public List<String> getUsersInMyGroups(String username) {

        List<String> usersInMyGroups = new ArrayList<>();

        Set<String> newGroups = getAllGroupsForUser(username);

        List<String> myGroups = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .parallelStream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .filter(group -> group.indexOf("afferenza") <= -1)
                .filter(group -> group.indexOf("USER") <= -1)
                .filter(group -> group.indexOf("DEPARTMENT") <= -1)
                .filter(group -> group.indexOf("PREVIOUS") <= -1)
                .collect(Collectors.toList());

        if (!Arrays.asList(env.getActiveProfiles()).contains("oiv")) {
            //filtro in ACE gli utenti che appartengono agli stessi gruppi dell'utente loggato
            usersInMyGroups.addAll(getUsersInACEGroups(myGroups));
        } else {
            //filtro in Membership gli utenti che appartengono agli stessi gruppi dell'utente loggato
            for (String myGroup : myGroups) {
                // se qui dovesse throware null,
                // reipostare usersInMyGroups.addAll(membershipService.findMembersInGroup(myGroup) != null ? membershipService.findMembersInGroup(myGroup) : new ArrayList<>());
                // Martin
                usersInMyGroups.addAll(getUsersInGroup(myGroup));
            }
        }

        usersInMyGroups = usersInMyGroups.stream()
                .distinct()
                .filter(user -> !user.equals(username))
                .collect(Collectors.toList());

        return usersInMyGroups;
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
