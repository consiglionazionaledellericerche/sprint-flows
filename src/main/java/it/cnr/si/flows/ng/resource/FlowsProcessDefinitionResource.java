package it.cnr.si.flows.ng.resource;

import java.util.List;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.security.AuthoritiesConstants;

@RestController
@RequestMapping("rest/processdefinitions")
public class FlowsProcessDefinitionResource {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public Object getAllProcessDefinitions() {

        List<ProcessDefinition> listraw = repositoryService.createProcessDefinitionQuery().latestVersion().list();

        List<ProcessDefinitionResponse> list = restResponseFactory.createProcessDefinitionResponseList(listraw);

        // Get result and set pagination parameters
        DataResponse response = new DataResponse();
        response.setStart(0);
        response.setSize(list.size());
        response.setTotal(list.size());
        response.setData(list);
        return response;


    }

    @RequestMapping(value = "/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity getProcessDefinitionById(@PathVariable String key) {

        ProcessDefinition definitionraw = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).latestVersion().singleResult();

        if (definitionraw != null) {
            ProcessDefinitionResponse definition = restResponseFactory.createProcessDefinitionResponse(definitionraw);
            return ResponseEntity.ok(definition);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
