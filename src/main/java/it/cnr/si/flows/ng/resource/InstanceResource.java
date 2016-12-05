package it.cnr.si.flows.ng.resource;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.rest.service.api.RestResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.repository.UserRepository;
import it.cnr.si.service.UserService;

@Controller
@RequestMapping("rest/processinstances")
public class InstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceResource.class);

    @Inject
    private UserRepository userRepository;
    @Inject
    private UserService userService;

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;

    private static final String ERRORE_PERMESSI_WORKFLOW = "ERRORE PERMESSI WORKFLOW";

    @RequestMapping(value = "myinstances", method = RequestMethod.GET)
    @ResponseBody
    @Timed
    public ResponseEntity<Map<String, Object>> getMyInstances(
            @RequestParam Optional<Integer> skipCount,
            @RequestParam Optional<Integer> maxItems,
            @RequestParam Optional<String> where,
            HttpServletRequest req) throws IOException {

        return null;
        //        try {
        //
        //            Map<String, Object> result = workflowService.getFilteredResults( skipCount.orElse(0),
        //                                                                             maxItems.orElse(5),
        //                                                                             where.orElse(""),
        //                                                                             user);
        //
        //            return new ResponseEntity<Map<String,Object>>(result, HttpStatus.OK);
        //
        //        } catch (IOException e) {
        //            Map<String, Object> result = new HashMap<>();
        //            LOGGER.error("Sending error response", e);
        //            result.put("error", true);
        //            result.put("success", false);
        //            return new ResponseEntity<Map<String,Object>>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        //        }
    }

    @RequestMapping(value = "/processes", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Map<String, Object>> getProcesses(HttpServletRequest req,
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
    //    @RequestMapping(value = "{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    //    @ResponseBody
    //    @Timed
    //    public ResponseEntity<Map<String, Object>> getInstance(HttpServletRequest req, @PathVariable("id") String id) {
    //
    //        ResponseEntity<Map<String, Object>> response = null;
    //        id = Utils.extractId(id);
    //
    //        try {
    //            CMISUser user = cmisService.getCMISUserFromSession(req);
    //            if (workflowService.isVisible(user, id)) {
    //                Map<String, Object> result = workflowService.getWorkflowInstanceAsAdmin(id);
    //                response = new ResponseEntity<Map<String,Object>>(result, HttpStatus.OK);
    //            } else {
    //                response = new ResponseEntity<Map<String,Object>>(HttpStatus.FORBIDDEN);
    //            }
    //        } catch (IOException e) {
    //            LOGGER.error("Errore nel recuper del flusso "+ id, e);
    //            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    //        }
    //        return response;


    /**
     * Questo deve funzionare solo per il supervisor!!!
     *
     * @param req
     * @param skipCount
     * @param maxItems
     * @param where
     * @param definitionName
     * @return
     */
    @RequestMapping(value = "workflowInstances/{definitionName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<Map<String, Object>> getWorkflowInstances(HttpServletRequest req,
            @RequestParam Optional<Integer> skipCount,
            @RequestParam Optional<Integer> maxItems,
            @RequestParam Optional<String> where,
            @PathVariable("definitionName") String definitionName) {

        return null;
        //        ResponseEntity<Map<String, Object>> responseEntity;
        //
        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //        Map<String, Object> result = new HashMap<>();
        //
        //        if (!isAuthorized(definitionName, user)) {
        //            LOGGER.info("user {} cannot view workflows {}", user.getId(), definitionName);
        //            responseEntity = new ResponseEntity<Map<String,Object>>(new HashMap<>(), HttpStatus.FORBIDDEN);
        //        } else {
        //            Map<String, String[]> params = new HashMap<>();
        //            params.put("where", new String[] {where.get()});
        //            params.put("maxItems", new String[] {maxItems.get().toString()});
        //            params.put("skipCount", new String[] {skipCount.get().toString()});
        //            try {
        //                result = workflowService.getWorkflowListAsAdmin(skipCount.get(), maxItems.get(), where.get(), false);
        //            } catch (IOException e) {
        //                e.printStackTrace();
        //            }
        //            responseEntity = new ResponseEntity<Map<String,Object>>(result, HttpStatus.OK);
        //        }
        //        return responseEntity;
    }


    @RequestMapping(value = "workflowInstancesById/{definitionName}/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<Map<String, Object>> getWorkflowInstanceById(HttpServletRequest req,
            @PathVariable("definitionName") String definitionName,
            @PathVariable("id") String id) {
        return  null;
        //        ResponseEntity<Map<String, Object>> responseEntity;
        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //
        //        if (!isAuthorized(definitionName, user)) {
        //            LOGGER.info("user {} cannot view workflow {}", user.getId(), id);
        //            responseEntity = new ResponseEntity<Map<String,Object>>(new HashMap<>(), HttpStatus.FORBIDDEN);
        //        } else {
        //            counterService.increment("workflow.supervisor.access");
        //            LOGGER.warn("TODO: check authorization for workflow {} of type {} by user {}", id, definitionName, user.getId());
        //
        //            BindingSession adminSession = cmisService.getAdminSession();
        //            String url = cmisService.getBaseURL() + "service/api/workflow-instances/" + id + "?includeTasks=true";
        //            LOGGER.debug(url);
        //
        //            responseEntity = RepositoryUtils.submitRequest(url, adminSession);
        //        }
        //        return responseEntity;
    }



    // TODO returns ResponseEntity<Map<String, Object>>
    @RequestMapping(value = "variables/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public void getWorkflowVariables(HttpServletRequest req, HttpServletResponse resp, @PathVariable("id") String id)
            throws IOException {

        //        CMISUser user = cmisService.getCMISUserFromSession(req);
        //        LOGGER.debug("getWorkflowVariables user: "+ user +", id: "+id);
        //
        //        long workflowId = Utils.extractId(id);
        //
        //        try {
        //            Map<String, Object> workflowVariables = workflowService.getWorkflowVariables(user, workflowId);
        //            JSONObject res = new JSONObject(workflowVariables);
        //            PrintWriter writer = resp.getWriter();
        //            writer.write(res.toString());
        //            writer.flush();
        //            // return new ResponseEntity<Map<String,Object>>(workflowVariables, HttpStatus.OK);
        //        } catch (IllegalAccessException e) {
        //            LOGGER.info(ERRORE_PERMESSI_WORKFLOW);
        //            LOGGER.info("L'utente ha richiesto variabili per un flusso che non deve poter vedere", e);
        //            getVariablesOldMethod(req, resp, id);
        //            // return new
        //            // ResponseEntity<Map<String,Object>>(HttpStatus.FORBIDDEN);//
        //            // resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        //        } catch (WorkflowException e) {
        //            LOGGER.info("Errore durante il recupero di un flusso", e);
        //            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        //            // return new ResponseEntity<Map<String,Object>>(HttpStatus.INTERNAL_SERVER_ERROR);
        //            // resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        //        }
    }

//    @RequestMapping(value = "start/{id}",
//            method = RequestMethod.POST,
//            consumes = MediaType.APPLICATION_JSON_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE)
//    @Timed
//    public ResponseEntity<Object> startWorkflowByProcessName(HttpServletRequest req,
//            @PathVariable("id") String processId,
//            @RequestBody Map<String, Object> data) {
//
//        LOGGER.info(data.toString());
//        try {
//            validateData(data);
//            String key = "";
//            ProcessInstance instance = runtimeService.startProcessInstanceById(processId, key, data);
//            ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(instance);
//            return new ResponseEntity<Object>(response, HttpStatus.OK); // TODO verificare best practice
//        } catch (Exception e) { // TODO specifi Exception
//            LOGGER.error(e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
//        }
//    }


    private void validateData(Map<String, Object> data) {
        // TODO throw exeption if invalid
    }

    @RequestMapping(value = "deleteWorkflow", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<Map<String, Object>> delete (
            HttpServletRequest req,
            @RequestParam(value = "workflowId", required = true) long workflowId) {
        return null;
//        ResponseEntity<Map<String, Object>> response;
//        Map<String, Object> responseBody;
//        CMISUser user = cmisService.getCMISUserFromSession(req);
//
//        try {
//            if( permissionService.canDelete(user, workflowId) ) {
//                responseBody = workflowRepository.deleteWorkflow(workflowId);
//            } else {
//                throw new PermissionException("L'utente "+ user +" non ha i permessi per eliminare il flusso "+ workflowId);
//            }
//            response = new ResponseEntity<>(responseBody, HttpStatus.OK);
//        } catch (AlfrescoResponseException e) {
//            LOGGER.error(e.getMessage() + " " + e.getResponse(), e);
//            return new ResponseEntity<Map<String,Object>>(e.getResponse(), HttpStatus.INTERNAL_SERVER_ERROR);
//        } catch(PermissionException e) {
//            LOGGER.error("L'utente " + user.getFullName() + " ha provato a CANCELLARE il flusso " + workflowId + " senza essere un responsabile");
//            responseBody = new HashMap<>();
//            responseBody.put("message", "L'utente " + user.getFullName() +
//                    " non è autorizzato a cancellare il flusso perchè non è RESPONSABILE del flusso " + workflowId);
//            response = new ResponseEntity<>(responseBody, HttpStatus.FORBIDDEN);
//        } catch (IOException e) {
//            LOGGER.error("Errore nell'eliminazione del workflow " + workflowId, e);
//            Map<String, Object> error = new HashMap<>();
//            error.put("error", e.getMessage());
//            response = new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//        return response;
    }

    /* ----------- */


}
