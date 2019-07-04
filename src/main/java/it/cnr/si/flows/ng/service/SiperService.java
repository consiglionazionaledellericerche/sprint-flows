package it.cnr.si.flows.ng.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Collections;
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
        //request
        ResponseEntity responseString = siperRestTemplate.exchange(this.url + pathResponsabile, HttpMethod.GET,
                                                                   new HttpEntity<>("body"), String.class, cdsuo);
        // mapping della response in una List<Map<String, Object>>
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        BufferedReader reader = new BufferedReader(new StringReader(((String)responseString.getBody())));
        List<Map<String, Object>> response = gson.fromJson(reader, type);

        return response;
    }

    public List<Map<String, Object>> getDirettoreCDSUO(String cdsuo) {

        //request
        ResponseEntity responseString = siperRestTemplate.exchange(this.url + pathDirettore, HttpMethod.GET,
                                                             new HttpEntity<>("body"), String.class, cdsuo);
        // mapping della response in una List<Map<String, Object>>
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        BufferedReader reader = new BufferedReader(new StringReader(((String)responseString.getBody())));
        List<Map<String, Object>> response = gson.fromJson(reader, type);

        return response;
    }
}