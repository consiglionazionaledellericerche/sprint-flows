package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.FlowsUser;
import it.cnr.si.flows.ng.repository.FlowsHistoricProcessInstanceQuery;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.FlowsUserService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("api/users")
public class FlowsUserResourceAce {

    @Inject
    private Environment env;

    @Autowired
    private LdapTemplate ldapTemplate;

    @Inject
    private AceBridgeService aceService;

    @Inject
    private ManagementService managementService;

    @Inject
    private RestResponseFactory restResponseFactory;

    @Inject
    private FlowsUserService flowsUserService;

    @RequestMapping(value = "/ace/user/{username:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getAce(@PathVariable String username) {
        return aceService.getAceGroupsForUser(username);
    }

    @RequestMapping(value = "/ace/group/{groupname:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getAceGroup(@PathVariable String groupname) {
        return aceService.getUsersinAceGroup(groupname);
    }

    @RequestMapping(value = "/ace/groupdetail/{id}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public String getAceGroupDetail(@PathVariable Integer id) {
        return aceService.getNomeStruturaById(id);
    }


    @RequestMapping(value = "/{username:.+}/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> searchUsers(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

        List<SearchResult> search = new ArrayList();

        if (activeProfiles.contains("cnr")) {
            //con il profilo "CNR" faccio la ricerca per l'autocompletamento degli utenti su ldap
            search = ldapTemplate.search("", "(uid=*" + username + "*)", new AttributesMapper<SearchResult>() {
                public SearchResult mapFromAttributes(Attributes attrs) throws NamingException {
                    return new SearchResult(attrs.get("uid").get().toString(),
                                            attrs.get("cnrnome").get() + " " + attrs.get("cnrcognome").get() + " " +
                                                    "(" + attrs.get("uid").get().toString() + ")");
                }
            });

        } else if (activeProfiles.contains("oiv")) {
            //con il profilo "OIV" faccio la ricerca per l'autocompletamento degli utenti sul DB
            List<FlowsUser> result = flowsUserService.searchByLogin(username);
            search = result.stream()
                    .map(user -> new SearchResult(user.getLogin(), user.getLogin()))
                    .collect(Collectors.toList());
        }

        response.put("more", search.size() > 10);
        response.put("results", search.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/struttura/{struttura:.+}/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getUoLike(@PathVariable String struttura) {

        Map<String, Object> response = new HashMap<>();

        List<Pair<Integer, String>> results = aceService.getUoLike(struttura);
        List<SearchResult> collect = results.stream().map(p -> new SearchResult(p.getLeft().toString(), p.getRight())).collect(Collectors.toList());

        response.put("more", collect.size() > 10);
        response.put("results", collect.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    //    todo: non vieme mai usato => cancellare?
    @RequestMapping(value = "/customquery", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<HistoricProcessInstanceResponse> customQuery() {

        FlowsHistoricProcessInstanceQuery query = new FlowsHistoricProcessInstanceQuery(managementService);
        List<String> groups = new ArrayList<>();
        groups.add("sfd@2216");
        query.setVisibleToGroups(groups);

        List<HistoricProcessInstance> processes = managementService.executeCommand(new Command<List<HistoricProcessInstance>>() {

            @SuppressWarnings("unchecked")
            @Override
            public List<HistoricProcessInstance> execute(CommandContext commandContext) {
                return (List<HistoricProcessInstance>) commandContext.getDbSqlSession().selectList("selectFlowsHistoricProcessInstancesWithVariablesByQueryCriteria", query);
            }
        });

        return restResponseFactory.createHistoricProcessInstanceResponseList(processes);
    }


    public class SearchResult {
        public String value;
        public String label;

        public SearchResult(String v, String l) {
            this.value = v;
            this.label = l;
        }
    }
}
