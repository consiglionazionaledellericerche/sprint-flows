package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;

@Controller
@RequestMapping("api/processInstances")
public class FlowsProcessInstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
    @Autowired
    private RestResponseFactory restResponseFactory;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ProcessInstanceResource processInstanceResource;
    @Autowired
    private FlowsAttachmentResource attachmentResource;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private TaskService taskService;
    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    private Utils utils = new Utils();





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
            @RequestParam boolean active,
            @RequestParam String processDefinition,
            @RequestParam String order,
            @RequestParam int firstResult,
            @RequestParam int maxResults) {

        String username = SecurityUtils.getCurrentUserLogin();
        List<HistoricProcessInstance> list;
        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

        if (active) {
            historicProcessInstanceQuery.variableValueEquals("initiator", username)
                    .unfinished()
                    .includeProcessVariables();
        } else {
            historicProcessInstanceQuery.variableValueEquals("initiator", username)
                    .finished()
                    .includeProcessVariables();
        }

        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            historicProcessInstanceQuery.processDefinitionKey(processDefinition);
        if (order.equals(ASC))
            historicProcessInstanceQuery.orderByProcessInstanceStartTime().asc();
        else
            historicProcessInstanceQuery.orderByProcessInstanceStartTime().desc();

        list = historicProcessInstanceQuery.listPage(firstResult, maxResults);

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(list.size()); //numero flussi restituito
        response.setTotal(historicProcessInstanceQuery.count()); //totale Flussi
        response.setData(restResponseFactory.createHistoricProcessInstanceResponseList(list));
        response.setOrder(order);

        return ResponseEntity.ok(response);
    }

    // TODO refactor in path param
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> getProcessInstanceById(@RequestParam("processInstanceId") String processInstanceId) {
        Map<String, Object> result = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    /**
     * Restituisce le Process Instances attive o terminate.
     *
     * @param active boolean active
     * @return le process Instance attive o terminate
     */
    @RequestMapping(value = "/getProcessInstances", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured({AuthoritiesConstants.ADMIN})
    @Timed
    public ResponseEntity getProcessInstances(
            HttpServletRequest req,
            @RequestParam("active") boolean active,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {
        HistoricProcessInstanceQuery historicProcessQuery = historyService.createHistoricProcessInstanceQuery().includeProcessVariables();

        historicProcessQuery = utils.orderProcess(order, historicProcessQuery);

        historicProcessQuery = (HistoricProcessInstanceQuery) utils.searchParamsForProcess(req, historicProcessQuery);
        if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
            historicProcessQuery.processDefinitionKey(processDefinition);

        if (active) {
            historicProcessQuery.unfinished();
        } else {
            historicProcessQuery.finished().or().deleted();
        }

        List<HistoricProcessInstance> processInstances = historicProcessQuery.listPage(firstResult, maxResults);

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(processInstances.size());// numero di task restituiti
        response.setTotal(historicProcessQuery.count()); //numero totale di task avviati da me
        response.setData(restResponseFactory.createHistoricProcessInstanceResponseList(processInstances));

        return new ResponseEntity<>(response, HttpStatus.OK);
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

    /**
     * Funzionalità di Ricerca delle Process Instances.
     *
     * @param req               the req
     * @param processInstanceId Il processInstanceId della ricerca
     * @param active            Boolean che indica se ricercare le Process Instances attive o terminate
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

        Map<String, Object> result = flowsProcessInstanceService.search(req, processInstanceId, active, order, firstResult, maxResults);
        return ResponseEntity.ok(result);
    }
}
