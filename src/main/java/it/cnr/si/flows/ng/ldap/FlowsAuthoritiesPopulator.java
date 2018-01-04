package it.cnr.si.flows.ng.ldap;

import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modified my mtrycz on 26/05/17.
 */
public class FlowsAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    public static final String DEPARTMENT_NUMBER = "departmentnumber";
    public static final String AUTHORITY_PREFIX = "DEPARTMENT_";

    private final Logger log = LoggerFactory.getLogger(FlowsAuthoritiesPopulator.class);

    @Inject
    private RelationshipService relationshipService;
    @Inject
    private MembershipService membershipService;
    @Inject
    private Environment env;
    
    @CacheEvict(value = "allGroups", key = "#username")
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

        List<GrantedAuthority> fullGrantedAuthorities = relationshipService.getAllGroupsForUser(username);
        list.addAll(fullGrantedAuthorities);
        
        log.info("Full Groups, including from local relationship {}", fullGrantedAuthorities);

        return list;
    }
}