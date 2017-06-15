package it.cnr.si.flows.ng.service;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.image.ProcessDiagramGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Collections;

/**
 * Created by cirone on 15/06/17.
 */
@Service
public class FlowsProcessDiagramService {

    @Inject
    private RuntimeService runtimeService;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private ProcessDiagramGenerator pdg;
    @Inject
    private ProcessEngineConfiguration processEngineConfiguration;
    @Inject
    private HistoryService historyService;


    public InputStream getDiagramForProcessInstance(@PathVariable String id, String font) {
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
            // per cerchiare l'endEvent con cui un processo e' eventualmente terminato
//            HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();
//            HistoricActivityInstance endActivity = query
//                    .activityType(ELEMENT_EVENT_END)
//                    .processInstanceId(((HistoricProcessInstanceEntity) hpi).getProcessInstanceId())
//                    .singleResult();

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
        return resource;
    }

    private boolean containsActiveTasks(SubProcess sp, String processInstanceId) {
        for (FlowElement fe : sp.getFlowElements()) {
            if (runtimeService.getActiveActivityIds(processInstanceId).contains(fe.getId()))
                return true;
        }
        return false;
    }
}
