package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;
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
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceActionRequest;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResource;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static it.cnr.si.flows.ng.utils.Utils.*;

@Controller
@RequestMapping("api/processInstances")
public class FlowsProcessInstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    @Autowired
    private RestResponseFactory restResponseFactory;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ProcessInstanceResource processInstanceResource;
    @Autowired
    private FlowsAttachmentResource attachmentResource;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;





    /**
     * Restituisce le Processs Instances avviate dall'utente loggato
     *
     * @param active booleano che indica se recuperare le MIE Process Instancess attive o quelle terminate
     * @return the my processes
     */
    @RequestMapping(value = "/myProcessInstances", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getMyProcessInstances(
            @RequestParam boolean active,
            @RequestParam String processDefinition,
            @RequestParam String order,
            @RequestParam int firstResult,
            @RequestParam int maxResults) {

        String username = SecurityUtils.getCurrentUserLogin();
        List<HistoricProcessInstance> list;
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();



        if (active) {
            historicProcessInstanceQuery.variableValueEquals("initiator", username)
                    .unfinished()
                    .includeProcessVariables();
        } else {
            historicProcessInstanceQuery.variableValueEquals("initiator", username)
                    .finished()
                    .includeProcessVariables();
        }

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            historicProcessInstanceQuery.processDefinitionKey(processDefinition);
        if (order.equals(ASC))
            historicProcessInstanceQuery.orderByProcessInstanceStartTime().asc();
        else
            historicProcessInstanceQuery.orderByProcessInstanceStartTime().desc();

        list = historicProcessInstanceQuery.listPage(firstResult, maxResults);

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(list.size()); //numero flussi restituito
        response.setTotal(historicProcessInstanceQuery.count()); //totale Flussi
        response.setData(restResponseFactory.createHistoricProcessInstanceResponseList(list));
        response.setOrder(order);

        return ResponseEntity.ok(response);
    }

    // TODO refactor in path param
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getProcessInstanceById(@RequestParam("processInstanceId") String processInstanceId) {
        Map<String, Object> result = new HashMap<>();

        // PrecessInstance metadata
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).includeProcessVariables().singleResult();
        result.put("entity", restResponseFactory.createHistoricProcessInstanceResponse(processInstance));

        // ProcessDefinition (static) metadata
        ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

        // Attachments
        ResponseEntity<List<FlowsAttachment>> attachements = attachmentResource.getAttachementsForProcessInstance(processInstanceId);
        result.put("attachments", attachements.getBody());

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

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    /**
     * Restituisce le Process Instances attive o terminate.
     *
     * @param active boolean active
     * @return le process Instance attive o terminate
     */
    @RequestMapping(value = "/getProcessInstances", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured({AuthoritiesConstants.ADMIN})
    @Timed
    public ResponseEntity getProcessInstances(@RequestParam("active") boolean active) {
        List<HistoricProcessInstance> processInstances;
        if (active) {
            processInstances = historyService.createHistoricProcessInstanceQuery()
                    .unfinished()
                    .includeProcessVariables().list();
        } else {
            processInstances = historyService.createHistoricProcessInstanceQuery()
                    .finished().or().deleted()
                    .includeProcessVariables().list();
        }
        return new ResponseEntity<>(restResponseFactory.createHistoricProcessInstanceResponseList(processInstances), HttpStatus.OK);
    }

    @RequestMapping(value = "deleteProcessInstance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured({AuthoritiesConstants.ADMIN})
    @Timed
    public HttpServletResponse delete(
            HttpServletResponse response,
            @RequestParam(value = "processInstanceId", required = true) String processInstanceId,
            @RequestParam(value = "deleteReason", required = true) String deleteReason) {
        processInstanceResource.deleteProcessInstance(processInstanceId, deleteReason, response);
        return response;
    }

    // TODO ???
    @RequestMapping(value = "suspendProcessInstance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured({AuthoritiesConstants.ADMIN})
    @Timed
    public ProcessInstanceResponse suspend(
            HttpServletRequest request,
            @RequestParam(value = "processInstanceId", required = true) String processInstanceId) {
        ProcessInstanceActionRequest action = new ProcessInstanceActionRequest();
        action.setAction(ProcessInstanceActionRequest.ACTION_SUSPEND);
        return processInstanceResource.performProcessInstanceAction(processInstanceId, action, request);
    }

    /**
     * Funzionalità di Ricerca delle Process Instances.
     *
     * @param req               the req
     * @param processInstanceId Il processInstanceId della ricerca
     * @param active            Boolean che indica se ricercare le Process Insrtances attive o terminate
     * @param order             L'ordine in cui vogliamo i risltati ('ASC' o 'DESC')
     * @return le response entity frutto della ricerca
     */
    @RequestMapping(value = "/search/{processInstanceId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> search(
            HttpServletRequest req,
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam("active") boolean active,
            @RequestParam("order") String order,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults) {

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
        return ResponseEntity.ok(result);
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
    /* ----------- */


}
