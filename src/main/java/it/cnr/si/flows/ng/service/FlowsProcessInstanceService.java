package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static it.cnr.si.flows.ng.utils.Utils.*;

/**
 * Created by cirone on 15/06/17.
 */
@Service
public class FlowsProcessInstanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceService.class);
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
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");


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

    public Map<String, Object> search(HttpServletRequest req, String processInstanceId, boolean active, String order, int firstResult, int maxResults) {
        String jsonString = "";
        Map<String, Object> result = new HashMap<>();

        try {
            jsonString = IOUtils.toString(req.getReader());
        } catch (Exception e) {
            LOGGER.error("Errore nella letture dello stream della request", e);
        }
        JSONArray params = new JSONObject(jsonString).getJSONArray("params");

        HistoricProcessInstanceQuery processQuery = historyService.createHistoricProcessInstanceQuery();

        if (!processInstanceId.equals(ALL_PROCESS_INSTANCES))
            processQuery.processDefinitionKey(processInstanceId);

        if (active)
            processQuery.unfinished();
        else
            processQuery.finished();

        for (int i = 0; i < params.length(); i++) {
            JSONObject appo = params.optJSONObject(i);
            String key = appo.getString("key");
            String value = appo.getString("value");
            String type = appo.getString("type");
            //wildcard ("%") di default ma non a TUTTI i campi
            switch (type) {
                case "textEqual":
                    processQuery.variableValueEquals(key, value);
                    break;
                case "boolean":
                    // gestione variabili booleane
                    processQuery.variableValueEquals(key, Boolean.valueOf(value));
                    break;
                case "date":
                    processDate(processQuery, key, value);
                    break;
                default:
                    //variabili con la wildcard  (%value%)
                    processQuery.variableValueLikeIgnoreCase(key, "%" + value + "%");
                    break;
            }
        }
        if (order.equals(ASC))
            processQuery.orderByProcessInstanceStartTime().asc();
        else if (order.equals(DESC))
            processQuery.orderByProcessInstanceStartTime().desc();

        long totalItems = processQuery.includeProcessVariables().count();
        result.put("totalItems", totalItems);

        List<HistoricProcessInstance> taskRaw = processQuery.includeProcessVariables().listPage(firstResult, maxResults);
        List<HistoricProcessInstanceResponse> tasks = restResponseFactory.createHistoricProcessInstanceResponseList(taskRaw);
        result.put("processInstances", tasks);
        return result;
    }


    private void processDate(HistoricProcessInstanceQuery taskQuery, String key, String value) {
        try {
            Date date = sdf.parse(value);

            if (key.contains("Less")) {
                taskQuery.variableValueLessThanOrEqual(key.replace("Less", ""), date);
            } else if (key.contains("Great"))
                taskQuery.variableValueGreaterThanOrEqual(key.replace("Great", ""), date);
        } catch (ParseException e) {
            LOGGER.error("Errore nel parsing della data {} - ", value, e);
        }
    }


}
