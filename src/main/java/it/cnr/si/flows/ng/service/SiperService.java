package it.cnr.si.flows.ng.service;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SiperService {

    private RestTemplate siperRestTemplate;
    private String url;
    private String pathResponsabile = "/siper/json/sedi?titCa={cdsuo}&userinfo=true&ruolo=resp";
    private String pathDirettore = "/siper/json/sedi?titCa={cdsuo}&userinfo=true&ruolo=dircds";

    @Inject
    private Environment env;

    @PostConstruct
    public void init() {

        this.url = env.getProperty("cnr.siper.url");

        this.siperRestTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = this.siperRestTemplate.getInterceptors();

        String username = env.getProperty("cnr.siper.username");
        String password = env.getProperty("cnr.siper.password");
        interceptors.add(new BasicAuthorizationInterceptor(username, password));

        this.siperRestTemplate.setInterceptors(interceptors);
    }

    public List<Map<String, Object>> getResponsabileCDSUO(String cdsuo) {

        ResponseEntity<? extends List> forEntity = siperRestTemplate.getForEntity(
                this.url + pathResponsabile,
                List.class,
                cdsuo);

        return (List<Map<String, Object>>) forEntity.getBody();
    }
    
    public List<Map<String, Object>> getDirettoreCDSUO(String cdsuo) {

        ResponseEntity<? extends List> forEntity = siperRestTemplate.getForEntity(
                this.url + pathDirettore,
                List.class,
                cdsuo);

        return (List<Map<String, Object>>) forEntity.getBody();
    }

}
