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
        CNRUser user = (CNRUser) super.mapUserFromContext(dirContextOperations, username, grantedAuthorities);

        List<GrantedAuthority> fullGrantedAuthorities = membershipService.getAllAdditionalAuthoritiesForUser(username);
        fullGrantedAuthorities.addAll(grantedAuthorities);
        log.info("Full Groups, including from local membership {}", fullGrantedAuthorities);

        user.setAuthorities(fullGrantedAuthorities);
        return user;
    }
}
