package it.cnr.si.flows.ng.config;


import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.validation.constraints.Email;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.service.SecurityService;

@Component
public class LogPrologSettingFilter extends GenericFilterBean {

    @Inject
    private SecurityService securityService;    
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (securityService.isAuthenticated()) {
                if (getImpersonatorName().isPresent())
                    MDC.put("currentUser", getImpersonatorName().get() + " as " + securityService.getCurrentUserLogin());
                else 
                    MDC.put("currentUser", securityService.getCurrentUserLogin());
            } else {
                MDC.put("currentUser", "not logged in");
            }
        } catch (Exception e) {
            MDC.put("currentUser", "undefined");
        }
        chain.doFilter(request, response);
    }
    
    private Optional<String> getImpersonatorName() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof KeycloakPrincipal<?>) {
                KeycloakPrincipal<KeycloakSecurityContext> keycloakPrincipal = (KeycloakPrincipal<KeycloakSecurityContext>) principal;
                Object impersonator = keycloakPrincipal.getKeycloakSecurityContext().getToken().getOtherClaims().get("impersonator");
                if (impersonator != null) {
                    LinkedHashMap<String, String> i = (LinkedHashMap<String, String>)impersonator;
                    return Optional.of(String.valueOf(i.get("username")));
                }
            }
        } catch (Exception e) {}
        return Optional.empty();
    }

}
