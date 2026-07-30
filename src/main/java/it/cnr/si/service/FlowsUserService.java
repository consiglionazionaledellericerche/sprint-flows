package it.cnr.si.service;

import it.cnr.si.domain.Authority;
import it.cnr.si.domain.FlowsUser;
import it.cnr.si.domain.Membership;
import it.cnr.si.domain.Relationship;
import it.cnr.si.flows.ng.dto.FlowsUserDto;
import it.cnr.si.repository.AuthorityRepository;
import it.cnr.si.repository.FlowsUserRepository;
import it.cnr.si.repository.MembershipRepository;
import it.cnr.si.security.AuthoritiesConstants;

import it.cnr.si.service.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.HashSet;
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
//    @Autowired(required = false)
//    public JdbcTokenStore jdbcTokenStore;
    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;
    @Inject
    private FlowsUserRepository flowsUserRepository;
    @Inject
    private AuthorityRepository authorityRepository;
    @Inject
    private MembershipService membershipService;
    @Inject
    private RelationshipService relationshipService;
    @Inject
    private MembershipRepository membershipRepository;
    @Inject
    private CnrgroupService cnrgroupService;

    @Inject
    private SecurityService securityService;

    public Optional<FlowsUser> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return flowsUserRepository.findOneByActivationKey(key)
                .map(user -> {
                    // activate given user for the registration key.
                    user.setActivated(true);
                    user.setActivationKey(null);
                    flowsUserRepository.save(user);
                    log.debug("Activated user: {}", user);
                    return user;
                });
    }


    public Optional<FlowsUser> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);

        return flowsUserRepository.findOneByResetKey(key)
                .filter(user -> {
                    ZonedDateTime oneDayAgo = ZonedDateTime.now().minusHours(24);
                    return user.getResetDate().isAfter(oneDayAgo);
                })
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    flowsUserRepository.save(user);
                    return user;
                });
    }


    public Optional<FlowsUser> requestPasswordReset(String mail) {
        return flowsUserRepository.findOneByEmail(mail)
                .filter(FlowsUser::getActivated)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(ZonedDateTime.now());
                    flowsUserRepository.save(user);
                    return user;
                });
    }


    public FlowsUser createUser(
            String login, String password, String firstName, String lastName, String email,
            String langKey, String phone) {


        // TODO: Questo metodo non funziona, ma tanto non dobbiamo creare utenti locali
        FlowsUser newUser = new FlowsUser();
        // Authority authority = authorityRepository.findOne(AuthoritiesConstants.USER).get();
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(password);
        newUser.setLogin(login);
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setEmail(email);
        newUser.setLangKey(langKey);
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        // authorities.add(authority);
        newUser.setAuthorities(authorities);
        newUser.setPhone(phone);
        flowsUserRepository.save(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }


    public FlowsUser createUser(FlowsUserDto flowsUseDto) {
        // TODO: Questo metodo non funziona, ma tanto non dobbiamo creare utenti locali
        FlowsUser user = new FlowsUser();
        user.setLogin(flowsUseDto.getLogin());
        user.setFirstName(flowsUseDto.getFirstName());
        user.setLastName(flowsUseDto.getLastName());
        user.setEmail(flowsUseDto.getEmail());
        if (flowsUseDto.getLangKey() == null) {
            user.setLangKey("en"); // default language
        } else {
            user.setLangKey(flowsUseDto.getLangKey());
        }
        if (flowsUseDto.getAuthorities() != null) {
            Set<Authority> authorities = new HashSet<>();
            // flowsUseDto.getAuthorities().stream().forEach(
            //         authority -> authorities.add(authorityRepository.findOne(authority))
            // );
            user.setAuthorities(authorities);
        }
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(ZonedDateTime.now());
        user.setActivated(true);
        user.setPhone(flowsUseDto.getPhone());
        user.setGender(flowsUseDto.getGender());
        flowsUserRepository.save(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }


    public void updateUser(
            Long id, String login, String firstName, String lastName, String email,
            boolean activated, String langKey, Set<String> authorities, String phone, String gender) {
        // TODO: Questo metodo non funziona, ma tanto non dobbiamo creare utenti locali
        flowsUserRepository
                .findOneById(id)
                .ifPresent(u -> {
                    u.setLogin(login);
                    u.setFirstName(firstName);
                    u.setLastName(lastName);
                    u.setEmail(email);
                    u.setActivated(activated);
                    u.setLangKey(langKey);
                    Set<Authority> managedAuthorities = u.getAuthorities();
                    managedAuthorities.clear();
                    // authorities.stream().forEach(
                    //         authority -> managedAuthorities.add(authorityRepository.findOne(authority))
                    // );
                    u.setPhone(phone);
                    u.setGender(gender);
                    log.debug("Changed Information for User: {}", u);
                });
    }


    public void deleteUser(String login) {
//        jdbcTokenStore.findTokensByUserName(login).stream().forEach(token ->
//                                                                        jdbcTokenStore.removeAccessToken(token));
        flowsUserRepository.findOneByLogin(login).ifPresent(u -> {
            flowsUserRepository.delete(u);
            log.debug("Deleted User: {}", u);
        });
    }


    public void changePassword(String password) {
        flowsUserRepository.findOneByLogin(securityService.getCurrentUserLogin()).ifPresent(u -> {
            String encryptedPassword = passwordEncoder.encode(password);
            u.setPassword(encryptedPassword);
            flowsUserRepository.save(u);
            log.debug("Changed password for User: {}", u);
        });
    }


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
    public FlowsUser getUserWithAuthorities(Long id) {
        FlowsUser user = flowsUserRepository.findById(id).get();
        user.getAuthorities().size(); // eagerly load the association
        return user;
    }


    @Transactional(readOnly = true)
    public FlowsUser getUserWithAuthorities() {
        FlowsUser user = flowsUserRepository.findOneByLogin(securityService.getCurrentUserLogin()).orElse(null);
        if (user != null)
            user.getAuthorities().size(); // eagerly load the association

        return user;
    }


    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     * </p>
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        ZonedDateTime now = ZonedDateTime.now();
        List<FlowsUser> users = flowsUserRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minusDays(3));
        for (FlowsUser user : users) {
            log.debug("Deleting not activated user {}", user.getLogin());
            flowsUserRepository.delete(user);
        }
    }

    public List<FlowsUser> searchByLogin(String username) {
        return flowsUserRepository.searchByLogin(username);
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
 