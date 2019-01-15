package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.CoolFlowsBridgeService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.TaskQuery;
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


    @RequestMapping(value = "/mytasks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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


    @RequestMapping(value = "/availabletasks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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


    @RequestMapping(value = "/taskAssignedInMyGroups", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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


    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable("id") String taskId) {

        Map<String, Object> response = flowsTaskService.getTask(taskId);

        return ResponseEntity.ok(response);
    }



    @RequestMapping(value = "/claim/{taskId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
//    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.canAssignTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> claimTask(@PathVariable("taskId") String taskId) {

        String username = SecurityUtils.getCurrentUserLogin();
        taskService.claim(taskId, username);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/claim/{taskId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canAssignTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> unclaimTask(@PathVariable("taskId") String taskId) {
        taskService.unclaim(taskId);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/{id}/{user:.*}", method = RequestMethod.PUT)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canAssignTask(#id, #user)")
    public ResponseEntity<Map<String, Object>> assignTask(
            HttpServletRequest req,
            @PathVariable("id") String id,
            @PathVariable("user") String user) {

        //    todo: non è ancora usata nell'interfaccia, fare i test
        taskService.setAssignee(id, user);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "complete", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canCompleteTaskOrStartProcessInstance(#req, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Object> completeTask(MultipartHttpServletRequest req) throws IOException {

        return flowsTaskService.completeTask(req);
    }


    @RequestMapping(value = "/taskCompletedByMe", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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
    @RequestMapping(value = "/coolAvailableTasks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
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