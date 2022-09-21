package it.cnr.si.web.rest;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
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
    
    @Deprecated
    @PostMapping(
            path = "oauth/token",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> autorizeoauth(
            @RequestParam String grant_type,
            @RequestParam(required = false) String client_id,
            @RequestParam(required = false) String client_secret,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password) throws URISyntaxException {
        
        RestTemplate rt = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", client_id != null? client_id : defaultClientId);
        params.add("client_secret", client_secret != null ? client_secret : defaultClientSecret);
        params.add("grant_type", grant_type);
        params.add("username", username);
        params.add("password", password);
        
        RequestEntity<MultiValueMap<String, String>> request = new RequestEntity<MultiValueMap<String, String>>(
                params, headers, HttpMethod.POST, new URI("http://dockerwebtest02.si.cnr.it:8110/auth/realms/cnr/protocol/openid-connect/token"));
        
        ResponseEntity<String> result = rt.exchange(request, String.class);
        
        return ResponseEntity.ok(result.getBody());
    }
    
    
}
