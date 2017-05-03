package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.task.Attachment;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.AttachmentResponse;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("rest/processInstances")
public class FlowsProcessInstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
    @Autowired
    private RestResponseFactory restResponseFactory;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ProcessInstanceResource processInstanceResource;
    @Autowired
    private RepositoryService repositoryService;
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
    @Secured(AuthoritiesConstants.USER)
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

    // TODO refactor in path param
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getProcessInstanceById(@RequestParam("processInstanceId") String processInstanceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // PrecessInstance metadata
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).includeProcessVariables().singleResult();
            result.put("entity", restResponseFactory.createHistoricProcessInstanceResponse(processInstance));

            // ProcessDefinition (static) metadata
            ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl)repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

            // Attachments
            List<Attachment> processInstanceAttachments = taskService.getProcessInstanceAttachments(processInstanceId);
            List<AttachmentResponse> collect = processInstanceAttachments.stream().map(a -> restResponseFactory.createAttachmentResponse(a)).collect(Collectors.toList());
            result.put("attachments", collect);

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

            // History
            List<HistoricActivityInstance> historyQuery = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceStartTime()
                    .asc()
                    .list();
            result.put("history", restResponseFactory.createHistoricActivityInstanceResponseList(historyQuery));

        } catch (Exception e) { // TODO
            LOGGER.error("Errore: ", e);
        }
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
            processInstances = historyService.createHistoricProcessInstanceQuery().unfinished().includeProcessVariables().list();
        } else {
            processInstances = historyService.createHistoricProcessInstanceQuery().finished().includeProcessVariables().list();
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

    /* ----------- */


}
