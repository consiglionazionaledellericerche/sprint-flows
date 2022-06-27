package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;


import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.SecurityService;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/processDefinitions")
public class FlowsProcessDefinitionResource {

    @Inject
    private RepositoryService repositoryService;
    @Inject
    private MembershipService membershipService;
    @Inject
    private SecurityService securityService;

    @Autowired
    private RestResponseFactory restResponseFactory;

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, List<ProcessDefinitionResponse>>> getAllProcessDefinitions(
            @RequestParam(value="includeSuspended", required=false) boolean includeSuspended) {

        Map<String, List<ProcessDefinitionResponse>> result = new HashMap<>();

        ProcessDefinitionQuery pdQuery = repositoryService.createProcessDefinitionQuery().latestVersion();
        if (!includeSuspended)
            pdQuery.active();
        List<ProcessDefinition> allPD = pdQuery.list();

        // lista delle Process Definition che l'utente loggato può avviare
        List<ProcessDefinition> listBootable = allPD.stream()
                .filter(pd -> !pd.isSuspended())
                .filter(pd -> canStartProcesByDefinitionKey(pd.getKey()))
                .collect(Collectors.toList());

        List<ProcessDefinitionResponse> responseListAll = restResponseFactory.createProcessDefinitionResponseList(allPD);
        result.put("all", responseListAll);
        List<ProcessDefinitionResponse> responseListBootable = restResponseFactory.createProcessDefinitionResponseList(listBootable);
        result.put("bootable", responseListBootable);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<ProcessDefinitionResponse> getProcessDefinitionById(@PathVariable String key) {

        ProcessDefinition definitionraw = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).latestVersion().singleResult();

        if (definitionraw != null) {
            ProcessDefinitionResponse definition = restResponseFactory.createProcessDefinitionResponse(definitionraw);
            return new ResponseEntity<>(definition, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @RequestMapping(value = "/activate/{key}", method = RequestMethod.POST)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<Void> activateProcessDefinitionByKey(@PathVariable String key) throws IOException {
    	repositoryService.activateProcessDefinitionByKey(key);
        return new ResponseEntity<>(HttpStatus.OK);
    }
   
    
    
    @RequestMapping(value = "/suspend/{key}", method = RequestMethod.POST)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<Void> suspendProcessDefinitionByKey(@PathVariable String key) throws IOException {
    	repositoryService.suspendProcessDefinitionByKey(key);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/send", method = RequestMethod.POST)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<Void> updateProcessDefinition(@RequestParam("procDef_data") MultipartFile procDef) throws IOException {

        DeploymentBuilder builder = repositoryService.createDeployment();
        builder.addInputStream(procDef.getOriginalFilename(), procDef.getInputStream());
        builder.deploy();

        return ResponseEntity.ok().build();

    }


    public boolean canStartProcesByDefinitionKey(String definitionKey) {
    	
        return membershipService.getAllRolesForUser(securityService.getCurrentUserLogin())
                .stream()
                .map(Utils::removeLeadingRole)
                .anyMatch(a -> a.startsWith("abilitati#" + definitionKey + "@") );
    }


    private List<ProcessDefinition> canStartProcesByDefinitionList(List<ProcessDefinition> processDefinitions) {
        List<ProcessDefinition> response = new ArrayList<>();
        for (ProcessDefinition processDefinition : processDefinitions) {
            if (canStartProcesByDefinitionKey(processDefinition.getKey()))
                response.add(processDefinition);
        }
        return response;
    }
    
}