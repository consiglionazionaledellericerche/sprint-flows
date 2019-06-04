package it.cnr.si.service;

import it.cnr.si.domain.FlowsUser;
import it.cnr.si.domain.Membership;
import it.cnr.si.domain.Relationship;
import it.cnr.si.repository.FlowsUserRepository;
import it.cnr.si.repository.MembershipRepository;
import it.cnr.si.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.RoleOiv.member;


/**
 * Service class per la gestione dei FlowsUser: usiamo un "nostro" service
 * (e non quello di sprint-core) perchè abbiamo dei campi customizzati (phone).
 */
@Service
@Transactional
public class FlowsUserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);
    @Inject
    private FlowsUserRepository flowsUserRepository;
    @Inject
    private MembershipService membershipService;
    @Inject
    private RelationshipService relationshipService;
    @Inject
    private MembershipRepository membershipRepository;
    @Inject
    private CnrgroupService cnrgroupService;



    @Transactional(readOnly = true)
    public Optional<FlowsUser> getUserWithAuthoritiesByLogin(String login) {
//        todo: cambiarlo perchè non prendiamo più gli utenti dal DB ma da ace
//        aceBridgeService.getAceGroupsForUser(login); //restituisce i gruppi di cui l'utente fa parte ma non il FlowsUser
//        UserDetails user = flowsUserDetailsService.loadUserByUsername(login); //restituisce un UserDetails e devo "costruirmi" il FlowsUser

        return flowsUserRepository.findOneByLogin(login).map(u -> {
            u.getAuthorities().size();
            return u;
        });
    }



    @Transactional(readOnly = true)
    public FlowsUser getUserWithAuthorities() {
        FlowsUser user = flowsUserRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).orElse(null);
        if (user != null)
            user.getAuthorities().size(); // eagerly load the association

        return user;
    }



    public List<Membership> getGroupsForUser(String user, Pageable pageable) {

        //recupero le mebership in cui l'utente ha "role" member
        Page<Membership> pageMember = membershipService.getGroupsWithRole(pageable, user, member.name());
        //recupero i gruppi di cui l'utente fa parte sia come "coordinator" che come "member"
        List<Membership> userGroup = membershipRepository.getGroupForUser(user);

        //di quelli di cui è "member" recupero anche le relationship
        for (Membership membership : pageMember.getContent()) {
            String groupname = membership.getCnrgroup().getName();
            //le membership che recupero dalle relatrionship devono avere lo stesso grouprole indicato nella relationship
            Set<Relationship> relationships = relationshipService.getAllRelationshipForGroup(groupname);
            for (Relationship relationship : relationships) {
                Membership membershipFromRelationship = new Membership();

                membershipFromRelationship.setGrouprole(relationship.getGroupRole());
                membershipFromRelationship.setUser(membership.getUser());
                membershipFromRelationship.setCnrgroup(cnrgroupService.findCnrgroupByName(relationship.getGroupRelationship()));

                userGroup.add(membershipFromRelationship);
            }
        }
        //tolgo i gruppi "duplicati"
        userGroup = userGroup.stream().distinct().collect(Collectors.toList());
        return userGroup;
    }
}
