package it.cnr.si.flows.ng.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.codahale.metrics.annotation.Timed;


@Controller
@RequestMapping("rest")
public class DiagramResource {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
//    @Autowired
    private ProcessDiagramGenerator pdg;

    @RequestMapping(value = "/diagram/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public void getDiagramForProcess(
            @PathVariable String id,
            HttpServletRequest request,
            HttpServletResponse response
            ) throws IOException {
        
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(id)
                .singleResult();

        String diagramResourceName = processDefinition.getDiagramResourceName();
        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getDiagramResourceName());


        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        org.apache.commons.io.IOUtils.copy(resourceAsStream, response.getOutputStream());

    }

    @RequestMapping(value = "/diagram", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public void getDiagram(
            HttpServletRequest request,
            HttpServletResponse response
            ) throws IOException {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("permessiFerieProcess")
                .latestVersion()
                .singleResult();

        String diagramResourceName = processDefinition.getDiagramResourceName();
        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getDiagramResourceName());


        response.setContentType(MediaType.IMAGE_PNG_VALUE);
        org.apache.commons.io.IOUtils.copy(resourceAsStream, response.getOutputStream());

    }

    @RequestMapping(value = "/diagram2", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public void getDiagram2(
            HttpServletRequest request,
            HttpServletResponse response
            ) throws IOException {
//        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
//                .processDefinitionKey("permessiFerieProcess")
//                .latestVersion()
//                .singleResult();
//
//        String diagramResourceName = processDefinition.getDiagramResourceName();
//        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getDiagramResourceName());
//
//
//        response.setContentType(MediaType.IMAGE_PNG_VALUE);
//        org.apache.commons.io.IOUtils.copy(resourceAsStream, response.getOutputStream());

        TaskResponse tr;
        
        ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("permessiFerieProcess")
                .latestVersion()
                .singleResult();


        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());


        if (processDefinition != null && processDefinition.isGraphicalNotationDefined()) {
          BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
          InputStream resource = pdg.generateDiagram(bpmnModel, "png", runtimeService.getActiveActivityIds(processInstance.getId()));

          response.setContentType(MediaType.IMAGE_PNG_VALUE);
          org.apache.commons.io.IOUtils.copy(resource, response.getOutputStream());

        } else {
          throw new ActivitiIllegalArgumentException("Process instance with id '" + processInstance.getId() + "' has no graphical notation defined.");
        }
    }
}
