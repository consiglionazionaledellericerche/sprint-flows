package it.cnr.si.flows.ng.ldap;

import it.cnr.si.service.RelationshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Modified my mtrycz on 26/05/17.
 */
public class FlowsAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    public static final String DEPARTMENT_NUMBER = "departmentnumber";
    public static final String AUTHORITY_PREFIX = "DEPARTMENT_";

    private final Logger log = LoggerFactory.getLogger(FlowsAuthoritiesPopulator.class);

    @Inject
    private RelationshipService relationshipService;

    @Override
    public Collection<GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {

        log.debug("security LDAP LdapAuthoritiesPopulator");

        ArrayList<GrantedAuthority> list = new ArrayList<>();
        list.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (userData != null && userData.attributeExists(DEPARTMENT_NUMBER)) {
            String departmentNumber = userData.getStringAttribute(DEPARTMENT_NUMBER);
            log.debug("add authority {} to user {}", departmentNumber, username);
            list.add(new SimpleGrantedAuthority(AUTHORITY_PREFIX + departmentNumber));
        } else {
            log.debug("no attribute {} defined for user {}", DEPARTMENT_NUMBER, username);
        }

        List<GrantedAuthority> fullGrantedAuthorities = relationshipService.getAllAdditionalGroupsForUser(username);
        list.addAll(fullGrantedAuthorities);
        log.info("Full Groups, including from local membership {}", fullGrantedAuthorities);

        return list;
    }
}