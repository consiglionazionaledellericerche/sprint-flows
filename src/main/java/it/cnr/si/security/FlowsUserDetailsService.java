package it.cnr.si.security;

import it.cnr.si.domain.User;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.UserRepository;
import it.cnr.si.service.RelationshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("flowsUserDetailsService")
@Primary
public class FlowsUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(FlowsUserDetailsService.class);

    @Inject
    private UserRepository userRepository;
    @Inject
    private LdapUserDetailsService ldapUserDetailsService;
    @Inject
    private RelationshipService relationshipService;
    @Inject
    private Environment env;
    @Inject
    private Utils utils;


    @Override
    @Transactional
    //la cache "user" attivata solo per il profilo cnr perchè in oiv abbiamo pochi utenti e la cache fa casini con la creazione dei nuovi utenti
    @Cacheable(value = "user", key = "#login", condition = "@utils.isProfileActive(\"cnr\")")
    public UserDetails loadUserByUsername(final String login) {
        UserDetails userDetails = null;

        log.debug("Loading User {}", login);
        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        Optional<User> userFromDatabase = userRepository.findOneByLogin(lowercaseLogin);
        if (userFromDatabase.isPresent()) {
            userDetails = userFromDatabase.map(user -> {
                if (!user.getActivated()) {
                    throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
                }
                List<GrantedAuthority> grantedAuthorities = user.getAuthorities().stream()
                        .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                        .collect(Collectors.toList());
                grantedAuthorities.addAll(relationshipService.getAllGroupsForUser(lowercaseLogin));

                return new org.springframework.security.core.userdetails.User(lowercaseLogin,
                                                                              user.getPassword(),
                                                                              grantedAuthorities);
            }).orElseGet(null);
        } else {
            List<String> profiles = Arrays.asList(env.getActiveProfiles());
            if (profiles.contains("cnr"))
                userDetails = ldapUserDetailsService.loadUserByUsername(login);
//            else if(profiles.contains("oiv")){} implementare l'impersonate per gli utenti oiv'   todo: implementare l'impersonate per gli utenti oiv
        }

        if (userDetails == null)
            throw new UsernameNotFoundException("User " + lowercaseLogin + " was not found in the " +
                                                        "database or LDAP");
        else
            return userDetails;
    }
}
