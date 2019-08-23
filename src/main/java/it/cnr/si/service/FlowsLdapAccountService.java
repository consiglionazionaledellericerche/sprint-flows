package it.cnr.si.service;

import it.cnr.si.config.ldap.CNRUser;
import it.cnr.si.security.LdapSecurityUtils;
import it.cnr.si.web.rest.dto.CNRUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.ContainerCriteria;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing users.
 */
@Service
@Transactional
@Primary
@Profile("!oiv")
public class FlowsLdapAccountService extends LdapAccountService {

    private final static Logger log = LoggerFactory.getLogger(LdapAccountService.class);

    @Inject
    private LdapTemplate ldapTemplate;

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

        List<String> roles = getRoles(LdapSecurityUtils.getAuthentication().getAuthorities()); // Meglio cambiare il file in sprint-ldap?

        log.info(roles.toString());

        return new CNRUserDTO(username, null, matricola, firstName, lastName, email, null, roles, departmentNumber);
    }

    private static List<String> getRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities
                .stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getFulluser(String username) {

        ContainerCriteria criteria = LdapQueryBuilder
                .query()
                .where("objectClass").is("cnrPerson")
                .and("uid").is(username);

        return ldapTemplate.search( criteria, new AttributesMapper() {
            @Override
            public Map<String, String> mapFromAttributes(Attributes attributes) throws NamingException {

                Map<String, String> result = new HashMap<>();

                NamingEnumeration<? extends Attribute> allAttributes = attributes.getAll();

//                while (allAttributes.hasMore()) {
//                    Attribute attribute = allAttributes.next();
//                    result.put( attribute.getID(), String.valueOf(attribute.get()) );
//                }

                result.put("uid", attributes.get("uid").get().toString());
                result.put("cnrextra3", attributes.get("cnrextra3").get().toString());
                result.put("departmentNumber", attributes.get("departmentNumber").get().toString());

                return result;
            }
        });
    }


}
