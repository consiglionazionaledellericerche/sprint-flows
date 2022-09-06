package it.cnr.si.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.jfree.util.Log;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.IDToken;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import it.cnr.si.domain.CNRUser;
import it.cnr.si.model.FlowsKeycloakAuthenticationToken;
import it.cnr.si.model.KeycloakUserInfo;
import it.cnr.si.model.SsoModelWebDto;
import it.cnr.si.model.UserInfoDto;
import it.cnr.si.security.AuthoritiesConstants;

public class CustomKeyCloakAuthenticationProvider extends KeycloakAuthenticationProvider {

    public static final String CONTEXTS = "contexts";
    public static final String ROLES = "roles";
    public static final String ROLESEO = "rolesEo";
    @Inject
    private SSOConfigurationProperties properties;
    @Inject
    private KeycloakRestTemplate keycloakRestTemplate;
    
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) authentication;
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String role : token.getAccount().getRoles()) {
            grantedAuthorities.add(new KeycloakRole(role));
        }

        if ("app.scrivaniadigitale".equals(authentication.getName())) {
            grantedAuthorities.add(new KeycloakRole("ROLE_ADMIN"));
        }
        
        Optional<UserInfoDto> userInfo = getUserInfo();
        Optional<CNRUser> cnrUser = getUser(token.getAccount(), userInfo);
        Collection<? extends GrantedAuthority> mappedAuthorities = mapAuthorities(token.getAccount(), grantedAuthorities, userInfo);
        cnrUser.ifPresent(u -> u.setAuthorities(mappedAuthorities));
        
        return new FlowsKeycloakAuthenticationToken(token.getAccount(),
                                                    token.isInteractive(),
                                                    mappedAuthorities,
                                                    cnrUser.orElse(null),
                                                    userInfo.orElse(null));
    }

    public boolean isCNRUser(OidcKeycloakAccount account) {
        return Optional.ofNullable(account.
                getKeycloakSecurityContext()
                .getIdToken()).orElse(account.
                        getKeycloakSecurityContext()
                        .getToken())
            .getOtherClaims()
            .entrySet()
            .stream()
            .filter(stringObjectEntry -> stringObjectEntry.getKey().equalsIgnoreCase(properties.getUser()))
            .findAny()
            .map(stringObjectEntry -> stringObjectEntry.getValue())
            .filter(Boolean.class::isInstance)
            .map(Boolean.class::cast)
            .orElse(Boolean.FALSE);
    }

    public String getMatricola(OidcKeycloakAccount account) {
        return Optional.ofNullable(account.
                        getKeycloakSecurityContext()
                        .getIdToken()).orElse(account.
                        getKeycloakSecurityContext()
                        .getToken())
            .getOtherClaims()
            .entrySet()
            .stream()
            .filter(stringObjectEntry -> stringObjectEntry.getKey().equalsIgnoreCase(properties.getMatricola()))
            .findAny()
            .map(stringObjectEntry -> stringObjectEntry.getValue())
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse(null);
    }

    public String getUsernameCNR(OidcKeycloakAccount account) {
        return Optional.ofNullable(account.
                        getKeycloakSecurityContext()
                        .getIdToken()).orElse(account.
                        getKeycloakSecurityContext()
                        .getToken())
                .getOtherClaims()
                .entrySet()
                .stream()
                .filter(stringObjectEntry -> stringObjectEntry.getKey().equalsIgnoreCase(properties.getUsername_cnr()))
                .findAny()
                .map(stringObjectEntry -> stringObjectEntry.getValue())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElse(null);
    }

    public String getLivello(OidcKeycloakAccount account) {
        return Optional.ofNullable(account.
                        getKeycloakSecurityContext()
                        .getIdToken()).orElse(account.
                        getKeycloakSecurityContext()
                        .getToken())
            .getOtherClaims()
            .entrySet()
            .stream()
            .filter(stringObjectEntry -> stringObjectEntry.getKey().equalsIgnoreCase(properties.getLivello()))
            .findAny()
            .map(stringObjectEntry -> stringObjectEntry.getValue())
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .orElse(null);
    }

    public Collection<? extends GrantedAuthority> mapAuthorities(OidcKeycloakAccount account,
                                                                List<GrantedAuthority> grantedAuthorities,
                                                                Optional<UserInfoDto> userInfo) {
        if (isCNRUser(account)) {
            grantedAuthorities.add(new KeycloakRole(AuthoritiesConstants.USER));
        }
        
        // mappo le authorities dal token
        final Optional<Map.Entry<String, Object>> contexts = Optional.ofNullable(account.
                        getKeycloakSecurityContext()
                        .getIdToken()).orElse(account.
                        getKeycloakSecurityContext()
                        .getToken())
            .getOtherClaims()
            .entrySet()
            .stream()
            .filter(stringObjectEntry -> stringObjectEntry.getKey().equalsIgnoreCase(CONTEXTS))
            .findAny();
        if (contexts.isPresent()) {
            final Stream<Map.Entry> stream = contexts
                .map(stringObjectEntry -> stringObjectEntry.getValue())
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(map -> map.entrySet())
                .get()
                .stream()
                .filter(Map.Entry.class::isInstance)
                .map(Map.Entry.class::cast);
            
            final Optional<Map.Entry> any1 = stream
                .filter(entry -> entry.getKey().equals(properties.getContesto()))
                .findAny();
            if (any1.isPresent()) {
                final Optional<Map<String, List<?>>> mapRoles =
                    Optional.ofNullable(any1.get().getValue())
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast);
                if (mapRoles.isPresent()) {
                    final Optional<? extends List<?>> rolesEo = mapRoles
                             .get()
                             .entrySet()
                             .stream()
                             .filter(stringEntry -> stringEntry.getKey().equalsIgnoreCase(ROLESEO))
                             .map(Map.Entry::getValue)
                             .findAny();
                    if (rolesEo.isPresent()) {
                        rolesEo.get().stream()
                                .filter(Map.class::isInstance)
                                .map(Map.class::cast)
                                .forEach(s -> {
                                    String siglaRuolo = (String)s.get("siglaRuolo");
                                    final Optional<List<Map>> mapEntorg = Optional.ofNullable(s.get("entitaOrganizzative"))
                                            .filter(List.class::isInstance)
                                            .map(List.class::cast);
                                    Set<String> entorg = mapEntorg.get().stream()
                                            .map(el->el.get("idnsip"))
                                            .filter(String.class::isInstance)
                                            .map(String.class::cast)
                                            .collect(Collectors.toSet());

                                    grantedAuthorities.add(new KeycloakRole(siglaRuolo, entorg));
                        });
                    }

                    final Optional<? extends List<?>> roles = mapRoles
                            .get()
                            .entrySet()
                            .stream()
                            .filter(stringEntry -> stringEntry.getKey().equalsIgnoreCase(ROLES))
                            .map(Map.Entry::getValue)
                            .findAny();

                    roles.ifPresent(objects -> objects.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .filter(s->grantedAuthorities.stream().map(GrantedAuthority::getAuthority).noneMatch(auth->auth.equals(s)))
                            .forEach(s -> grantedAuthorities.add(new KeycloakRole(s))));
                }
            }
        }
        
        // mappo le authorities da UserInfo
        if (properties.getMap_roles_from_user_info()) {
            userInfo.ifPresent(info -> {
                Set<SsoModelWebDto> roles = info.getRoles();
                roles.forEach(role -> {
                    String siglaRuolo = role.getSiglaRuolo();
                    if (role.getEntitaOrganizzative().isEmpty()) {
                        grantedAuthorities.add(new KeycloakRole(siglaRuolo +"@"+ "0000"));
                    } else {
                        role.getEntitaOrganizzative().stream()
                            .forEach(eo -> {
                                String sigla = siglaRuolo;
                                if ("responsabile-struttura-ruolo".equals(sigla))
                                    sigla = "responsabile-struttura";
                                grantedAuthorities.add(new KeycloakRole(sigla +"@"+ eo.getId()));
                            });
                    }
                });
            });
        }
                
        return grantedAuthorities;
    }
    
    private Optional<CNRUser> getUser(OidcKeycloakAccount account, Optional<UserInfoDto> userInfo) {

        KeycloakPrincipal<?> kPrincipal = (KeycloakPrincipal<?>) account.getPrincipal();
        IDToken token = Optional.ofNullable(kPrincipal.getKeycloakSecurityContext().getIdToken())
                                .orElse(kPrincipal.getKeycloakSecurityContext().getToken());
        CNRUser cnrUser = new CNRUser();
        cnrUser.setUsername(token.getPreferredUsername());
        cnrUser.setFirstName(token.getGivenName());
        cnrUser.setLastName(token.getFamilyName());
        cnrUser.setEmail(token.getEmail());
        if (isCNRUser(account)) {
            cnrUser.setMatricola(Optional.ofNullable(getMatricola(account))
                    .orElse(Optional.ofNullable(getUsernameCNR(account)).orElse(token.getPreferredUsername())));
        }
        
        return Optional.of(cnrUser);
    }
    
    private Optional<UserInfoDto> getUserInfo() {
//        try {
            final ResponseEntity<KeycloakUserInfo> forEntity =
                    keycloakRestTemplate.getForEntity(properties.getUserinfo_endpoint(), KeycloakUserInfo.class);
            return Optional.ofNullable(forEntity.getBody())
                    .map(KeycloakUserInfo::getUserInfo);
//        } catch (Exception e) {
//            Log.error("Error getting UserInfo for current user", e);
//            return Optional.empty();
//        }
    }
    
}
