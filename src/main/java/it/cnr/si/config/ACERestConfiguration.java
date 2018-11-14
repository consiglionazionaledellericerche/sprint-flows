package it.cnr.si.config;


import feign.Feign;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import it.cnr.si.flows.ng.service.AceAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;

import javax.inject.Inject;
import java.util.List;

//@Profile("cnr")
//@Configuration
//@EnableOAuth2Client
public class ACERestConfiguration {

    @Inject
    private OAuth2ClientContext oauth2Context;

    @Value("${spring.ace.url}")
    private String aceUrl;
    @Value("${spring.ace.password}")
    private String acePassword;
    @Value("${spring.ace.username}")
    private String aceUsername;

    @Bean(name ="aceRestTemplate")
    public OAuth2RestTemplate aceRestTemplate() {

        final AceAuthService service = Feign.builder()
                .decoder(new GsonDecoder())
                .encoder(new FormEncoder(new GsonEncoder()))
                .target(AceAuthService.class, aceUrl + "api");

         service.getToken(aceUsername, acePassword);

        ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
        details.setPassword(acePassword);
        details.setUsername(aceUsername);
        details.setAccessTokenUri(aceUrl);
        details.setTokenName("ace_token");

        OAuth2RestTemplate rt = new OAuth2RestTemplate(details);
        return rt;
    }
}
