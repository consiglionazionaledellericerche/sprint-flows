package it.cnr.si.flows.ng.service;

import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static it.cnr.si.flows.ng.utils.Utils.DESC;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
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
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.opencsv.CSVWriter;

import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.aop.FlowsHistoricProcessInstanceQuery;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;

/**
 * Created by cirone on 15/06/17.
 */
@Service
public class FlowsProcessInstanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceService.class);
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private HistoryService historyService;
    @Inject
    private RestResponseFactory restResponseFactory;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private TaskService taskService;
    @Inject
    private ViewRepository viewRepository;
    @Inject
    private ManagementService managementService;
    @Inject
    private RuntimeService runtimeService;


    public Map<String, Object> getProcessInstanceWithDetails(String processInstanceId) {
        Map<String, Object> result = new HashMap<>();

        // PrecessInstance metadata
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
        result.put("entity", restResponseFactory.createHistoricProcessInstanceResponse(processInstance));

        // ProcessDefinition (static) metadata
        ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

        // Attachments
        Map<String, FlowsAttachment> attachements = flowsAttachmentService.getAttachementsForProcessInstance(processInstanceId);
        result.put("attachments", attachements);

        final Map<String, Object> identityLinks = new HashMap<>();
        Map<String, Object> processLinks = new HashMap<>();
        processLinks.put("links", restResponseFactory.createRestIdentityLinks(runtimeService.getIdentityLinksForProcessInstance(processInstanceId)));
        identityLinks.put("process", processLinks);
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
        ArrayList<Map<String, Object>> history = new ArrayList<>();
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

        FlowsHistoricProcessInstanceQuery processQuery = new FlowsHistoricProcessInstanceQuery(managementService);

        List<String> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(Utils::removeLeadingRole)
                        .collect(Collectors.toList());

        if (!authorities.contains("ADMIN"))
            processQuery.setVisibleToGroups(authorities);


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

        processQuery.includeProcessVariables();
        long totalItems = processQuery.count();
        result.put("totalItems", totalItems);

        List<HistoricProcessInstance> processesRaw;

        if (firstResult != -1 && maxResults != -1)
            processesRaw = processQuery.listPage(firstResult, maxResults);
        else
            processesRaw = processQuery.list();

        List<HistoricProcessInstanceResponse> processes = restResponseFactory.createHistoricProcessInstanceResponseList(processesRaw);
        result.put("processInstances", processes);
        return result;
    }


    public void buildCsv(List<HistoricProcessInstanceResponse> processInstances, PrintWriter printWriter, String processDefinitionKey) throws IOException {
        // vista (campi e variabili) da inserire nel csv in base alla tipologia di flusso selezionato
        View view = null;
        if (!processDefinitionKey.equals(ALL_PROCESS_INSTANCES)) {
            view = viewRepository.getViewByProcessidType(processDefinitionKey, "export-csv");
        }
        CSVWriter writer = new CSVWriter(printWriter, '\t');
        ArrayList<String[]> entriesIterable = new ArrayList<>();
        boolean hasHeaders = false;
        ArrayList<String> headers = new ArrayList<>();
        headers.add("Business Key");
        headers.add("Start Date");
        for (HistoricProcessInstanceResponse pi : processInstances) {
            List<RestVariable> variables = pi.getVariables();
            ArrayList<String> tupla = new ArrayList<>();
            //field comuni a tutte le Process Instances (Business Key, Start date)
            tupla.add(pi.getBusinessKey());
            tupla.add(Utils.formattaDataOra(pi.getStartTime()));

            //field specifici per ogni procesDefinition
            if (view != null) {
                JSONArray fields = new JSONArray(view.getView());
                for (int i = 0; i < fields.length(); i++) {
                    JSONObject field = fields.getJSONObject(i);
                    tupla.add(Utils.filterProperties(variables, field.getString("varName")));
                    //solo per il primo ciclo, prendo le label dei field specifici
                    if (!hasHeaders)
                        headers.add(field.getString("label"));
                }
            }
            if (!hasHeaders) {
                //inserisco gli headers come intestazione dei field del csv
                entriesIterable.add(0, getArray(headers));
                hasHeaders = true;
            }
            entriesIterable.add(getArray(tupla));
        }
        writer.writeAll(entriesIterable);
        writer.close();
    }


    private void processDate(HistoricProcessInstanceQuery taskQuery, String key, String value) {
        try {
            Date date = Utils.parsaData(value);

            if (key.contains("Less")) {
                taskQuery.variableValueLessThanOrEqual(key.replace("Less", ""), date);
            } else if (key.contains("Great"))
                taskQuery.variableValueGreaterThanOrEqual(key.replace("Great", ""), date);
        } catch (ParseException e) {
            LOGGER.error("Errore nel parsing della data {} - ", value, e);
        }
    }


    private String[] getArray(ArrayList<String> tupla) {
        String[] entries = new String[tupla.size()];
        entries = tupla.toArray(entries);
        return entries;
    }
}
