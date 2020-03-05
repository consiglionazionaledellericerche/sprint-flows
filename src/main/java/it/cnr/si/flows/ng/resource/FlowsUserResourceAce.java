package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.base.PageDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.PersonaDto;
import it.cnr.si.service.dto.anagrafica.scritture.UtenteDto;
import org.activiti.engine.ManagementService;
import org.activiti.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("api/users")
@Profile("cnr")
public class FlowsUserResourceAce {

    @Autowired
    private LdapTemplate ldapTemplate;
    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private AceService aceService;
    @Inject
    private ManagementService managementService;
    @Inject
    private RestResponseFactory restResponseFactory;

    @RequestMapping(value = "/{username:.+}/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> searchUsers(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        Map<String, String> query = new HashMap<String, String>() {{put("username", username);}};
        PageDto<UtenteDto> utenti = aceService.searchUtenti(query);

        response.put("more", utenti.getCount() > 10);
        response.put("results", utenti.getItems().stream()
                .limit(10)
                .map(u ->  {
                    PersonaDto p = u.getPersona();
                    String label = p != null ? p.getNome() +" "+ p.getCognome() : u.getUsername();
                    return new Utils.SearchResult(
                            u.getUsername(),
                            label
                    );
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/struttura/{struttura:.+}/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getUoLike(@PathVariable String struttura) {

        Map<String, Object> response = new HashMap<>();

        List<Utils.SearchResult> collect = aceBridgeService.getUoLike(struttura)
                .stream()
                .map(p -> new Utils.SearchResult(p.getId().toString(), p.getCdsuo() +" - "+ p.getDenominazione()))
                .collect(Collectors.toList());

        response.put("more", collect.size() > 10);
        response.put("results", collect.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }


    @GetMapping(value = "/dipartimenti/{tipo:.+}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getDipartimerntiList(@PathVariable int tipo) {

        Map<String, Object> response = new HashMap<>();

        List<Utils.SearchResult> collect = aceBridgeService.getUoByTipo(tipo)
                .stream()
                .map(p -> new Utils.SearchResult(p.getId().toString(), p.getCdsuo() +" - "+ p.getDenominazione()))
                .collect(Collectors.toList());

        response.put("more", collect.size() > 10);
        response.put("results", collect.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

}
