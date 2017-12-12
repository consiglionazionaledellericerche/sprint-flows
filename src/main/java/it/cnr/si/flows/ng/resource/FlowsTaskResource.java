package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;
import org.activiti.engine.TaskService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
//    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.canAssignTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> unclaimTask(@PathVariable("taskId") String taskId) {
        taskService.unclaim(taskId);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/{id}/{user}", method = RequestMethod.PUT)
    @Secured(AuthoritiesConstants.USER)
    @Timed
////    todo: scommentare quando sarà pronta anche l'interfaccia grafica
////    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canAssignTask(#id, #user)")
    public ResponseEntity<Map<String, Object>> assignTask(
            HttpServletRequest req,
            @PathVariable("id") String id,
            @PathVariable("user") String user) {

        //    todo: non è ancora usata nell'interfaccia, fare i test
        taskService.setAssignee(id, user);

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "complete", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canCompleteTaskOrStartProcessInstance(#req, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Object> completeTask(MultipartHttpServletRequest req) throws IOException {

        return flowsTaskService.completeTask(req);
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
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Object> search(
            HttpServletRequest req,
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam("active") boolean active,
            @RequestParam("order") String order,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults) {

        Map<String, Object> result = flowsTaskService.search(req, processInstanceId, active, order, firstResult, maxResults);
        return ResponseEntity.ok(result);
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


    /**
     * Export csv: esporta il result-set di una search sulle Process Instances in un file Csv
     *
     * @param req               the req
     * @param res               the res
     * @param processInstanceId the process instance id della search-request
     * @param active            the active Process Instances attive o terminate
     * @param order             the order ordinamento del result-set
     * @param firstResult       the first result (in caso di esportazione parziale del result-set)
     * @param maxResults        the max results (in caso di esportazione parziale del result-set)
     * @throws IOException the io exception
     */
    @RequestMapping(value = "/exportCsv/{processInstanceId}", headers = "Accept=application/vnd.ms-excel", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = "application/vnd.ms-excel")
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public void exportCsv(
            HttpServletRequest req,
            HttpServletResponse res,
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam("active") boolean active,
            @RequestParam("order") String order,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults) throws IOException {

        Map<String, Object> result = flowsTaskService.search(
                req, processInstanceId, active, order, firstResult, maxResults);

        flowsTaskService.buildCsv(
                (List<HistoricTaskInstanceResponse>) result.get("tasks"),
                res.getWriter(), processInstanceId);
    }


}