package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.CoolFlowsBridgeService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.activiti.rest.common.api.DataResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

/**
 * @author mtrycz
 *
 */
@RestController
@RequestMapping("api/tasks")
public class FlowsTaskResource {

    @Inject
    private TaskService taskService;
    @Inject
    private FlowsTaskService flowsTaskService;

    @Autowired(required = false) @Deprecated
    private CoolFlowsBridgeService coolBridgeService;

    @Inject
    private RuntimeService runtimeService;



    @PostMapping(value = "/mytasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getMyTasks(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        DataResponse response = flowsTaskService.getMyTask(req, processDefinition, firstResult, maxResults, order);

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
    public ResponseEntity<Object> completeTask(MultipartHttpServletRequest req) throws IOException {

        return flowsTaskService.completeTask(req);
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
}