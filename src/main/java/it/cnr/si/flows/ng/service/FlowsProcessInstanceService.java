package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.service.api.RestResponseFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cirone on 15/06/17.
 */
@Service
public class FlowsProcessInstanceService {

    @Inject
    FlowsAttachmentService flowsAttachmentService;
    @Inject
    private HistoryService historyService;
    @Inject
    private RestResponseFactory restResponseFactory;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private TaskService taskService;


    public Map<String, Object> getProcessInstanceWithDetails(@RequestParam("processInstanceId") String processInstanceId) {
        Map<String, Object> result = new HashMap<>();
        // PrecessInstance metadata
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).includeProcessVariables().singleResult();
        result.put("entity", restResponseFactory.createHistoricProcessInstanceResponse(processInstance));

        // ProcessDefinition (static) metadata
        ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

        // Attachments
        Map<String, FlowsAttachment> attachements = flowsAttachmentService.getAttachementsForProcessInstance(processInstanceId);
        result.put("attachments", attachements);

        // IdentityLinks (candidate groups)
        final Map<String, Object> identityLinks = new HashMap<>();
        taskService.createTaskQuery().processInstanceId(processInstanceId).active().list().forEach(
                task -> {
                    Map<String, Object> identityLink = new HashMap<>();
                    String taskDefinitionKey = task.getTaskDefinitionKey();
                    PvmActivity taskDefinition = processDefinition.findActivity(taskDefinitionKey);
                    TaskDefinition taskDef = (TaskDefinition) taskDefinition.getProperty("taskDefinition");
                    List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());

                    identityLink.put("links", restResponseFactory.createRestIdentityLinks(links));
                    identityLink.put("assignee", taskDef.getAssigneeExpression());
                    identityLink.put("candidateGroups", taskDef.getCandidateGroupIdExpressions());
                    identityLink.put("candidateUsers", taskDef.getCandidateUserIdExpressions());

                    identityLinks.put(task.getId(), identityLink);
                });
        result.put("identityLinks", identityLinks);

        //History
        ArrayList<Map> history = new ArrayList<>();
        historyService.createHistoricTaskInstanceQuery()
                .includeTaskLocalVariables()
                .processInstanceId(processInstanceId)
                .list()
                .forEach(
                        task -> {
                            List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(task.getId());
                            HashMap<String, Object> entity = new HashMap<>();
                            entity.put("historyTask", restResponseFactory.createHistoricTaskInstanceResponse(task));
                            entity.put("historyIdentityLink", restResponseFactory.createHistoricIdentityLinkResponseList(links));
                            history.add(entity);
                        });
        result.put("history", history);
        return result;
    }

}
