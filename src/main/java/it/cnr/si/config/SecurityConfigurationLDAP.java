package it.cnr.si.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import javax.inject.Inject;
import java.util.Arrays;

/**
 * Created by francesco on 30/03/15.
 */

@Configuration
@Profile("cnr")
@EnableWebSecurity
@Order(200)
public class SecurityConfigurationLDAP extends WebSecurityConfigurerAdapter {

    private final Logger log = LoggerFactory.getLogger(SecurityConfigurationLDAP.class);

    private RelaxedPropertyResolver propertyResolver;

    @Inject
    private Environment env;

    @Inject
    private UserDetailsContextMapper userDetailsContextMapper;
    @Autowired(required = false)
    private LdapAuthoritiesPopulator authPopulator;

    @Inject
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (Arrays.asList(env.getActiveProfiles()).contains("cnr")) {
            log.info("security LDAP configure global");
            this.propertyResolver = new RelaxedPropertyResolver(env, "spring.ldap.");
            String url = propertyResolver.getProperty("url");
            if (propertyResolver != null && url != null) {
                log.info("ldap server: " + url);

                auth.ldapAuthentication()
                        .userDetailsContextMapper(userDetailsContextMapper)
                        .ldapAuthoritiesPopulator(authPopulator)
                        .userSearchBase(propertyResolver.getProperty("userSearchBase"))
                        .userSearchFilter(propertyResolver.getProperty("userSearchFilter"))
                        .groupSearchBase(null)
                        .contextSource()
                        .url(url)
                        .managerDn(propertyResolver.getProperty("managerDn"))
                        .managerPassword(propertyResolver.getProperty("managerPassword"));
            } else {
                log.warn("no ldap configuration found");
            }
        } else {
            log.info("Profilo non cnr, non carico LDAP");
        }
    }

}
