package it.cnr.si.flows.ng.resource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.activiti.engine.ManagementService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.aop.FlowsHistoricProcessInstanceQuery;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.security.AuthoritiesConstants;

@RestController
@RequestMapping("api/users")
public class FlowsUserResource {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Inject
    private AceBridgeService aceService;

    @Inject
    private ManagementService managementService;

    @Inject
    private RestResponseFactory restResponseFactory;

    @RequestMapping(value= "/ace/user/{username:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getAce(@PathVariable String username) throws SQLException {
        return aceService.getAceGroupsForUser(username);
    }

    @RequestMapping(value= "/ace/group/{groupname:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getAceGroup(@PathVariable String groupname) throws SQLException {
        return aceService.getUsersinAceGroup(groupname);
    }

    @RequestMapping(value= "/ace/groupdetail/{id}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public String getAceGroupDetail(@PathVariable Integer id) throws SQLException {
        return aceService.getNomeStruturaById(id);
    }


    @RequestMapping(value = "/{username:.+}/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getMyTasks(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        // TODO se cercare con due wildcard e' un overkill (ma non credo visto che sono un diecimila voci)
        // TODO modificare la serach con un parametro piu' scaltro
        List<Object> search = ldapTemplate.search("", "(uid=*"+ username +"*)", new AttributesMapper<Object>() {
            public Object mapFromAttributes(Attributes attrs) throws NamingException {
                return attrs.get("uid").get();
            }
        });

        response.put("more", search.size() > 10);
        response.put("results", search.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value= "/customquery", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<HistoricProcessInstanceResponse> customQuery() throws SQLException {

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

}
