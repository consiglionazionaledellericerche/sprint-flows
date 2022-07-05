package it.cnr.si.web.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.config.JHipsterProperties;
import it.cnr.si.domain.Avviso;
import it.cnr.si.domain.CNRUser;
import it.cnr.si.model.UserInfoDto;
import it.cnr.si.repository.AvvisoRepository;
import it.cnr.si.service.SecurityService;
import it.cnr.si.service.UserService;

@RestController
@RequestMapping("/flows/api")
public class ProfileInfoResource {

    @Inject
    Environment env;
    @Inject
    private AvvisoRepository avvisoRepository;
    @Inject
    private JHipsterProperties jHipsterProperties;
    @Inject
    private SecurityService securityService;
    
    //questa cache non viene "evictata" perchè il profilo può cambiare solo se si riavvia l'applicazione
//    @Cacheable(value = "profile-info")
    @RequestMapping(value = "/profile-info",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public Map getActiveProfiles() {

        String[] activeProfiles = env.getActiveProfiles();
        String[] displayOnActiveProfiles = jHipsterProperties.getRibbon().getDisplayOnActiveProfiles();
        Map response = new HashMap<>();

        if (displayOnActiveProfiles == null) {
            response.put("ribbonEnv", null);
        } else {

            List<String> ribbonProfiles = new ArrayList<>(Arrays.asList(displayOnActiveProfiles));
            List<String> springBootProfiles = Arrays.asList(activeProfiles);
            ribbonProfiles.retainAll(springBootProfiles);
    
            if (!ribbonProfiles.isEmpty()) {
                response.put("ribbonEnv", ribbonProfiles.get(0));
            }
        }

        response.put("activeProfiles", activeProfiles);

        return response;
    }
    
    @RequestMapping(value = "/current-account",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Optional<CNRUser>> getAccount() {
        
        return ResponseEntity.ok(securityService.getUser());
        
    }

    @RequestMapping(value = "/current-account2",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Optional<UserInfoDto>> getAccount2() {
        
        return ResponseEntity.ok(securityService.getUserInfo());
        
    }
    
    @GetMapping("/avvisiattivi")
    @Timed
    public ResponseEntity<List<Avviso>> getAvvisiAttivi() {

        return ResponseEntity.ok(avvisoRepository.findByAttivoTrueOrderByIdDesc());

    }
}