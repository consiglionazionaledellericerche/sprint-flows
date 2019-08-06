package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.jada.firma.arss.stub.SignReturnV2;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.FlowsPermissionException;
import it.cnr.si.flows.ng.exception.ProcessDefinitionAndTaskIdEmptyException;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

import static it.cnr.si.flows.ng.service.FlowsFirmaService.ERRORI_ARUBA;
import static it.cnr.si.flows.ng.service.FlowsFirmaService.NOME_FILE_FIRMA;
import static it.cnr.si.flows.ng.utils.Enum.Azione.Firma;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Firmato;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;
import static it.cnr.si.flows.ng.utils.Utils.isEmpty;

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
    private FlowsAttachmentService attachmentService;
    @Inject
    private FlowsFirmaService flowsFirmaService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private PermissionEvaluatorImpl permissionEvaluator;
    @Inject
    private UserDetailsService flowsUserDetailsService;


    @PostMapping(value = "/mytasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getMyTasks(
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order,
            @RequestBody(required = false) String body){

        DataResponse response = flowsTaskService.getMyTasks(body!=null ? new JSONArray(body) : new JSONArray(),
                                                            processDefinition,
                                                            firstResult,
                                                            maxResults,
                                                            order);
        return ResponseEntity.ok(response);
    }


    @PostMapping(value = "/availabletasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getAvailableTasks(
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order,
            @RequestBody(required = false) String body) {

        DataResponse response = flowsTaskService.getAvailableTask(body!=null ? new JSONArray(body) : new JSONArray(),
                                                                  processDefinition,
                                                                  firstResult,
                                                                  maxResults,
                                                                  order);
        return ResponseEntity.ok(response);
    }



    @PostMapping(value = "/taskAssignedInMyGroups", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> taskAssignedInMyGroups(
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order,
            @RequestBody(required = false) String body) {

        DataResponse response = flowsTaskService.taskAssignedInMyGroups(body!=null ? new JSONArray(body) : new JSONArray(),
                                                                        processDefinition,
                                                                        firstResult,
                                                                        maxResults,
                                                                        order);
        return ResponseEntity.ok(response);
    }



    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable("id") String taskId) {

        Map<String, Object> response = flowsTaskService.getTask(taskId);

        return ResponseEntity.ok(response);
    }


    @GetMapping(value = "/activeByProcessInstanceId/{processInstanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Timed
    public ResponseEntity<TaskResponse> getActiveTaskByProcessInstanceId(@PathVariable("processInstanceId") String processInstanceId) {

        Task task = flowsTaskService.getActiveTaskForProcessInstance(processInstanceId);
        TaskResponse taskResponse = restResponseFactory.createTaskResponse(task);

        return ResponseEntity.ok(taskResponse);
    }

    @PutMapping(value = "/claim/{taskId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.canClaimTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> claimTask(@PathVariable("taskId") String taskId) {

        String username = SecurityUtils.getCurrentUserLogin();
        try {
            taskService.claim(taskId, username);
        } catch(ActivitiObjectNotFoundException notFoundException){
            LOGGER.error("Errore nella presa in carico del task {} da parte dell`utente {}: TASK NON TROVATO", taskId, username);
            notFoundException.printStackTrace();
        }catch (Exception e){
            LOGGER.error("Errore nella presa in carico del task {} da parte dell`utente {}", taskId, username);
            e.printStackTrace();
        }
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


    @PutMapping(value = "/addCandidateGroup/{group:.*}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Timed
    public ResponseEntity<Map<String, Object>> addCandidateGroup(
            @RequestParam(name = "processInstanceId", required=false) String processInstanceId,
            @RequestParam(name = "taskId", required=false) String taskId,
            @PathVariable(value = "group") String group) {

        if(taskId == null) {
            // se vengo da pagine in cui ho solo il processInstanceId (tipo ricerca) trovo il taskId
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .includeProcessVariables()
                    .singleResult();
            taskId = task.getId();
        }
        taskService.addCandidateGroup(taskId, group);

        // Aggiungo l`identityLink per la visualizzazione
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        runtimeService.addGroupIdentityLink(task.getProcessInstanceId(), group, PROCESS_VISUALIZER);

        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    

    @DeleteMapping(value = "/removeCandidateGroup/{group:.*}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Timed
    public ResponseEntity<Map<String, Object>> removeCandidateGroup(
            @RequestParam(name = "processInstanceId", required=false) String processInstanceId,
            @RequestParam(name = "taskId", required=false) String taskId,
            @PathVariable(value = "group") String group) {

        if(taskId == null) {
            // se vengo da pagine in cui ho solo il processInstanceId (tipo ricerca) trovo il taskId
            Task task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .includeProcessVariables()
                    .singleResult();
            taskId = task.getId();
        }
        taskService.deleteCandidateGroup(taskId, group);

        // Aggiungo l`identityLink per la visualizzazione
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        runtimeService.deleteGroupIdentityLink(task.getProcessInstanceId(), group, PROCESS_VISUALIZER);

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
    public ResponseEntity<ProcessInstanceResponse> completeTask(MultipartHttpServletRequest req) {

        Map<String, Object> data = extractParameters(req);
        String taskId       = (String) data.get("taskId");
        String definitionId = (String) data.get("processDefinitionId");
        if (isEmpty(taskId) && isEmpty(definitionId))
            throw new ProcessDefinitionAndTaskIdEmptyException();

        if (isEmpty(taskId)) {
            ProcessInstance instance = flowsTaskService.startProcessInstance(definitionId, data);

            return ResponseEntity.ok(restResponseFactory.createProcessInstanceResponse(instance));
        } else {
            flowsTaskService.completeTask(taskId, data);
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }


    @PostMapping(value = "/taskCompletedByMe", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Object> getTasksCompletedByMe(
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order,
            @RequestBody(required = false) String body) {

        DataResponse response = flowsTaskService.getTasksCompletedByMe(body!=null ? new JSONArray(body) : new JSONArray(),
                                                                       processDefinition,
                                                                       firstResult,
                                                                       maxResults,
                                                                       order);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/signMany")
    public ResponseEntity<Map<String, List<String>>> signMany(@RequestParam("username") String username,
                                                              @RequestParam("password") String password,
                                                              @RequestParam("otp") String otp,
                                                              @RequestParam("taskIds") List<String> taskIds)
            throws ArubaSignServiceException, FlowsPermissionException {

        verificaPrecondizioniFirmaMultipla(taskIds);

        List<Task> tasks = new ArrayList<>();
        List<String> nomiFileDaFirmare = new ArrayList<>();
        List<FlowsAttachment> fileDaFirmare = new ArrayList<>();
        List<byte[]> fileContents = new ArrayList<>();

        for (int i = 0; i < taskIds.size(); i++) {
            String id = taskIds.get(i);
            tasks.add(taskService.createTaskQuery().taskId(id).singleResult());
            nomiFileDaFirmare.add(NOME_FILE_FIRMA.get(tasks.get(i).getTaskDefinitionKey()));
            fileDaFirmare.add(taskService.getVariable(id, nomiFileDaFirmare.get(i), FlowsAttachment.class));
            fileContents.add(flowsAttachmentService.getAttachmentContentBytes(fileDaFirmare.get(i)));
        }

        List<String> succesfulTasks = new ArrayList<>();
        List<String> failedTasks    = new ArrayList<>();
        List<SignReturnV2> signResponses = flowsFirmaService.firmaMultipla(username, password, otp, fileContents);

        for (int i = 0; i < taskIds.size(); i++) {
            SignReturnV2 signResponse = signResponses.get(i);
            String taskId = taskIds.get(i);
            String nomeFile = nomiFileDaFirmare.get(i);
            FlowsAttachment att = fileDaFirmare.get(i);

            if (signResponse.getStatus().equals("OK")) {
                String key = taskService.getVariable(taskId, "key", String.class);
                String path = att.getPath();
                String signedFileName = FirmaDocumentoService.getSignedFilename(att.getFilename());
                String uid = flowsAttachmentService.saveOrUpdateBytes(signResponse.getBinaryoutput(), nomeFile, signedFileName, key, path);

                att.setUrl(uid);
                att.setFilename(signedFileName);
                att.setAzione(Firma);
                att.addStato(Firmato);
                att.setUsername(SecurityUtils.getCurrentUserLogin());
                att.setTime(new Date());
                att.setTaskId(taskId);
                att.setTaskName(tasks.get(i).getName());

                Map<String, Object> data = new HashMap<String, Object>() {{
                    put(nomeFile, att);
                    put("sceltaUtente", "Firma Multipla");
                }};
                flowsTaskService.completeTask(taskId, data);

                succesfulTasks.add(taskId);

            } else {
                String taskError = ERRORI_ARUBA.getOrDefault(signResponse.getReturnCode(), "Errore sconosciuto");
                String key = taskService.getVariable(taskId, "key", String.class);
                failedTasks.add(taskId +":"+ key +" - "+ taskError);
            }
        }

        return ResponseEntity.ok(
                new HashMap<String, List<String>>() {{
                    put("success", succesfulTasks);
                    put("failure", failedTasks);
                }});

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
        parameterNames.stream()
                .filter(paramName -> !parameterNames.contains(paramName.split("\\[")[0] + "_json"))
                .filter(paramName -> !paramName.equals("cacheBuster"))
                .forEach(paramName -> data.put(paramName, req.getParameter(paramName)));

        // aggiungo anche i files
        parameterNames.stream()
                .filter( paramName -> paramName.endsWith("_aggiorna") )
                .filter( paramName -> "true".equals(req.getParameter(paramName)) )
                .map( paramName -> paramName.replace("_aggiorna", ""))
                .forEach( paramName -> {

                    Optional.ofNullable(req.getFile(paramName + "_data"))
                            .ifPresent(file -> {
                                try {
                                    data.put(paramName + "_data", file.getBytes());
                                    data.put(paramName + "_filename", file.getOriginalFilename());
                                } catch (IOException e) {
                                    throw new RuntimeException("Errore nella lettura del file", e);
                                }
                            });
                });

        return data;
    }


    private void verificaPrecondizioniFirmaMultipla(List<String> taskIds) throws FlowsPermissionException {

        if ( ! taskIds.stream()
                .allMatch(id -> permissionEvaluator.canCompleteTask(id, flowsUserDetailsService)) )
            throw new FlowsPermissionException("Nel carrello sono presenti compiti per cui l'utente non ha i permessi necessari");


    }
}