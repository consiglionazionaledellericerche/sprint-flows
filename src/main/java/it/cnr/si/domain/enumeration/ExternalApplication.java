package it.cnr.si.domain.enumeration;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The ExternalApplication enumeration.
 */
public enum ExternalApplication {
    ABIL,
    STM,
    SIGLA,
    LABCON,
    ACE,
    MISSIONI,
    ATTESTATI,
    GENERIC;

    private RestTemplate template;
    public void setTemplate(RestTemplate template) {
        this.template = template;
    }
    public RestTemplate getTemplate() {
        if (this.template != null)
            return this.template;
        else
            throw new IllegalStateException("RestTemplate assente per l'aplpication "+ this.name());
    }
}
