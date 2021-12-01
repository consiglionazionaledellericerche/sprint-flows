package it.cnr.si.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@Profile(value = "oiv")
@Configuration
public class OIVRestConfiguration {
    private final Logger log = LoggerFactory.getLogger(OIVRestConfiguration.class);

    @Inject
    private Environment env;

    @Bean(name = "oivRestTemplate")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(env.getProperty("oiv.usr"), env.getProperty("oiv.psw")));
        return restTemplate;
    }
}
