package it.cnr.si.web.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class DeprecatedOauthResource {

    @Deprecated
    @PostMapping("oauth/token")
    public ResponseEntity<Map<String, Object>> autorizeoauth() {
        return ResponseEntity.ok().build();
    }
    
    
}
