package it.cnr.si.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/")
public class DeprecatedOauthResource {

    @Value("${cnr.ssoClientId}")
    private String defaultClientId;
    @Value("${cnr.ssoClientSecret}")
    private String defaultClientSecret;
    @Value("${keycloak.auth-server-url}")
    private String ssoUrl;
    @Value("${keycloak.realm}")
    private String ssoRealm;
    
    @Deprecated
    @PostMapping(
            path = "oauth/token",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
        public ResponseEntity<String> autorizeoauth(
                @RequestHeader HttpHeaders inHeaders,
                @RequestParam String grant_type,
                @RequestParam(required = false) String client_id,
                @RequestParam(required = false) String client_secret,
                @RequestParam(required = false) String username,
                @RequestParam(required = false) String password) throws URISyntaxException {
            
        RestTemplate rt = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String toEncode = defaultClientId+":"+defaultClientSecret;
        String basicAuth = Base64.getEncoder().encodeToString(toEncode.getBytes());
        headers.set("authorization", "Basic "+basicAuth);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", username);
        params.add("password", password);
        params.add("grant_type", "password");
        
        URI url = new URI(ssoUrl+"/realms/"+ ssoRealm +"/protocol/openid-connect/token");
        RequestEntity<MultiValueMap<String, String>> request = 
                new RequestEntity<MultiValueMap<String, String>>(params, headers, HttpMethod.POST, url);
        
        ResponseEntity<String> result = rt.exchange(request, String.class);
        
        return ResponseEntity.ok(result.getBody());
    }
    
    
}
