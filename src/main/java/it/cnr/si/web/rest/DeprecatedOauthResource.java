package it.cnr.si.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

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
    
    @Deprecated
    @PostMapping(
            path = "oauth/token",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> autorizeoauth(
            @RequestHeader HttpHeaders inHeaders) throws URISyntaxException {
        
        RestTemplate rt = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        String client_id = null, client_secret = null;
        
        List<String> auths = inHeaders.get("Authorization");
        if (auths.size() > 0) {
            String auth = auths.get(0);
            if (auth.startsWith("Basic ")) {
                auth = new String(Base64.getDecoder().decode(auth.substring(6)));
                String[] creds = auth.split(":");
                client_id = creds[0];
                client_secret = creds[1];
            }
        }
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", client_id);
        params.add("client_secret", client_secret);
        params.add("grant_type", "client_credentials");
        
        URI url = new URI(ssoUrl+"/realms/cnr/protocol/openid-connect/token");
        RequestEntity<MultiValueMap<String, String>> request = 
                new RequestEntity<MultiValueMap<String, String>>(params, headers, HttpMethod.POST, url);
        
        ResponseEntity<String> result = rt.exchange(request, String.class);
        
        return ResponseEntity.ok(result.getBody());
    }
    
    
}
