package it.cnr.si.flows.ng.ldap;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import it.cnr.si.config.ldap.CNRUser;
import it.cnr.si.config.ldap.LdapUserMapper;
import it.cnr.si.service.MembershipService;

@Component
@Primary
public class FlowsLdapUserMapper extends LdapUserMapper {

    private final Logger log = LoggerFactory.getLogger(LdapUserMapper.class);

    @Inject
    private MembershipService membershipService;

    @Override
    public UserDetails mapUserFromContext(DirContextOperations dirContextOperations, String username, Collection<? extends GrantedAuthority> grantedAuthorities) {

        String matricola = dirContextOperations.getStringAttribute(MATRICOLA);
        String email = dirContextOperations.getStringAttribute(MAIL);

        String departmentNumber = dirContextOperations.getStringAttribute("departmentNumber");
        String lastName = dirContextOperations.getStringAttribute("cnrcognome");
        String firstName = dirContextOperations.getStringAttribute("cnrnome");

        List<GrantedAuthority> fullGrantedAuthorities = membershipService.getGroupsForUser(username).stream()
                .map(g-> new SimpleGrantedAuthority(g))
                .collect(Collectors.toList());
        fullGrantedAuthorities.addAll(grantedAuthorities);
        log.info("user {}, matricola {}, email {}, groups {}", username, matricola, email, fullGrantedAuthorities.stream().map(s -> s.getAuthority()).collect(Collectors.joining(", ")));



        CNRUser user = new CNRUser();

        user.setUsername(username);
        user.setMatricola(matricola);
        user.setAuthorities(fullGrantedAuthorities);
        user.setEmail(email);

        user.setDepartmentNumber(departmentNumber);
        user.setLastName(lastName);
        user.setFirstName(firstName);

        return user;
    }
}
