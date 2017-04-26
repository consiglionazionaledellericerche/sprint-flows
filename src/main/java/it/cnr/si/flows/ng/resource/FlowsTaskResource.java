package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.service.UserService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.*;


/**
 * @author mtrycz
 *
 */
@RestController
@RequestMapping("rest/tasks")
public class FlowsTaskResource {

    public static final String TASK_EXECUTOR = "esecutore";
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTaskResource.class);
    private static final String ALL_PROCESS_INSTANCES = "all";
    @Autowired
    protected RestResponseFactory restResponseFactory;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    @Inject
    private UserService userService;
    @Inject
    private CounterService counterService;
    @Inject
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowsAttachmentService attachmentService;
    @Autowired
    private HistoryService historyService;


    public static long extractId(String id) throws IllegalArgumentException {
        try {
            if (id.contains("$"))
                id = id.split("\\$")[1];
            return Long.parseLong(id);

        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("L'id dato non è valido", e);
        }
    }

    // TODO magari un giorno avremo degli array, ma per adesso ce lo facciamo andare bene cosi'
    public static Map<String, Object> extractParameters(MultipartHttpServletRequest req) {

        Map<String, Object> data = new HashMap<>();

        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            data.put(paramName, req.getParameter(paramName));
        }

        return data;

    }

    @RequestMapping(value = "/mytasks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getMyTasks() {

        String username = SecurityUtils.getCurrentUserLogin();

        List<Task> listraw = taskService.createTaskQuery()
                .taskAssignee(username)
                .includeProcessVariables()
                .list();

        List<TaskResponse> list = restResponseFactory.createTaskResponseList(listraw);

        DataResponse response = new DataResponse();
        response.setStart(0);
        response.setSize(list.size());
        response.setTotal(list.size());
        response.setData(list);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/availabletasks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getAvailableTasks() {

        String username = SecurityUtils.getCurrentUserLogin();
        List<String> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
//                        .map(FlowsTaskResource::removeLeadingRole) //todo: vedere con Martin (le authorities sono ROLE_USER (come ora) o USER (come prima))
                        .collect(Collectors.toList());

        List<Task> listraw = taskService.createTaskQuery()
                .taskCandidateUser(username)
                .taskCandidateGroupIn(authorities)
                .includeProcessVariables()
                .list();

        List<TaskResponse> list = restResponseFactory.createTaskResponseList(listraw);

        DataResponse response = new DataResponse();
        response.setStart(0);
        response.setSize(list.size());
        response.setTotal(list.size());
        response.setData(list);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<TaskResponse> getTaskInstance(
            @PathVariable("id") String id) {

        Task taskRaw = taskService.createTaskQuery().taskId(id).includeProcessVariables().singleResult();

        TaskResponse task = restResponseFactory.createTaskResponse(taskRaw);


        return ResponseEntity.ok(task);
        //      long taskId = extractId(id);
        //
        //        Task task = taskService.createTaskQuery().taskId(""+taskId).singleResult();
        //        List<Task> tasks = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
        //        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        //
        //        List<Map<String, Object>> tasksMaps = tasks.stream().map(t -> mappaTaskInMap(t)).collect(Collectors.toList());
        //
        //        Map<String, Object> result = new HashMap<>();
        //        Map<String, Object> definition = new HashMap<>();
        //        definition.put("id", "cnrdsftm:validaTask");
        //
        //        Map<String, Object> mappaTaskInMap = mappaTaskInMap(task);
        //        ((Map<String, Object>) mappaTaskInMap.get("entry")).put("tasks", tasksMaps);
        //        ((Map<String, Object>) mappaTaskInMap.get("entry")).put("workflowInstance", mappaProcessInstanceInMap(pi));
        //        ((Map<String, Object>) mappaTaskInMap.get("entry")).put("definition", definition);
        //
        //
        //        result.put("data", mappaTaskInMap.get("entry"));
        //
        //
        //        return new ResponseEntity<Map<String, Object>>(result, HttpStatus.OK);

        //        // TODO verificare se ci possono essere parametri aggiuntivi utili//        boolean detailed = Boolean.parseBoolean(params.get("detailed"));
        //        long taskId = Utils.extractId(id);
        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //        BindingSession session = cmisService.getCurrentBindingSession(req);
        //
        //        try {
        //            Map<String, Object> taskInstance = flowsTaskService.getTask(user, session, taskId, detailed);
        //            return new ResponseEntity<Map<String,Object>>(taskInstance, HttpStatus.OK);
        //        } catch (PermissionException e) {
        //            LOGGER.error(e.getMessage(), e);
        //            return new ResponseEntity<Map<String,Object>>(HttpStatus.FORBIDDEN);
        //        } catch (IOException e) {
        //            Map<String, Object> response = new HashMap<>();
        //            response.put("error", e.getMessage());
        //            return new ResponseEntity<Map<String,Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        //        }
    }

    /**
     * @param req
     * @param taskId
     * @param params
     * @return
     */
    @RequestMapping(value = "/claim/{taskId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> claimTask(
            HttpServletRequest req,
            @PathVariable("taskId") String taskId) {

        String username = SecurityUtils.getCurrentUserLogin();
        LOGGER.info("Setting owner of task {} to {}", taskId, username);

        List<IdentityLink> list = taskService.getIdentityLinksForTask(taskId);
        String groupCandidate = null;
        for (IdentityLink link : list) {
            if (link.getType().equals("candidate")) {
                groupCandidate = link.getGroupId();
                break;
            }
        }
        String finalGroupCandidate = groupCandidate;
        boolean canClaim = userService.getUserWithAuthorities().getAuthorities().stream().anyMatch(autority -> autority.getName().equals(finalGroupCandidate));

        if (canClaim)
            taskService.claim(taskId, username);
        else
            return new ResponseEntity<Map<String, Object>>(HttpStatus.FORBIDDEN);

        return new ResponseEntity<Map<String,Object>>(HttpStatus.OK);
    }

    @RequestMapping(value = "/claim/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> unclaimTask(
            HttpServletRequest req,
            @PathVariable("id") String id) {

        String username = SecurityUtils.getCurrentUserLogin();
        String assignee = taskService.createTaskQuery()
                .taskId(id)
                .singleResult().getAssignee();

        if (username.equals(assignee)) {
            taskService.unclaim(id);
            return new ResponseEntity<Map<String,Object>>(HttpStatus.OK);
        } else {
            return new ResponseEntity<Map<String,Object>>(HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping(value = "/{id}/{user}", method = RequestMethod.PUT)
    @Timed
    public ResponseEntity<Map<String, Object>> assignTask(
            HttpServletRequest req,
            @PathVariable("id") String id,
            @PathVariable("user") String user) {

//    todo: test
//    todo: chi può asegnare un task?
//        String username = SecurityUtils.getCurrentUserLogin();

        taskService.setAssignee(id, user);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    // TODO returns ResponseEntity<Map<String, Object>>
    @RequestMapping(value = "variables/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> getTaskVariables(
            HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("id") String id)
            throws IOException {

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> list = new HashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();

        result.put("list", list);
        list.put("entries", entries);

        //        taskService.getVariables(id);

        return new ResponseEntity<Map<String,Object>>(result, HttpStatus.OK);

        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //        LOGGER.debug("getTaskVariables user: "+ user +", id: "+id);
        //
        //        try {
        //            Map<String, Object> workflowVariables = workflowService.getTaskVariables(user, id);
        //            JSONObject res = new JSONObject(workflowVariables);
        //            PrintWriter writer = resp.getWriter();
        //            writer.write(res.toString());
        //            writer.flush();
        //            //            return new ResponseEntity<Map<String,Object>>(workflowVariables, HttpStatus.OK);
        //        } catch (IllegalAccessException e) {
        //            LOGGER.info(ERRORE_PERMESSI_TASK);
        //            LOGGER.info("L'utente ha richiesto variabili per un flusso che non deve poter vedere", e);
        //            getVariablesOldMethod(req, resp, id);
        //            //          return new ResponseEntity<Map<String,Object>>(HttpStatus.FORBIDDEN);// ;resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        //        } catch (WorkflowException e) {
        //            LOGGER.info("Errore durante il recupero di un flusso", e);
        //            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        //            //          return new ResponseEntity<Map<String,Object>>(HttpStatus.INTERNAL_SERVER_ERROR);//resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        //        }
    }

    @RequestMapping(value = "complete", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> completeTask(MultipartHttpServletRequest req) {

        try {
            String username = SecurityUtils.getCurrentUserLogin();

            Map<String, Object> data = extractParameters(req);
            data.putAll(attachmentService.extractAttachmentsVariables(req));

            String taskId = (String) req.getParameter("taskId");
            String definitionId = (String) req.getParameter("definitionId");


            if ( isEmpty(taskId) && isEmpty(definitionId))
                return ResponseEntity.badRequest().body("Fornire almeno un taskId o un definitionId");

            if ( isNotEmpty(taskId) ) {
                // aggiungo l'identityLink che indica l'utente che esegue il task
                taskService.addUserIdentityLink(taskId, username, TASK_EXECUTOR);

                taskService.setVariablesLocal(taskId, data);
                taskService.complete(taskId, data);

                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();

                String counterId = processDefinition.getName() +"-"+ Calendar.getInstance().get(Calendar.YEAR);
                String key =  counterId +"-"+ counterService.getNext(counterId);

                data.put("title", key);
                data.put("pippo", "pluto");
                data.put("initiator", username);
                data.put("startDate", new Date());

                ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);

                LOGGER.debug("Avviata istanza di processo {}, id: {}", key, instance.getId());

                ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(instance);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (IOException e) {
            LOGGER.error("Errore nel processare i files:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore nel processare i files");
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

        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery();

        if (!processInstanceId.equals(ALL_PROCESS_INSTANCES))
            taskQuery.processDefinitionKey(processInstanceId);

        if (active)
            taskQuery.unfinished();
        else
            taskQuery.finished();

        for (int i = 0; i < params.length(); i++) {
            JSONObject appo = params.optJSONObject(i);
            String key = appo.getString("key");
            String value = appo.getString("value");
            String type = appo.getString("type");
            //wildcard ("%") di default ma non a TUTTI i campi
            switch (type) {
                case "textEqual":
                    taskQuery.processVariableValueEquals(key, value);
                    break;
                case "boolean":
                    // gestione variabili booleane
                    taskQuery.processVariableValueEquals(key, Boolean.valueOf(value));
                    break;
                case "date":
                    processDate(taskQuery, key, value);
                    break;
                default:
                    //variabili con la wildcard  (%value%)
                    taskQuery.processVariableValueLikeIgnoreCase(key, "%" + value + "%");
                    break;
            }
        }
        if (order.equals(ASC))
            taskQuery.orderByTaskCreateTime().asc();
        else if (order.equals(DESC))
            taskQuery.orderByTaskCreateTime().desc();

        long totalItems = taskQuery.includeProcessVariables().count();
        result.put("totalItems", totalItems);

        List<HistoricTaskInstance> taskRaw = taskQuery.includeProcessVariables().listPage(firstResult, maxResults);
        List<HistoricTaskInstanceResponse> tasks = restResponseFactory.createHistoricTaskInstanceResponseList(taskRaw);
        result.put("tasks", tasks);
        return ResponseEntity.ok(result);
    }


    @RequestMapping(value = "/taskCompleted", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Object> getTasksCompletedForMe(
            HttpServletRequest req,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults) {

        Map<String, Object> result = new HashMap<>();
        String username = SecurityUtils.getCurrentUserLogin();

        List<HistoricTaskInstance> response = new ArrayList<>();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(username)
                .includeProcessVariables().includeTaskLocalVariables()
                .list();

        for (HistoricTaskInstance task : tasks) {
            List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
            for (HistoricIdentityLink hil : identityLinks) {
                if (hil.getType().equals(TASK_EXECUTOR) && hil.getUserId().equals(username))
                    response.add(task);
            }
        }
        result.put("tasks", response);
        return ResponseEntity.ok(result);
    }


    private void processDate(HistoricTaskInstanceQuery taskQuery, String key, String value) {
        try {
            Date date = sdf.parse(value);

            if (key.contains("Less")) {
                taskQuery.processVariableValueLessThanOrEqual(key.replace("Less", ""), date);
            } else if (key.contains("Great"))
                taskQuery.processVariableValueGreaterThanOrEqual(key.replace("Great", ""), date);
        } catch (ParseException e) {
            LOGGER.error("Errore nel parsing della data {} - ", value, e);
        }
    }
}
