package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.ProcessDefinitionAndTaskIdEmptyException;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.service.MembershipService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.*;

/**
 * @author mtrycz
 *
 */
@RestController
@RequestMapping("api/tasks")
public class FlowsTaskResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTaskResource.class);
    private static final String ERROR_MESSAGE = "message";
    @Inject
    private RestResponseFactory restResponseFactory;
    @Inject
    private MembershipService membershipService;
    @Inject
    private FlowsUserDetailsService flowsUserDetailsService;
    @Inject
    private CounterService counterService;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private TaskService taskService;
    @Inject
    private FlowsAttachmentService attachmentService;
    @Inject
    private HistoryService historyService;
    @Inject
    private FlowsAttachmentResource attachmentResource;
    @Inject
    private PermissionEvaluatorImpl permissionEvaluator;
    private Utils utils = new Utils();


    @RequestMapping(value = "/mytasks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
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

        taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskQuery);

        utils.orderTasks(order, taskQuery);

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

    @RequestMapping(value = "/availabletasks", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getAvailableTasks(
            HttpServletRequest req,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {

        String username = SecurityUtils.getCurrentUserLogin();
        List<String> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(Utils::removeLeadingRole)
                        .collect(Collectors.toList());

        TaskQuery taskQuery = taskService.createTaskQuery()
                .taskCandidateUser(username)
                .taskCandidateGroupIn(authorities)
                .includeProcessVariables();

        taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskQuery);

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processDefinition);

        utils.orderTasks(order, taskQuery);

        List<TaskResponse> list = restResponseFactory.createTaskResponseList(taskQuery.listPage(firstResult, maxResults));

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(list.size());
        response.setTotal(taskQuery.count());
        response.setData(list);

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

        String username = SecurityUtils.getCurrentUserLogin();

        TaskQuery taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskService.createTaskQuery().includeProcessVariables());

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processDefinition);


        utils.orderTasks(order, taskQuery);

        //filtro (in "members") gli utenti che appartengono agli stessi gruppi dell'utente loggato
        List<String> myGroups = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .collect(Collectors.toList());
        List<String> members = new ArrayList<>();
        for (String myGroup : myGroups) {
            if (myGroup.indexOf("afferenza") <= -1 && myGroup.indexOf("USER") <= -1 && myGroup.indexOf("DEPARTMENT") <= -1 && myGroup.indexOf("PREVIUOS") <= -1 && (myGroup != null)) {
                List<String> userWithMyMembership = membershipService.findMembersInGroup(myGroup);
                userWithMyMembership.remove(username);
                members.removeAll(userWithMyMembership);
                members.addAll(userWithMyMembership);
            }
        }

