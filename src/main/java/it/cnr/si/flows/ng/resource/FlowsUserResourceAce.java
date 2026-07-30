package it.cnr.si.flows.ng.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.ManagementService;
import org.activiti.rest.service.api.RestResponseFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.flows.ng.utils.Utils.SearchResult;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.enums.TipiEntitaOrganizzativa;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;


@RestController
@RequestMapping("api/users")
@Profile("cnr")
public class FlowsUserResourceAce {

    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private AceService aceService;
    @Inject
    private ManagementService managementService;
    @Inject
    private RestResponseFactory restResponseFactory;

    @GetMapping(value = "/{username:.+}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> searchUsers(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        Map<String, String> query = new HashMap<String, String>() {{put("username", username);}};
        List<SimpleUtenteWebDto> utenti = aceService.searchUtenti(query);

        response.put("more", utenti.size() > 10);
        response.put("results", utenti.stream()
                .limit(10)
                .map(u ->  {
                    SimplePersonaWebDto p = u.getPersona();
                    String label = p != null ? p.getNome() +" "+ p.getCognome() : u.getUsername();
                    return new Utils.SearchResult(
                            u.getUsername(),
                            label
                            );
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/struttura/{struttura:.+}/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getUoLike(@PathVariable String struttura) {

        Map<String, Object> response = new HashMap<>();

        List<Utils.SearchResult> collect = aceBridgeService.getUoLike(struttura)
                .stream()
                .map(p -> new Utils.SearchResult(p.getCdsuo(), p.getCdsuo() +" - "+ p.getDenominazione()))
                .distinct()
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

        List<Utils.SearchResult> collect = aceBridgeService.getUoByTipo(TipiEntitaOrganizzativa.DIPARTIMENTO)
                .stream()
                .map(p -> new Utils.SearchResult(p.getId().toString(), p.getCdsuo() +" - "+ p.getDenominazione()))
                .collect(Collectors.toList());

        response.put("more", collect.size() > 10);
        response.put("results", collect.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/istituti/{searchterm:.*}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getIstituto(@PathVariable String searchterm) throws InterruptedException, ExecutionException {

        Map<String, Object> response = new HashMap<>();

        List<SimpleEntitaOrganizzativaWebDto> istituti = aceBridgeService.cercaIstituto(searchterm.trim());
        List<SearchResult> collect = new ForkJoinPool().submit(
                () -> istituti
                    .parallelStream()
                    .limit(10)
                    .map(i -> 
                    new Utils.SearchResult(
                            String.valueOf(i.getId()),
                            i.getDenominazione() + (aceService.getUtentiInRuoloEo("responsabile-struttura", i.getId()).size() > 0 ?
                                    " ("+ aceService.getUtentiInRuoloEo("responsabile-struttura", i.getId()).get(0).getUsername()+")" :
                                    "")
                            ) 
                            ).collect(Collectors.toList())
                ).get();

        response.put("more", collect.size() > 10);
        response.put("results", collect);

        return ResponseEntity.ok(response);
    }

}
