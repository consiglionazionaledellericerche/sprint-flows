package it.cnr.si.service;

import it.cnr.si.domain.Membership;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.MembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    @Inject
    private AceBridgeService aceService;

    /**
     * Save a membership.
     *
     * @param membership the entity to save
     * @return the persisted entity
     */
    public Membership save(Membership membership) {
        log.debug("Request to save Membership : {}", membership);
        Membership result = membershipRepository.save(membership);
        return result;
    }

    /**
     *  Get all the memberships.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Membership> findAll(Pageable pageable) {
        log.debug("Request to get all Memberships");
        Page<Membership> result = membershipRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get one membership by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Membership findOne(Long id) {
        log.debug("Request to get Membership : {}", id);
        Membership membership = membershipRepository.findOne(id);
        return membership;
    }

    /**
     *  Delete the  membership by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Membership : {}", id);
        membershipRepository.delete(id);
    }

    public Set<String> getGroupsForUser(String username) {
        return membershipRepository.findGroupsForUsername(username);
    }

    public List<GrantedAuthority> getAllAdditionalAuthoritiesForUser(String username) {
        return Stream.concat(getGroupsForUser(username).stream(), getACEGroupsForUser(username).stream())
                .distinct()
                .map(Utils::addLeadingRole)
                .map(g -> new SimpleGrantedAuthority(g))
                .collect(Collectors.toList());
    }

    private Set<String> getACEGroupsForUser(String username) {

        return new HashSet<String>(aceService.getAceGroupsForUser(username));
    }

    public List<String> findMembersInGroup(String groupName) {

        List<String> result = membershipRepository.findMembersInGroup(groupName);

        // TODO Recuperare utenti di un gruppo ACE (quando ci sara')

        return result;
    }
}
