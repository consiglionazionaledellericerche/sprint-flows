package it.cnr.si.flows.ng.config;

//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.http.HttpStatus;
//import org.springframework.security.authentication.*;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsChecker;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
////import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
////import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;
////import org.springframework.security.web.authentication.switchuser.SwitchUserAuthorityChanger;
////import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
////import org.springframework.security.web.authentication.switchuser.SwitchUserGrantedAuthority;
//
//import javax.servlet.FilterChain;
//import javax.servlet.ServletException;
//import javax.servlet.ServletRequest;
//import javax.servlet.ServletResponse;
//import javax.servlet.http.Cookie;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;

public class OAuthCookieSwithUserFilterDELETE {
//
//    private static final String CNR_IMPERSONATE = "cnr_impersonate";
////    private String switchUserUrl = SwitchUserSecurityConfiguration.IMPERSONATE_START_URL;
////    private String exitUserUrl = SwitchUserSecurityConfiguration.IMPERSONATE_EXIT_URL;
//    private String usernameParameter = "impersonate_username";
//    private UserDetailsService userDetailsService;
//    private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
//    private String switchAuthorityRole = ROLE_PREVIOUS_ADMINISTRATOR;
//    private ApplicationEventPublisher eventPublisher;
//    private SwitchUserAuthorityChanger switchUserAuthorityChanger;
//    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
//
//    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
//            throws IOException, ServletException {
//        HttpServletRequest request = (HttpServletRequest) req;
//        HttpServletResponse response = (HttpServletResponse) res;
//
//        // if you're just logging in or , clear any previous cookie
//        if (loggingIn(request)) // || loggingOut(); ?
//            clearImpersonationCookie(response);
//
//        // if exit impersonation, just return an empty OK
//        if (requiresExitUser(request)) {
//            clearImpersonationCookie(response);
//            response.setStatus(HttpServletResponse.SC_OK);
//            return;
//
//        } else {
//
//            // User has requested to impersonate
//            if (false) {
//                // forbid any requests from non-admins
//                if(!(request.isUserInRole("ADMIN") || request.isUserInRole("amministratori-supporto-tecnico@0000"))) {
//                    clearImpersonationCookie(response);
//                    response.sendError(HttpStatus.FORBIDDEN.value());
//                    return;
//                }
//
//                Authentication targetUser = attemptSwitchUser(request);
//                Cookie cookie = new Cookie(CNR_IMPERSONATE, targetUser.getName());
//                cookie.setPath("/");
//                response.addCookie(cookie);
//
//                // update the current context to the new target user
//                SecurityContextHolder.getContext().setAuthentication(targetUser);
//                response.setStatus(HttpServletResponse.SC_OK);
//                return;
//            }
//
//            // any other normal request; check if the principal needs to be switched
//            else if (requiresSwitchUser(request)) {
//                // forbid any requests from non-admins
//                if(!(request.isUserInRole("ADMIN") || request.isUserInRole("amministratori-supporto-tecnico@0000"))) {
//                    clearImpersonationCookie(response);
//                    response.sendError(HttpStatus.FORBIDDEN.value());
//                    return;
//                }
//
//                // if set, attempt switch and store original
//                try {
//                    Authentication targetUser = attemptSwitchUser(request);
//                    // update the current context to the new target user
//                    SecurityContextHolder.getContext().setAuthentication(targetUser);
//                }
//                catch (AuthenticationException e) {
//                    this.logger.debug("Switch User failed", e);
//                }
//            }
//            chain.doFilter(request, response);
//        }
//    }
//
//    private void clearImpersonationCookie(HttpServletResponse response) {
//        Cookie cookie = new Cookie(CNR_IMPERSONATE, "");
//        cookie.setPath("/");
//        cookie.setMaxAge(0);
//        response.addCookie(cookie);
//    }
//
//    protected boolean requiresExitUser(HttpServletRequest request) {
//        String uri = stripUri(request);
//
////        return uri.endsWith(request.getContextPath() + this.exitUserUrl) ||
////                uri.endsWith(request.getContextPath() + "logout") ;
//      return uri.endsWith(request.getContextPath() + "logout") ;
//    }
//
//    protected boolean loggingIn(HttpServletRequest request) {
//        String uri = stripUri(request);
//        return uri.endsWith(request.getContextPath() + "oauth/token");
//
//    }
//
//    /**
//     * Attempt to switch to another user. If the user does not exist or is not active,
//     * return null.
//     *
//     * @return The new <code>Authentication</code> request if successfully switched to
//     * another user, <code>null</code> otherwise.
//     *
//     * @throws UsernameNotFoundException If the target user is not found.
//     * @throws LockedException if the account is locked.
//     * @throws DisabledException If the target user is disabled.
//     * @throws AccountExpiredException If the target user account is expired.
//     * @throws CredentialsExpiredException If the target user credentials are expired.
//     */
//    protected Authentication attemptSwitchUser(HttpServletRequest request)
//            throws AuthenticationException {
//        UsernamePasswordAuthenticationToken targetUserRequest;
//
//        String username = request.getParameter(this.usernameParameter);
//
//        if (username == null) {
//            username = getImpersonationCookieValue(request);
//        }
//
//        if (username == null) {
//            username = "";
//        }
//
//
//        if (this.logger.isDebugEnabled()) {
//            this.logger.trace("Attempt to switch to user [" + username + "]");
//        }
//
//        UserDetails targetUser;
//        targetUser = this.userDetailsService.loadUserByUsername(username);
//
//        this.userDetailsChecker.check(targetUser);
//
//        // OK, create the switch user token
//        targetUserRequest = createSwitchUserToken(request, targetUser);
//
//        if (this.logger.isDebugEnabled()) {
//            this.logger.trace("Switch User Token [" + targetUserRequest + "]");
//        }
//
//        // publish event
//        if (this.eventPublisher != null) {
//            this.eventPublisher.publishEvent(new AuthenticationSwitchUserEvent(
//                    SecurityContextHolder.getContext().getAuthentication(), targetUser));
//        }
//
//        return targetUserRequest;
//    }
//
//
//    /**
//     * Create a switch user token that contains an additional <tt>GrantedAuthority</tt>
//     * that contains the original <code>Authentication</code> object.
//     *
//     * @param request The http servlet request.
//     * @param targetUser The target user
//     *
//     * @return The authentication token
//     *
//     * @see SwitchUserGrantedAuthority
//     */
//    private UsernamePasswordAuthenticationToken createSwitchUserToken(
//            HttpServletRequest request, UserDetails targetUser) {
//
//        UsernamePasswordAuthenticationToken targetUserRequest;
//
//        // grant an additional authority that contains the original Authentication object
//        // which will be used to 'exit' from the current switched user.
//
//        Authentication currentAuth;
//
//        try {
//            // SEC-1763. Check first if we are already switched.
//            currentAuth = attemptExitUser(request);
//        }
//        catch (AuthenticationCredentialsNotFoundException e) {
//            currentAuth = SecurityContextHolder.getContext().getAuthentication();
//        }
//
//        GrantedAuthority switchAuthority = new SwitchUserGrantedAuthority(
//                this.switchAuthorityRole, currentAuth);
//
//        // get the original authorities
//        Collection<? extends GrantedAuthority> orig = targetUser.getAuthorities();
//
//        // Allow subclasses to change the authorities to be granted
//        if (this.switchUserAuthorityChanger != null) {
//            orig = this.switchUserAuthorityChanger.modifyGrantedAuthorities(targetUser,
//                    currentAuth, orig);
//        }
//
//        // add the new switch user authority
//        List<GrantedAuthority> newAuths = new ArrayList<GrantedAuthority>(orig);
//        newAuths.add(switchAuthority);
//
//        // create the new authentication token
//        targetUserRequest = new UsernamePasswordAuthenticationToken(targetUser,
//                targetUser.getPassword(), newAuths);
//
//        // set details
//        targetUserRequest
//                .setDetails(this.authenticationDetailsSource.buildDetails(request));
//
//        return targetUserRequest;
//    }
////    protected boolean requiresSetCookie(HttpServletRequest request) {
////        String uri = stripUri(request);
////
////        return uri.endsWith(request.getContextPath() + this.switchUserUrl);
////    }
//
//
//    protected boolean requiresSwitchUser(HttpServletRequest request) {
//        if (request.getUserPrincipal() == null)
//            return false;
//
//        String uri = request.getRequestURI();
//        if (uri.contains("oauth/token") || uri.endsWith("logout"))
//            return false;
//
//        return getImpersonationCookieValue(request) != null;
//    }
//
//    private String getImpersonationCookieValue(HttpServletRequest request) {
//        Cookie[] cookies = request.getCookies();
//        if (cookies != null) {
//            for (Cookie cookie : cookies ) {
//                if (cookie.getName().equals(CNR_IMPERSONATE)) {
//                    return cookie.getValue();
//                }
//            }
//        }
//        return null;
//    }
//
//
//    private String stripUri(HttpServletRequest request) {
//        String uri = request.getRequestURI();
//        int idx = uri.indexOf(';');
//
//        if (idx > 0) {
//            uri = uri.substring(0, idx);
//        }
//
//        return uri;
//    }
//
//    /**
//     * Sets the authentication data access object.
//     *
//     * @param userDetailsService The <tt>UserDetailService</tt> which will be used to load
//     * information for the user that is being switched to.
//     */
//    public void setUserDetailsService(UserDetailsService userDetailsService) {
//        this.userDetailsService = userDetailsService;
//        super.setUserDetailsService(userDetailsService);
//    }
}