//		TODO: da analizzare se le prestazioni sono migliori rispetto a farsi dare la lista di task attivi e ciclare per quali il member è l'assignee (codice di Martin sottostante) 

        List<TaskResponse> list1 = new ArrayList<TaskResponse>();

        for(int i = 0; i < members.size(); i++){
            List<TaskResponse> appo1 = restResponseFactory.createTaskResponseList(taskQuery.taskAssignee(members.get(i)).list());
            list1.addAll(appo1);
        }

        List<TaskResponse> responseList = list1.subList(firstResult <= list1.size() ? firstResult : list1.size(),
                                                        maxResults <= list1.size() ? maxResults : list1.size());

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(responseList.size());
        response.setTotal(list1.size());
        response.setData(responseList);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable("id") String taskId) {

        Map<String, Object> response = new HashMap<>();
        Task taskRaw = taskService.createTaskQuery().taskId(taskId).includeProcessVariables().singleResult();

        // task + variables
        TaskResponse task = restResponseFactory.createTaskResponse(taskRaw);
        response.put("task", task);

        // attachments
        ResponseEntity<Map<String, FlowsAttachment>> attachementsEntity = attachmentResource.getAttachementsForTask(taskId);
        Map<String, FlowsAttachment> attachments = attachementsEntity.getBody();
        attachments.values().stream().forEach(e -> e.setBytes(null)); // il contenuto dei file non mi serve, e rallenta l'UI
        response.put("attachments", attachments);

        return ResponseEntity.ok(response);
    }


    @RequestMapping(value = "/claim/{taskId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.canAssignTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> claimTask(@PathVariable("taskId") String taskId) {

        String username = SecurityUtils.getCurrentUserLogin();
        taskService.claim(taskId, username);

        return new ResponseEntity<Map<String, Object>>(HttpStatus.OK);
    }


    @RequestMapping(value = "/claim/{taskId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.canAssignTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> unclaimTask(@PathVariable("taskId") String taskId) {
        taskService.unclaim(taskId);
        return new ResponseEntity<Map<String, Object>>(HttpStatus.OK);
    }


    @RequestMapping(value = "/{id}/{user}", method = RequestMethod.PUT)
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

        String username = SecurityUtils.getCurrentUserLogin();

        String taskId = (String) req.getParameter("taskId");
        String definitionId = (String) req.getParameter("processDefinitionId");
        if ( isEmpty(taskId) && isEmpty(definitionId) )
            throw new ProcessDefinitionAndTaskIdEmptyException();

        Map<String, Object> data = extractParameters(req);
        data.putAll(attachmentService.extractAttachmentsVariables(req));

        if (isEmpty(taskId)) {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();
            try {
                String counterId = processDefinition.getName() + "-" + Calendar.getInstance().get(Calendar.YEAR);
                String key = counterId + "-" + counterService.getNext(counterId);

                data.put("title", key);
                data.put("initiator", username);
                data.put("startDate", new Date());

                ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);

                LOGGER.info("Avviata istanza di processo {}, id: {}", key, instance.getId());

                ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(instance);
                return new ResponseEntity<>(response, HttpStatus.OK);

            } catch (Exception e) {
                String errorMessage = String.format("Errore nell'avvio della Process Instances di tipo %s con eccezione:", processDefinition);
                LOGGER.error(errorMessage, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(ERROR_MESSAGE, errorMessage));
            }
        } else {
            try {
                // aggiungo l'identityLink che indica l'utente che esegue il task
                taskService.addUserIdentityLink(taskId, username, TASK_EXECUTOR);
                taskService.setVariablesLocal(taskId, data);
                taskService.complete(taskId, data);

                return new ResponseEntity<>(HttpStatus.OK);
            } catch (Exception e) {
                //Se non riesco a completare il task rimuovo l'identityLink che indica "l'esecutore" del task e restituisco un INTERNAL_SERVER_ERROR
                //l'alternativa sarebbe aggiungere l'identityLink dopo avwer completato il task ma posso creare identityLink SOLO di task attivi
                String errorMessage = String.format("Errore durante il tentativo di completamento del task %s da parte dell'utente %s: %s", taskId, username, e.getMessage());
                LOGGER.error(errorMessage);
                taskService.deleteUserIdentityLink(taskId, username, TASK_EXECUTOR);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(ERROR_MESSAGE, errorMessage));
            }
        }
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

        Map<String, Object> result = new HashMap<>();

        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery();

        if (!processInstanceId.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processInstanceId);

        if (active)
            taskQuery.unfinished();
        else
            taskQuery.finished();
        String jsonString = "";

        try {
            jsonString = IOUtils.toString(req.getReader());
        } catch (Exception e) {
            LOGGER.error(ERRORE_NELLA_LETTURE_DELLO_STREAM_DELLA_REQUEST, e);
        }

        taskQuery = (HistoricTaskInstanceQuery) utils.extractProcessSearchParams(taskQuery, new JSONObject(jsonString).getJSONArray(PROCESS_PARAMS));
        taskQuery = (HistoricTaskInstanceQuery) utils.orderTasks(order, taskQuery);

        long totalItems = taskQuery.includeProcessVariables().count();
        result.put("totalItems", totalItems);

        List<HistoricTaskInstance> taskRaw = taskQuery.includeProcessVariables().listPage(firstResult, maxResults);
        List<HistoricTaskInstanceResponse> tasks = restResponseFactory.createHistoricTaskInstanceResponseList(taskRaw);
        result.put("tasks", tasks);
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

        String username = SecurityUtils.getCurrentUserLogin();

        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(username)
                .includeProcessVariables().includeTaskLocalVariables();

        query = (HistoricTaskInstanceQuery) utils.searchParamsForTasks(req, query);

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            query.processDefinitionKey(processDefinition);

        query = (HistoricTaskInstanceQuery) utils.orderTasks(order, query);
        //seleziono solo i task in cui il TASK_EXECUTOR sia l'utente che sta facendo la richiesta
        List<HistoricTaskInstance> taskList = new ArrayList<>();
        for (HistoricTaskInstance task : query.list()) {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
            for (HistoricIdentityLink hil : identityLinks) {
                if (hil.getType().equals(TASK_EXECUTOR) && hil.getUserId().equals(username))
                    taskList.add(task);
            }
        }
        List<HistoricTaskInstanceResponse> resultList = restResponseFactory.createHistoricTaskInstanceResponseList(
                taskList.subList(firstResult, (firstResult + maxResults <= taskList.size()) ? firstResult + maxResults : taskList.size()));

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(resultList.size());// numero di task restituiti
        response.setTotal(taskList.size()); //numero totale di task avviati da me
        response.setData(resultList);

        return ResponseEntity.ok(response);
    }


    // TODO magari un giorno avremo degli array, ma per adesso ce lo facciamo andare bene cosi'
    private static Map<String, Object> extractParameters(MultipartHttpServletRequest req) {

        Map<String, Object> data = new HashMap<>();
        List<String> parameterNames = Collections.list(req.getParameterNames());
        parameterNames.stream().forEach(paramName -> {
            // se ho un json non aggiungo i suoi singoli campi
            if (!parameterNames.contains(paramName.split("\\[")[0] +"_json"))
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