package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.config.ldap.CNRUser;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.repository.UserRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.service.SecurityService;
import it.cnr.si.service.UserService;
import org.activiti.engine.FormService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.isEmpty;
import static it.cnr.si.flows.ng.utils.Utils.isNotEmpty;


/**
 * @author mtrycz
 *
 */
@RestController
@RequestMapping("rest/tasks")
public class FlowsTaskResource {

    @Deprecated
    private static final String ERRORE_PERMESSI_TASK = "ERRORE PERMESSI TASK";

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTaskResource.class);
    @Autowired
    protected RestResponseFactory restResponseFactory;
    @Inject
    private UserRepository userRepository;
    @Inject
    private UserService userService;
    @Inject
    private CounterService counterService;
    @Inject
    private SecurityService securityService;
    @Inject
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private IdentityService identityService;

    @Autowired
    private TaskService taskService;

    public static long extractId(String id) throws IllegalArgumentException {
        try {
            if (id.contains("$"))
                id = id.split("\\$")[1];
            return Long.parseLong(id);

        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException("L'id dato non è valido", e);
        }
    }

    @RequestMapping(value = "/mytasks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> getMyTasks(Principal user,
            @RequestParam Map<String, String> params) {

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
    public ResponseEntity<DataResponse> getAvailableTasks(
            @RequestParam Map<String, String> params) {

        String username = SecurityUtils.getCurrentUserLogin();
        List<String> authorities =
                SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
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
            HttpServletRequest req,
            @PathVariable("id") String id,
            @RequestParam Map<String, String> params) {

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
     * @param id
     * @param params
     * @return
     */
    @RequestMapping(value = "/claim/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> claimTask(
            HttpServletRequest req,
            @PathVariable("id") String id) {

        String username = SecurityUtils.getCurrentUserLogin();
        LOGGER.info("Setting owner of task "+ id +" to "+ username);

        boolean canClaim = true; // TODO
        if (canClaim) {
            taskService.claim(id, username);
        }

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

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> assignTask(
            HttpServletRequest req,
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> data) {

        return null;
        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //        BindingSession session = cmisService.getCurrentBindingSession(req);
        //        String cm_owner = (String) data.get("cm_owner");
        //        long taskId = Utils.extractId(id);
        //
        //        LOGGER.info("Setting owner of task "+ id +" to "+ cm_owner);
        //
        //        try {
        //            Map<String, Object> taskInstance = flowsTaskService.setTaskOwner(user, session, taskId, data);
        //
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
    public ResponseEntity<Object> completeTask(
            MultipartHttpServletRequest req) {

        Map<String, Object> data = new HashMap<>();
        String username = SecurityUtils.getCurrentUserLogin();

        String taskId = (String) req.getParameter("data[taskId]");
        String definitionId = (String) req.getParameter("data[definitionId]");

        if ( isEmpty(taskId) && isEmpty(definitionId))
            return ResponseEntity.badRequest().body("Fornire almeno un taskId o un definitionId");

        if ( isNotEmpty(taskId) ) {
            taskService.complete(taskId, data);
            return new ResponseEntity<Object>(HttpStatus.OK);

        } else {

            try {
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();

                String counterId = processDefinition.getName() +"-"+ Calendar.getInstance().get(Calendar.YEAR);
                String key =  counterId +"-"+ counterService.getNext(counterId);
                data.put("title", key);
                data.put("pippo", "pluto");
                data.put("initiator", username);
                data.put("startDate", new Date());

                Iterator<String> i = req.getFileNames();
                while (i.hasNext()) {
                    String fileName = i.next();
                    LOGGER.debug("inserisco come variabile il file "+ fileName);
                    if (fileName.endsWith("[]")) { // multiple

                    } else {
                        MultipartFile file = req.getFile(fileName);
                        data.put(fileName, file.getBytes());
                        data.put(fileName+"_name", file.getOriginalFilename());
                        data.put(fileName+"_user", username);
                    }
                }

                ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);


                i = req.getFileNames();
                while (i.hasNext()) {
                    String fileName = i.next();
                    if (fileName.endsWith("[]")) { // multiple

                    } else {
                        MultipartFile file = req.getFile(fileName);
                        taskService.createAttachment(
                                fileName,
                                null,
                                instance.getId(),
                                file.getOriginalFilename(),
                                null,
                                file.getInputStream());
                    }
                }

                LOGGER.debug("Avviata istanza di processo "+ key +", id: "+ instance.getId());

                ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(instance);
                return new ResponseEntity<Object>(response, HttpStatus.OK);



            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore nel processare i files");
            }
        }



        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //        BindingSession session = cmisService.getCurrentBindingSession(req);
        //        boolean userHasWriteAccessToTask = true; // TODO
        //
        //        if (userHasWriteAccessToTask) {
        //            try {
        //                workflowService.completeTask(user, id, data, session);
        //            } catch (AlfrescoResponseException e) {
        //                LOGGER.error(e.getMessage() + " " + e.getResponse(), e);
        //                return new ResponseEntity<Map<String,Object>>(e.getResponse(), HttpStatus.INTERNAL_SERVER_ERROR);
        //            } catch (PermissionException e) {
        //                LOGGER.error(e.getMessage(), e);
        //                return new ResponseEntity<Map<String,Object>>(HttpStatus.FORBIDDEN);
        //            } catch (IOException e) {
        //                LOGGER.error(e.getMessage(), e);
        //                Map<String, Object> response = new HashMap<>();
        //                response.put("error", e.getMessage());
        //                return new ResponseEntity<Map<String,Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        //            }
        //        }
        //
        //        return null;
    }
}
