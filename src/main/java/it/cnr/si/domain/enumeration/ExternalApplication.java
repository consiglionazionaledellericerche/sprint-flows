package it.cnr.si.domain.enumeration;

import org.springframework.web.client.RestTemplate;


/**
 * The ExternalApplication enumeration.
 */
public enum ExternalApplication {
    ABIL,
    STM,
    SIGLA,
    LABCON,
    SIPER,
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
