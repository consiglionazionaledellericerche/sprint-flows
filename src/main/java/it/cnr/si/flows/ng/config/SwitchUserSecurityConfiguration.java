package it.cnr.si.flows.ng.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsManager;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

import it.cnr.si.config.ldap.CustomAuthoritiesPopulator;


@Configuration
@Order(202)
public class SwitchUserSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
        .authorizeRequests()
        .antMatchers("/login/impersonate*").hasRole("ADMIN")
        .antMatchers("/logout/impersonate*").hasRole("PREVIOUS_ADMINISTRATOR")
        .and()
        .addFilterAfter(switchUserFilter(), FilterSecurityInterceptor.class);
    };

    @Bean public LdapUserDetailsManager getLdapUserDetailsManager(LdapContextSource ctx) {
        return new LdapUserDetailsManager(ctx);
    }

    @Bean public LdapUserDetailsService getLdapUserDetailsService(LdapUserSearch search) {
        CustomAuthoritiesPopulator cap = new CustomAuthoritiesPopulator();
        return new LdapUserDetailsService(search, cap);
    }



    @Bean
    public LdapUserSearch getLdapUserSearch(Environment env, LdapContextSource ctx) {
        PropertyResolver p = new RelaxedPropertyResolver(env, "spring.ldap.");
        String userSearchBase = ""; //p.getProperty("userSearchBase");
        String userSearchFilter = p.getProperty("userSearchFilter");

        return new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, ctx);
    }

    @Bean
    public LdapContextSource getLdapContextSource(Environment env) {
        PropertyResolver p = new RelaxedPropertyResolver(env, "spring.ldap.");
        LdapContextSource contextSource= new LdapContextSource();
        contextSource.setUrl(p.getProperty("spring.ldap.url"));
        contextSource.setBase(p.getProperty("spring.ldap.userSearchBase"));
        contextSource.setUserDn(p.getProperty("spring.ldap.managerDn"));
        contextSource.setPassword(p.getProperty("spring.ldap.managerPassword"));
        return contextSource;
    }

    @Bean
    public SwitchUserFilter switchUserFilter(
            ) {
        OAuthCookieSwithUserFilter filter = new OAuthCookieSwithUserFilter();
        filter.setUserDetailsService(userDetailsService);
        filter.setSwitchUserUrl("/login/impersonate");
        filter.setExitUserUrl("/logout/impersonate");
        filter.setTargetUrl("/");
        return filter;
    }




}
