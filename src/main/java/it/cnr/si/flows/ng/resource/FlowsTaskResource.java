package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.exception.ProcessDefinitionAndTaskIdEmptyException;
import it.cnr.si.flows.ng.service.CoolFlowsBridgeService;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.*;
import static it.cnr.si.flows.ng.utils.Utils.*;

/**
 * @author mtrycz
 *
 */
@RestController
@RequestMapping("api/tasks")
public class FlowsTaskResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTaskResource.class);

    @Inject
    private TaskService taskService;
    @Inject
    private FlowsTaskService flowsTaskService;
    @Inject
    private RestResponseFactory restResponseFactory;
    @Inject
    private RepositoryService repositoryService;

    @Autowired(required = false) @Deprecated
    private CoolFlowsBridgeService coolBridgeService;

    @Inject
    private RuntimeService runtimeService;
    @Inject
    private RelationshipService relationshipService;
    @Inject
    private CounterService counterService;
    @Inject
    private FlowsAttachmentService attachmentService;


    @PostMapping(value = "/mytasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getMyTasks(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        String username = SecurityUtils.getCurrentUserLogin();

        TaskQuery taskQuery = taskService.createTaskQuery()
                .taskAssignee(username)
                .includeProcessVariables();

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processDefinition);

        taskQuery = (TaskQuery) Utils.searchParamsForTasks(req, taskQuery);

        Utils.orderTasks(order, taskQuery);

        List<TaskResponse> tasksList = restResponseFactory.createTaskResponseList(taskQuery.listPage(firstResult, maxResults));

        //aggiungo ad ogni singola TaskResponse la variabile che indica se il task è restituibile ad un gruppo (true)
        // o se è stato assegnato ad un utente specifico "dal sistema" (false)
        addIsReleasableVariables(tasksList);

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(tasksList.size());
        response.setTotal(taskQuery.count());
        response.setData(tasksList);

        return ResponseEntity.ok(response);
    }



    @PostMapping(value = "/availabletasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getAvailableTasks(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        DataResponse response = flowsTaskService.getAvailableTask(req, processDefinition, firstResult, maxResults, order);

        return ResponseEntity.ok(response);
    }



    @PostMapping(value = "/taskAssignedInMyGroups", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> taskAssignedInMyGroups(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        DataResponse response = flowsTaskService.taskAssignedInMyGroups(req, processDefinition, firstResult, maxResults, order);

        return ResponseEntity.ok(response);
    }



    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable("id") String taskId) {

        Map<String, Object> response = flowsTaskService.getTask(taskId);

        return ResponseEntity.ok(response);
    }



    @PutMapping(value = "/claim/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
//    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.canClaimTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> claimTask(@PathVariable("taskId") String taskId) {

        String username = SecurityUtils.getCurrentUserLogin();
        taskService.claim(taskId, username);

        return new ResponseEntity<>(HttpStatus.OK);
    }



    @PutMapping(value = "/reassign/{assignee:.*}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.isResponsabile(#taskId, #processInstanceId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> reassignTask(
            @RequestParam(name = "processInstanceId", required=false) String processInstanceId,
            @RequestParam(name = "taskId", required=false) String taskId,
            @PathVariable(value = "assignee") String assignee) {

        if(taskId == null) {
            // se vengo da pagine in cui ho solo il processInstanceId (tipo ricerca) trovo il taskId
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .includeProcessVariables()
                    .singleResult();
            taskId = task.getId();
        }
        taskService.setAssignee(taskId, assignee);

        // Aggiungo l`identityLink per la visualizzazione
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        runtimeService.addUserIdentityLink(task.getProcessInstanceId(), taskId, PROCESS_VISUALIZER);

        return new ResponseEntity<>(HttpStatus.OK);
    }



    @DeleteMapping(value = "/claim/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canClaimTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> unclaimTask(@PathVariable("taskId") String taskId) {
        taskService.unclaim(taskId);
        return new ResponseEntity<>(HttpStatus.OK);
    }



    @PostMapping(value = "complete",consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canCompleteTaskOrStartProcessInstance(#req, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<ProcessInstanceResponse> completeTask(MultipartHttpServletRequest req) throws Exception {

        String username = SecurityUtils.getCurrentUserLogin();

        String taskId = (String) req.getParameter("taskId");
        String definitionId = (String) req.getParameter("processDefinitionId");

        if (isEmpty(taskId) && isEmpty(definitionId))
            throw new ProcessDefinitionAndTaskIdEmptyException();

        if (isEmpty(taskId)) {

            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();

            String counterId = processDefinition.getName() + "-" + Calendar.getInstance().get(Calendar.YEAR);
            String key = counterId + "-" + counterService.getNext(counterId);

            Map<String, Object> data = extractParameters(req);
            attachmentService.extractAttachmentVariables(req, data, key);

            ProcessInstance instance = flowsTaskService.startProcessInstance(data, definitionId, key);

            ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(instance);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } else {
            try {
                String key = taskService.getVariable(taskId, "key", String.class);
                Map<String, Object> data = extractParameters(req);
                attachmentService.extractAttachmentVariables(req, data, key);

                // aggiungo l'identityLink che indica l'utente che esegue il task
                taskService.addUserIdentityLink(taskId, username, TASK_EXECUTOR);
                taskService.setVariablesLocal(taskId, data);
                taskService.complete(taskId, data);

                return new ResponseEntity<>(HttpStatus.OK);

            } catch (Exception e) {
                taskService.deleteUserIdentityLink(taskId, username, TASK_EXECUTOR);
                throw e;
            }
        }
    }



    @PostMapping(value = "/taskCompletedByMe", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Object> getTasksCompletedByMe(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        DataResponse response = flowsTaskService.getTasksCompletedByMe(req, processDefinition, firstResult, maxResults, order);

        return ResponseEntity.ok(response);
    }



    @Profile("cnr")
    @Deprecated
    @GetMapping(value = "/coolAvailableTasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Long>> getCoolAvailableTasks() {

        String username = SecurityUtils.getCurrentUserLogin();
        Map<String, Long> result = new HashMap<String, Long>() {{
            put("acquisti", 0L);
            put("flussoApprovvigionamentiIT", 0L);
            put("flussoAttestati", 0L);
            put("flussoDetermineAcquisti", 0L);
            put("flussoMissioniOrdine", 0L);
            put("flussoMissioniRevoca", 0L);
            put("flussoMissioniRimborso", 0L);
            put("flussoRelazioniCDA", 0L);
        }};

        long sprintTasks = taskService.createTaskQuery()
                .taskAssignee(username)
                .or()
                .taskCandidateGroupIn(SecurityUtils.getCurrentUserAuthorities())
                .count();
        result.put("acquisti", sprintTasks);

        List<Map> coolTasks = coolBridgeService.getCoolAvailableAndAssignedTasks(username);

        coolTasks.forEach(t -> {
            Map<String, String> entry = (Map<String, String>) t.get("entry");
            String procDefId = entry.get("processDefinitionId").split(":")[0];
            result.compute(procDefId, (k,v) -> v+1);
        });

        return ResponseEntity.ok(result);
    }

    // TODO magari un giorno avremo degli array, ma per adesso ce lo facciamo andare bene cosi'
    public static Map<String, Object> extractParameters(MultipartHttpServletRequest req) {

        Map<String, Object> data = new HashMap<>();
        List<String> parameterNames = Collections.list(req.getParameterNames());
        parameterNames.stream().forEach(paramName -> {
            // se ho un json non aggiungo i suoi singoli campi (Ed escludo il parametro "cacheBuster")
            if ((!parameterNames.contains(paramName.split("\\[")[0] + "_json")) && (!paramName.equals("cacheBuster")))
                data.put(paramName, req.getParameter(paramName));
        });
        return data;
    }

    private void addIsReleasableVariables(List<TaskResponse> tasks) {
        for (TaskResponse task : tasks) {
            RestVariable isUnclaimableVariable = new RestVariable();
            isUnclaimableVariable.setName("isReleasable");
            // if has candidate groups or users -> can release
            isUnclaimableVariable.setValue(taskService.getIdentityLinksForTask(task.getId())
                    .stream()
                    .anyMatch(l -> l.getType().equals(IdentityLinkType.CANDIDATE)));
            task.getVariables().add(isUnclaimableVariable);
        }
    }
}