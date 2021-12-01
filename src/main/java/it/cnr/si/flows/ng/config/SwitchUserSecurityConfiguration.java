package it.cnr.si.flows.ng.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsManager;
import org.springframework.security.ldap.userdetails.LdapUserDetailsService;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;

import it.cnr.si.flows.ng.ldap.FlowsAuthoritiesPopulator;


@Configuration
@Order(202)
public class SwitchUserSecurityConfiguration extends WebSecurityConfigurerAdapter {

    public static final String IMPERSONATE_EXIT_URL = "/impersonate/exit";
    public static final String IMPERSONATE_START_URL = "/impersonate/start";

    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                //url per vedere con che profilo è avviata l'applicazione(serve per caricare la giusta immagine della home)
                .antMatcher("/flows/api/profile-info**")
                .antMatcher("/flows/api/avvisiattivi**")
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and()
                .antMatcher("/impersonate/**")
                .authorizeRequests()
                .antMatchers(IMPERSONATE_START_URL).hasAnyRole("ROLE_ADMIN", "ROLE_amministratori-supporto-tecnico@0000")
                .antMatchers(IMPERSONATE_EXIT_URL).hasRole("PREVIOUS_ADMINISTRATOR")
                .and()
                .addFilterAfter(switchUserFilter(), FilterSecurityInterceptor.class)
                .addFilterAfter(logPrologSettingFilter(), OAuthCookieSwithUserFilter.class);
    }
    
    @Profile(value = {"cnr"})
    @Bean public LdapUserDetailsManager getLdapUserDetailsManager(LdapContextSource ctx) {
        return new LdapUserDetailsManager(ctx);
    }

    @Profile(value = {"cnr"})
    @Bean public LdapUserDetailsService getLdapUserDetailsService(LdapUserSearch search, LdapAuthoritiesPopulator fap) {
        return new LdapUserDetailsService(search, fap);
    }

    @Profile(value = {"cnr"})
    @Bean public LdapAuthoritiesPopulator getFlowsAuthoritiesPopulator(ApplicationContext appContext) {
        FlowsAuthoritiesPopulator cap = new FlowsAuthoritiesPopulator();
        appContext.getAutowireCapableBeanFactory().autowireBean(cap);
        return cap;
    }

    @Bean
    @Profile(value = {"cnr"})
    public LdapUserSearch getLdapUserSearch(Environment env, LdapContextSource ctx) {
        String userSearchBase = ""; //p.getProperty("userSearchBase");
        String userSearchFilter = env.getProperty("spring.ldap.userSearchFilter");

        return new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, ctx);
    }

    @Bean
    @Profile(value = {"cnr"})
    public LdapContextSource getLdapContextSource(Environment env) {

        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(env.getProperty("spring.ldap.url"));
        contextSource.setBase(env.getProperty("spring.ldap.userSearchBase"));
        contextSource.setUserDn(env.getProperty("spring.ldap.managerDn"));
        contextSource.setPassword(env.getProperty("spring.ldap.managerPassword"));
        return contextSource;
    }

    @Bean
    @Profile(value = {"cnr"})
    public LdapTemplate getLdapTemplate(LdapContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }

    @Bean
    public SwitchUserFilter switchUserFilter() {
        OAuthCookieSwithUserFilter filter = new OAuthCookieSwithUserFilter();
        filter.setUserDetailsService(userDetailsService);
        filter.setSwitchUserUrl(IMPERSONATE_START_URL);
        filter.setExitUserUrl(IMPERSONATE_EXIT_URL);
        filter.setTargetUrl("/");
        return filter;
    }

    @Bean
    public LogPrologSettingFilter logPrologSettingFilter() {
        return new LogPrologSettingFilter();
    }
}
