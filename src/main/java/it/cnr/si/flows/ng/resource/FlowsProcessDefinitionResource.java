package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import com.hazelcast.security.SecurityContext;

import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder.Case;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/processDefinitions")
public class FlowsProcessDefinitionResource {

    @Inject
    private RepositoryService repositoryService;


    @Autowired
    private RestResponseFactory restResponseFactory;

    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    // TODO refactor, non usiamo DataResponse di Activity, ma e' meglio ResponseEntity di Spring (o direttamente un oggetto)
    // Inoltre abbiamo bisogno sia di un getAllProcessDefinitions sia di un "GetAllProcessDefinitionsAvviaibiliDaMe"
    // -> splittare il metedo
    public DataResponse getAllProcessDefinitions() {

        List<ProcessDefinition> listraw = repositoryService.createProcessDefinitionQuery().latestVersion().list();
        listraw = listraw.stream().filter(d -> canStartProcesByDefinitionKey(d.getKey())).collect(Collectors.toList());

        // Get result and set pagination parameters
        List<ProcessDefinitionResponse> list = restResponseFactory.createProcessDefinitionResponseList(listraw);
        DataResponse response = new DataResponse();
        response.setStart(0);
        response.setSize(list.size());
        response.setTotal(list.size());
        response.setData(list);
        return response;
    }

    public boolean canStartProcesByDefinitionKey(String definitionKey) {

        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return authorities.stream().anyMatch(a -> a.getAuthority().startsWith("ROLE_abilitati#"+ definitionKey));

    }


    @RequestMapping(value = "/{key}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<ProcessDefinitionResponse> getProcessDefinitionById(@PathVariable String key) {

        ProcessDefinition definitionraw = repositoryService.createProcessDefinitionQuery().processDefinitionKey(key).latestVersion().singleResult();

        if (definitionraw != null) {
            ProcessDefinitionResponse definition = restResponseFactory.createProcessDefinitionResponse(definitionraw);
            return new ResponseEntity<ProcessDefinitionResponse>(definition, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<Void> updateProcessDefinition(@RequestParam("procDef") MultipartFile procDef) throws IOException {

        DeploymentBuilder builder = repositoryService.createDeployment();
        builder.addInputStream(procDef.getOriginalFilename(), procDef.getInputStream());
        builder.deploy();

        return ResponseEntity.ok().build();

    }

}
