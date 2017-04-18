package it.cnr.si.flows.ng.ldap;

import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import it.cnr.si.config.ldap.CustomAuthoritiesPopulator;
import it.cnr.si.service.MembershipService;


public class FlowsCustomAuthoritiesPopulator extends CustomAuthoritiesPopulator {

    private MembershipService membershipService;

    public FlowsCustomAuthoritiesPopulator (MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    public Collection<GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        Collection<GrantedAuthority> auths = super.getGrantedAuthorities(userData, username);
        auths.addAll(membershipService.getGroupsForUser(username).stream()
            .map(a -> new SimpleGrantedAuthority(a))
            .collect(Collectors.toList()));

        return auths;
    }

}
