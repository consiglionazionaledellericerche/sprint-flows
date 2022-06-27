package it.cnr.si.flows.ng.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Service
@Profile("cnr")
public class FlowsSiperService {

    private RestTemplate siperRestTemplate;
    private String url;
    private String pathResponsabile = "/siper/json/sedi?titCa={cdsuo}&userinfo=true&ruolo=resp";
    private String pathDirettore = "/siper/json/sedi?titCa={cdsuo}&userinfo=true&ruolo=dircds";
    private String pathResponsabileIdnsip = "/siper/json/sedi?sedeId={idnsip}&userinfo=true&ruolo=resp";
    private String pathDirettoreIdnsip = "/siper/json/sedi?sedeId={idnsip}&userinfo=true&ruolo=dircds";
    private String pathCDSUOAfferenzaUtente = "/siper/json/userinfo/{userName}";
    
    @Inject
    private Environment env;

    @PostConstruct
    public void init() {

        this.url = env.getProperty("cnr.siper.url");

        this.siperRestTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = this.siperRestTemplate.getInterceptors();

        String username = env.getProperty("cnr.siper.username");
        String password = env.getProperty("cnr.siper.password");
        interceptors.add(new BasicAuthenticationInterceptor(username, password));

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

    
    public List<Map<String, Object>> getResponsabileIDNSIP(String idnsip) {
        //request
        ResponseEntity responseString = siperRestTemplate.exchange(this.url + pathResponsabileIdnsip, HttpMethod.GET,
                                                                   new HttpEntity<>("body"), String.class, idnsip);
        // mapping della response in una List<Map<String, Object>>
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        BufferedReader reader = new BufferedReader(new StringReader(((String)responseString.getBody())));
        List<Map<String, Object>> response = gson.fromJson(reader, type);

        return response;
    }

    public List<Map<String, Object>> getDirettoreIDNSIP(String idnsip) {

        //request
        ResponseEntity responseString = siperRestTemplate.exchange(this.url + pathDirettoreIdnsip, HttpMethod.GET,
                                                                   new HttpEntity<>("body"), String.class, idnsip);
        // mapping della response in una List<Map<String, Object>>
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        BufferedReader reader = new BufferedReader(new StringReader(((String)responseString.getBody())));
        List<Map<String, Object>> response = gson.fromJson(reader, type);

        return response;
    }

    public Map<String, Object> getCDSUOAfferenzaUtente(String userName) {

        //request
        ResponseEntity responseString = siperRestTemplate.exchange(this.url + pathCDSUOAfferenzaUtente, HttpMethod.GET,
                                                                   new HttpEntity<>("body"), String.class, userName);
        // mapping della response in una List<Map<String, Object>>
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        BufferedReader reader = new BufferedReader(new StringReader(((String)responseString.getBody())));
        Map<String, Object> response = gson.fromJson(reader, type);

        return response;
    }
}