package it.cnr.si.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.cnr.si.config.ldap.CNRUser;
import it.cnr.si.service.LdapAccountService;
import it.cnr.si.web.rest.dto.CNRUserDTO;

/**
 * Service class for managing users.
 */
@Service
@Transactional
@Primary
public class FlowsLdapAccountService extends LdapAccountService {

    private final static Logger log = LoggerFactory.getLogger(LdapAccountService.class);

    public CNRUserDTO getAccount() {

        UserDetails user = (UserDetails) LdapSecurityUtils.getAuthentication().getPrincipal();

        log.info("user: " + user);

        String matricola;
        String email;
        String departmentNumber;
        String firstName;
        String lastName;

        if (user instanceof CNRUser) {
            CNRUser cnrUser = (CNRUser) user;
            matricola = cnrUser.getMatricola();
            email = cnrUser.getEmail();
            departmentNumber = cnrUser.getDepartmentNumber();
            lastName = cnrUser.getLastName();
            firstName = cnrUser.getFirstName();
        } else {
            email = null;
            matricola = null;
            firstName = null;
            lastName = null;
            departmentNumber = null;
        }

        String username = user.getUsername();
        log.info(username);

        List<String> roles = getRoles(LdapSecurityUtils.getAuthentication().getAuthorities());

        log.info(roles.toString());

        return new CNRUserDTO(username, null, matricola, firstName, lastName, email, null, roles, departmentNumber);
    }

    private static List<String> getRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities
                .stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());
    }

}
