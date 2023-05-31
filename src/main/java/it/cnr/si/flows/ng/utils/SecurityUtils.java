package it.cnr.si.flows.ng.utils;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import it.cnr.si.service.SecurityService;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

@Service
public class SecurityUtils {

    @Inject
    private SecurityService securityService;
    
    private SecurityUtils() {}


    public List<String> getCurrentUserAuthorities() {
        return securityService.getUser().get().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .collect(Collectors.toList());
    }


    // public String getRealUserLogged(){
    //     //Se tra le Authorities dell`user c`è una SwitchUserGrantedAuthority ==> lo user è "impersonate" da un utente ADMIN
    //     Optional<SwitchUserGrantedAuthority> switchUserGrantedAuthority = (Optional<SwitchUserGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
    //             .filter(userGrantedAuthority -> userGrantedAuthority instanceof SwitchUserGrantedAuthority)
    //             .findAny();

    //     // recupero l`utente "realmente" loggato
    //     return switchUserGrantedAuthority.map(userGrantedAuthority -> userGrantedAuthority.getSource().getName())
    //             .orElse(it.cnr.si.security.SecurityUtils.getCurrentUserLogin());
    // }
}
