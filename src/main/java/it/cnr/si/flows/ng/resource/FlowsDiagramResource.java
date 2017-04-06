package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsProcessDiagramGenerator;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
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
import java.util.Collections;

import static org.activiti.bpmn.constants.BpmnXMLConstants.ELEMENT_EVENT_END;


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
    private FlowsProcessDiagramGenerator pdg;
    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;
    @Autowired
    private HistoryService historyService;

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
    public ResponseEntity<InputStreamResource> getDiagramForProcessInstance(
            @PathVariable String id, String font)
            throws IOException {
        InputStream resource;
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(id).singleResult();

        if (font == null || font.isEmpty())
            font = "Arial";

        if (processInstance != null) {
            ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            for (FlowElement fe : bpmnModel.getProcesses().get(0).getFlowElements()) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(fe.getId());
                if (fe instanceof SubProcess) {
                    graphicInfo.setExpanded(containsActiveTasks((SubProcess) fe, processInstance.getId()));
                }
            }
            resource = pdg.generateDiagram(bpmnModel, "png", runtimeService.getActiveActivityIds(processInstance.getId()),
                                           Collections.<String>emptyList(),
                                           font, font, font,
                                           processEngineConfiguration.getClassLoader(), 1.2);
        } else {
            HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
            HistoricProcessInstance hpi = historicProcessInstanceQuery.processInstanceId(id).singleResult();

            BpmnModel bpmnModel = repositoryService.getBpmnModel(hpi.getProcessDefinitionId());
            HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();
            HistoricActivityInstance endActivity = query
                    .activityType(ELEMENT_EVENT_END)
                    .processInstanceId(((HistoricProcessInstanceEntity) hpi).getProcessInstanceId())
                    .singleResult();
            for (FlowElement fe : bpmnModel.getProcesses().get(0).getFlowElements()) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(fe.getId());
                if (fe instanceof SubProcess) {
                    graphicInfo.setExpanded(containsActiveTasks((SubProcess) fe, hpi.getId()));
                }
            }
            resource = pdg.generateDiagram(bpmnModel, "png",
                                           font, font, font,
                                           processEngineConfiguration.getClassLoader(), 1.2);
        }
        return ResponseEntity.ok(new InputStreamResource(resource));
    }

    @RequestMapping(value = "/diagram/taskInstance/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramPerTaskInstanceId(
            @PathVariable String id)
            throws IOException {

        Task task = taskService.createTaskQuery().taskId(id).singleResult();

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


    private boolean containsActiveTasks(SubProcess sp, String processInstanceId) {
        for (FlowElement fe : sp.getFlowElements()) {
            if (runtimeService.getActiveActivityIds(processInstanceId).contains(fe.getId()))
                return true;
        }
        return false;
    }
}
