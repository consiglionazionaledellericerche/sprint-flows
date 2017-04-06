package it.cnr.si.flows.ng.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Attachment;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.AttachmentResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceActionRequest;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResource;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;

@Controller
@RequestMapping("rest/processInstances")
public class FlowsProcessInstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
    @Autowired
    protected RestResponseFactory restResponseFactory;
    @Autowired
    HistoryService historyService;
    @Autowired
    IdentityService identityService;
    @Autowired
    ProcessInstanceResource processInstanceResource;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;


    /**
     * Restituisce le Processs Instances avviate dall'utente loggato
     *
     * @param active booleano che indica se recuperare le MIE Process Instancess attive o quelle terminate
     * @return the my processes
     */
    @RequestMapping(value = "/myProcessInstances", method = RequestMethod.GET)
    @ResponseBody
    @Timed
    public ResponseEntity<DataResponse> getMyProcessInstances(
            @RequestParam boolean active) {

        String username = SecurityUtils.getCurrentUserLogin();
        List<HistoricProcessInstance> list;
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
        if (active) {
            list = historicProcessInstanceQuery.variableValueEquals("initiator", username)
                    .unfinished()
                    .includeProcessVariables().list();
        } else {
            list = historicProcessInstanceQuery.variableValueEquals("initiator", username)
                    .finished()
                    .includeProcessVariables().list();
        }

        DataResponse response = new DataResponse();
        response.setStart(0);
        response.setSize(list.size());
        response.setTotal(list.size());
        response.setData(list);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/processes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> getProcesses(
            HttpServletRequest req,
            @RequestParam Map<String, String> params) {

        return null;
        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //        BindingSession session = cmisService.getCurrentBindingSession(req);
        //
        //        try {
        //            return new ResponseEntity<Map<String,Object>>(workflowService.getProcesses(user, session, params), HttpStatus.OK);
        //        } catch (IOException e) {
        //            LOGGER.error(e.getMessage(), e);
        //            Map<String, Object> response = new HashMap<>();
        //            response.put("error", e.getMessage());
        //            return new ResponseEntity<Map<String,Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        //        } catch (AlfrescoResponseException e) {
        //            LOGGER.error(e.getMessage() + " " + e.getResponse(), e);
        //            return new ResponseEntity<Map<String,Object>>(e.getResponse(), HttpStatus.INTERNAL_SERVER_ERROR);
        //        }
    }


    // TODO refactor in path param
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getProcessInstanceById(@RequestParam("processInstanceId") String processInstanceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).includeProcessVariables().singleResult();
            result.put("entity", restResponseFactory.createHistoricProcessInstanceResponse(processInstance));

            List<Attachment> processInstanceAttachments = taskService.getProcessInstanceAttachments(processInstanceId);
            List<AttachmentResponse> collect = processInstanceAttachments.stream().map(a -> restResponseFactory.createAttachmentResponse(a)).collect(Collectors.toList());
            result.put("attachments", collect);

            List<HistoricActivityInstance> historyQuery = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .list();

            result.put("history", restResponseFactory.createHistoricActivityInstanceResponseList(historyQuery));
        } catch (Exception e){
            LOGGER.error("Errore: ", e);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    /**
     * Restituisce le Process Instances attive.
     *
     * @return the process instances actives
     */
    @RequestMapping(value = "/active", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured({AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN})
    @Timed
    public ResponseEntity<List<ProcessInstanceResponse>> getActiveProcessInstances() {
        List<ProcessInstance> processInstance = runtimeService.createProcessInstanceQuery().includeProcessVariables().list();
        return new ResponseEntity<>(restResponseFactory.createProcessInstanceResponseList(processInstance), HttpStatus.OK);
    }

    /**
     * Restituisce le Process Instances completate.
     *
     * @return the completed process instances
     */
    @RequestMapping(value = "/completed", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured({AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN})
    @Timed
    public ResponseEntity<List<HistoricProcessInstanceResponse>> getCompletedProcessInstances() {
        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery().finished().includeProcessVariables().list();
        return new ResponseEntity<>(restResponseFactory.createHistoricProcessInstanceResponseList(processInstances), HttpStatus.OK);
    }

    @RequestMapping(value = "deleteProcessInstance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured({AuthoritiesConstants.ADMIN})
    @Timed
    public HttpServletResponse delete(
            HttpServletResponse response,
            @RequestParam(value = "processInstanceId", required = true) String processInstanceId,
            @RequestParam(value = "deleteReason", required = true) String deleteReason) {
        processInstanceResource.deleteProcessInstance(processInstanceId, deleteReason, response);
        return response;
    }


    @RequestMapping(value = "suspendProcessInstance", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured({AuthoritiesConstants.ADMIN})
    @Timed
    public ProcessInstanceResponse suspend(
            HttpServletRequest request,
            @RequestParam(value = "processInstanceId", required = true) String processInstanceId) {
        ProcessInstanceActionRequest action = new ProcessInstanceActionRequest();
        action.setAction(ProcessInstanceActionRequest.ACTION_SUSPEND);
        return processInstanceResource.performProcessInstanceAction(processInstanceId, action, request);
    }

    /* ----------- */


}
