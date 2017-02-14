package it.cnr.si.flows.ng.config;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

import it.cnr.si.config.ldap.LdapUserMapper;


@Configuration
@Order(202)
public class SwitchUserSecurityConfiguration extends WebSecurityConfigurerAdapter {


    @Autowired
    private UserDetailsService userDetailsService;


    @Bean
    public SwitchUserFilter switchUserFilter(
            ) {
        OAuthCookieSwithUserFilter filter = new OAuthCookieSwithUserFilter();
        filter.setUserDetailsService(userDetailsService);
//        filter.setLdapUserDetailsService(ldapUserDetailsService);
        filter.setSwitchUserUrl("/login/impersonate");
        filter.setExitUserUrl("/logout/impersonate");
        filter.setTargetUrl("/");
        return filter;
    }

//    @Bean
//    public LdapUserDetailsService getLdapUserDetailsService(
//            LdapUserSearch ldapUserSearch,
//            LdapAuthoritiesPopulator ldapAuthoritiesPopulator,
//            LdapUserMapper userDetailsMapper) {
//
//        LdapUserDetailsService service = new LdapUserDetailsService(ldapUserSearch, ldapAuthoritiesPopulator);
//        service.setUserDetailsMapper(userDetailsMapper);
//
//        return service;
//    }

//    @Bean
//    public FilterBasedLdapUserSearch getFilterBasedLdapUserSearch(Environment env,
//            UserDetailsContextMapper userDetailsContextMapper,
//            BaseLdapPathContextSource contextSource) {
//
//        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "spring.ldap.");
//
//        FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch(
//                propertyResolver.getProperty("userSearchBase"),
//                propertyResolver.getProperty("userSearchBase"),
//                contextSource);
//        return search;
//    }
//
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
        .authorizeRequests()
        .antMatchers("/login/impersonate*").hasRole("ADMIN")
        .antMatchers("/logout/impersonate*").hasRole("PREVIOUS_ADMINISTRATOR")
        .and()
        .addFilterAfter(switchUserFilter(), FilterSecurityInterceptor.class);
    };


}
