package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.ldap.LdapPersonToSearchResultMapper;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.base.PageDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import org.activiti.engine.ManagementService;
import org.activiti.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.*;
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

        Map<String, String> query = new HashMap<String, String>() {{put("term", username);}};
        PageDto<PersonaWebDto> persone = aceService.getPersone(query);

        response.put("more", persone.getCount() > 10);
        response.put("results", persone.getItems().stream()
                .limit(10)
                .map(p -> new Utils.SearchResult(p.getUsername(), p.getNome() +" "+ p.getCognome()))
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

    //    todo: non vieme mai usato => cancellare?
    // todo: se non si e' rotto niente, cancellare entro 30/10/18 - martin
//    @RequestMapping(value = "/customquery", method = RequestMethod.GET)
//    @Secured(AuthoritiesConstants.ADMIN)
//    public List<HistoricProcessInstanceResponse> customQuery() {
//
//        FlowsHistoricProcessInstanceQuery query = new FlowsHistoricProcessInstanceQuery(managementService);
//        List<String> groups = new ArrayList<>();
//        groups.add("sfd@2216");
//        query.setVisibleToGroups(groups);
//
//        List<HistoricProcessInstance> processes = managementService.executeCommand(new Command<List<HistoricProcessInstance>>() {
//
//            @SuppressWarnings("unchecked")
//            @Override
//            public List<HistoricProcessInstance> execute(CommandContext commandContext) {
//                return (List<HistoricProcessInstance>) commandContext.getDbSqlSession().selectList("selectFlowsHistoricProcessInstancesWithVariablesByQueryCriteria", query);
//            }
//        });
//
//        return restResponseFactory.createHistoricProcessInstanceResponseList(processes);
//    }


}
