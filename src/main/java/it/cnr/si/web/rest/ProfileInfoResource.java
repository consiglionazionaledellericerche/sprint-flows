package it.cnr.si.web.rest;

import it.cnr.si.config.JHipsterProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.*;

@RestController
@RequestMapping("/flows/api")
public class ProfileInfoResource {

    @Inject
    Environment env;

    @Inject
    private JHipsterProperties jHipsterProperties;

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
        }

        List<String> ribbonProfiles = new ArrayList<>(Arrays.asList(displayOnActiveProfiles));
        List<String> springBootProfiles = Arrays.asList(activeProfiles);
        ribbonProfiles.retainAll(springBootProfiles);

        if (!ribbonProfiles.isEmpty()) {
            response.put("ribbonEnv", ribbonProfiles.get(0));
        }

        response.put("activeProfiles", activeProfiles);

        return response;
    }
}