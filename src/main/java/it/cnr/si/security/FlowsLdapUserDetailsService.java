package it.cnr.si.security;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.util.Assert;

import it.cnr.si.service.MembershipService;

public class FlowsLdapUserDetailsService {
    private final LdapUserSearch userSearch;
    private final LdapAuthoritiesPopulator authoritiesPopulator;
    private UserDetailsContextMapper userDetailsMapper = new LdapUserDetailsMapper();
    private MembershipService membershipService;

    public FlowsLdapUserDetailsService(LdapUserSearch userSearch,
            LdapAuthoritiesPopulator authoritiesPopulator, MembershipService membershipService) {
        Assert.notNull(userSearch, "userSearch must not be null");
        Assert.notNull(authoritiesPopulator, "authoritiesPopulator must not be null");
        Assert.notNull(membershipService, "membershipService must not be null");
        this.userSearch = userSearch;
        this.authoritiesPopulator = authoritiesPopulator;
        this.membershipService = membershipService;
    }

    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        DirContextOperations userData = userSearch.searchForUser(username);

        Collection<GrantedAuthority> grantedAuthorities = (Collection<GrantedAuthority>) authoritiesPopulator.getGrantedAuthorities(userData, username);
        grantedAuthorities.addAll(
                membershipService.getGroupsForUser(username).stream()
                .map(groupname -> new SimpleGrantedAuthority(groupname))
                .collect(Collectors.toList()));

        return userDetailsMapper.mapUserFromContext(userData, username,
                grantedAuthorities);
    }

    public void setUserDetailsMapper(UserDetailsContextMapper userDetailsMapper) {
        Assert.notNull(userDetailsMapper, "userDetailsMapper must not be null");
        this.userDetailsMapper = userDetailsMapper;
    }
}
