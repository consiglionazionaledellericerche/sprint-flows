package it.cnr.si.service;

import it.cnr.si.config.CustomKeyCloakAuthenticationProvider;
import it.cnr.si.config.KeycloakRole;
import it.cnr.si.config.SSOConfigurationProperties;
import it.cnr.si.domain.CNRUser;
import it.cnr.si.model.KeycloakUserInfo;
import it.cnr.si.model.SsoModelWebDto;
import it.cnr.si.model.UserInfoDto;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.IDToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by francesco on 05/10/15.
 */
@Service
@Profile("keycloak")
public class KeyCloakSecurityServiceImpl implements SecurityService, InitializingBean {

    private final Logger log = LoggerFactory.getLogger(KeyCloakSecurityServiceImpl.class);
    @Inject
    private CustomKeyCloakAuthenticationProvider customKeycloakAuthenticationProvider;
    @Inject
    private SSOConfigurationProperties properties;
    @Inject
    private KeycloakRestTemplate keycloakRestTemplate;

    public Optional<UserInfoDto> getUserInfo() {
        final ResponseEntity<KeycloakUserInfo> forEntity =
                keycloakRestTemplate.getForEntity(properties.getUserinfo_endpoint(), KeycloakUserInfo.class);
        return Optional.ofNullable(forEntity.getBody())
                .map(KeycloakUserInfo::getUserInfo);
    }

    public Optional<CNRUser> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof KeycloakAuthenticationToken) {
            KeycloakAuthenticationToken authentication1 = (KeycloakAuthenticationToken) authentication;
            OidcKeycloakAccount account = authentication1.getAccount();
            final Principal principal = account.getPrincipal();
            if (principal instanceof KeycloakPrincipal) {
                KeycloakPrincipal kPrincipal = (KeycloakPrincipal) principal;
                IDToken token = Optional.ofNullable(kPrincipal.
                        getKeycloakSecurityContext()
                        .getIdToken()).orElse(kPrincipal.
                        getKeycloakSecurityContext()
                        .getToken());
                CNRUser cnrUser = new CNRUser();
                cnrUser.setUsername(token.getPreferredUsername());
                cnrUser.setFirstName(token.getGivenName());
                cnrUser.setLastName(token.getFamilyName());
                cnrUser.setEmail(token.getEmail());
                cnrUser.setAuthorities(customKeycloakAuthenticationProvider
                        .mapAuthorities(account, new ArrayList<>())
                        .stream()
                        .collect(Collectors.toList())
                );
                if (customKeycloakAuthenticationProvider.isCNRUser(account)) {
                    cnrUser.setMatricola(Optional.ofNullable(customKeycloakAuthenticationProvider.getMatricola(account))
                            .orElse(Optional.ofNullable(customKeycloakAuthenticationProvider.getUsernameCNR(account)).orElse(token.getPreferredUsername())));
                }
                // 04/07/22 martin: voglio tutte le authorities da UserInfo in CNRUser
                addAuthoritiesFromUserInfo(cnrUser);
                
                return Optional.of(cnrUser);
            }
        } else {
            log.warn("user has not {}", KeycloakAuthenticationToken.class.getSimpleName());
        }

        return Optional.empty();

    }

    public Optional<String> getMatricola() {
        return getUser().map(item -> {
            return item.getMatricola();
        });
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user
     */
    public String getCurrentUserLogin() {
        return getUser()
                .map(cnrUser -> cnrUser.getUsername())
                .orElse(null);
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return getUser().isPresent();
    }

    /**
     * If the current user has a specific authority (security role).
     *
     * <p>The name of this method comes from the isUserInRole() method in the Servlet API</p>
     *
     * @param authority the authority to check
     * @return true if the current user has the authority, false otherwise
     */
    public boolean isCurrentUserInRole(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof KeycloakAuthenticationToken) {
            KeycloakAuthenticationToken authentication1 = (KeycloakAuthenticationToken) authentication;

            OidcKeycloakAccount account = authentication1.getAccount();

            if (customKeycloakAuthenticationProvider.isCNRUser(account)) {
                return customKeycloakAuthenticationProvider
                        .mapAuthorities(account, new ArrayList<>())
                        .stream()
                        .filter(grantedAuthority -> grantedAuthority.getAuthority().equalsIgnoreCase(authority))
                        .findAny().isPresent();
            }
        }
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Initialize SecurtityService of sprint-keycloak");
    }

    @Cacheable
    private void addAuthoritiesFromUserInfo(CNRUser cnrUser) {
        Collection<? extends GrantedAuthority> authorities = cnrUser.getAuthorities();
        Collection<GrantedAuthority> rolesFromUserInfo = new ArrayList<GrantedAuthority>();
        getUserInfo().ifPresent(userInfo -> {
            Set<SsoModelWebDto> roles = userInfo.getRoles();
            roles.forEach(role -> {
                String siglaRuolo = role.getSiglaRuolo();
                if (role.getEntitaOrganizzative().isEmpty()) {
                    rolesFromUserInfo.add(new SimpleGrantedAuthority(siglaRuolo+"@0000"));
                } else {
                    role.getEntitaOrganizzative().forEach(eo -> {
                        rolesFromUserInfo.add(new SimpleGrantedAuthority(siglaRuolo+"@"+eo.getId()));
                    });
                }
            });
        });

        rolesFromUserInfo.addAll(authorities);
        cnrUser.setAuthorities(rolesFromUserInfo);
    }
}
