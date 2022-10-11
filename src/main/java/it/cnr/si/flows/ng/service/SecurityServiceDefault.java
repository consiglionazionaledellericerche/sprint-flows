package it.cnr.si.flows.ng.service;

import it.cnr.si.domain.CNRUser;
import it.cnr.si.model.UserInfoDto;
import it.cnr.si.service.SecurityService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Profile("iss")
public class SecurityServiceDefault implements SecurityService, InitializingBean {
    private final Logger log = LoggerFactory.getLogger(SecurityServiceDefault.class);
    @Override
    public Optional<CNRUser> getUser() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMatricola() {
        return Optional.empty();
    }

    @Override
    public Optional<UserInfoDto> getUserInfo() {
        return Optional.empty();
    }

    @Override
    public String getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        String userName = null;
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails springSecurityUser = (UserDetails)authentication.getPrincipal();
                userName = springSecurityUser.getUsername();
            } else if (authentication.getPrincipal() instanceof String) {
                userName = (String)authentication.getPrincipal();
            }
        }

        return userName;
    }

    @Override
    public boolean isAuthenticated() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Collection<? extends GrantedAuthority> authorities = securityContext.getAuthentication().getAuthorities();
        if (authorities != null) {
            Iterator var2 = authorities.iterator();

            while(var2.hasNext()) {
                GrantedAuthority authority = (GrantedAuthority)var2.next();
                if (authority.getAuthority().equals("ROLE_ANONYMOUS")) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean isCurrentUserInRole(String s) {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails springSecurityUser = (UserDetails)authentication.getPrincipal();
            return springSecurityUser.getAuthorities().contains(new SimpleGrantedAuthority(s));
        } else {
            return false;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info(" SecurtityService of SecurityServiceDefault");
    }
}
