package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.service.FirmaService;

import org.activiti.bpmn.model.Activity;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.UserTask;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


@Controller
@RequestMapping("rest")
public class FlowsDiagramResource {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ProcessDiagramGenerator pdg;
    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private FirmaService firmaService;

    @RequestMapping(value = "/diagram/processDefinition/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramForProcessDefinition(
            @PathVariable String id)
                    throws IOException {

        InputStream resourceAsStream = repositoryService.getProcessDiagram(id);

        return ResponseEntity.ok(new InputStreamResource(resourceAsStream));
    }


    @RequestMapping(value = "/diagram/processInstance/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramForProcessInstance(
            @PathVariable String id, String font)
                    throws IOException {

        Execution execution = runtimeService.createExecutionQuery().executionId(id).singleResult();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(execution.getProcessInstanceId()).singleResult();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());


        if (font == null || font.equals("") )
            font = processEngineConfiguration.getActivityFontName();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());

        //        org.activiti.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
        //
        //        org.activiti.bpmn.model.Process process2 = process.clone();
        //        Collection<FlowElement> flowElements = process2.getFlowElements();

        for (FlowElement fe : bpmnModel.getProcesses().get(0).getFlowElements() ) {
            GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(fe.getId());
            System.out.println(fe.getId() + fe.getName());
            if (fe instanceof SubProcess) {
                SubProcess sp = (SubProcess) fe;
                if (!containsActiveTasks(sp, processInstance.getId())) {
                    graphicInfo.setExpanded(false);
                    graphicInfo.setHeight(100);
                    graphicInfo.setWidth(150);
                    for (FlowElement innerFe : sp.getFlowElements()) {

                        System.out.println("   "+innerFe.getId() +": "+ innerFe.getName() +" "+ innerFe.getClass().getName());

                        GraphicInfo innerGraphicInfo = bpmnModel.getGraphicInfo(innerFe.getId());
                        if (innerGraphicInfo != null) {
                            innerGraphicInfo.setWidth(1);
                            innerGraphicInfo.setHeight(1);
                            innerGraphicInfo.setX(graphicInfo.getX());
                            innerGraphicInfo.setY(graphicInfo.getY());
                        }
                    }
                }
            }
        };

        InputStream resource = pdg.generateDiagram(bpmnModel, "png", runtimeService.getActiveActivityIds(processInstance.getId()),
                Collections.<String>emptyList(),
                font, font, font,
                processEngineConfiguration.getClassLoader(), 1.2);

        return ResponseEntity.ok(new InputStreamResource(resource));
    }

    private boolean containsActiveTasks(SubProcess sp, String processInstanceId) {
        for (FlowElement fe : sp.getFlowElements()) {
            if (runtimeService.getActiveActivityIds(processInstanceId).contains(fe))
                return true;
        }
        return false;
    }
    @RequestMapping(value = "/diagram/taskInstance/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramPerTaskInstanceId(
            @PathVariable String id)
                    throws IOException {

        Task task = taskService.createTaskQuery().taskId(id).singleResult();
        Execution execution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();

        return getDiagramForProcessInstance(task.getProcessInstanceId(), null);

    }

    @RequestMapping(value = "/diagram/taskInstance/{id}/{font}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramPerTaskInstanceId(
            @PathVariable String id,
            @PathVariable String font)
                    throws IOException {

        Task task = taskService.createTaskQuery().taskId(id).singleResult();

        return getDiagramForProcessInstance(task.getProcessInstanceId(), font);

    }

}
