package it.cnr.si.flows.ng.utils;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

public class SecurityUtils {

    private SecurityUtils() {}

    public static boolean isCurrentUserInRole(String authority) {
        return it.cnr.si.security.SecurityUtils.isCurrentUserInRole(authority);
    }

    public static String getCurrentUserLogin() {
        return it.cnr.si.security.SecurityUtils.getCurrentUserLogin();
    }

    public static boolean isAuthenticated() {
        return it.cnr.si.security.SecurityUtils.isAuthenticated();
    }

    public static List<String> getCurrentUserAuthorities() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .collect(Collectors.toList());
    }

}
