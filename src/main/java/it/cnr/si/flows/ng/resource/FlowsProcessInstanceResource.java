package it.cnr.si.flows.ng.resource;

import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceActionRequest;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResource;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.GsonFactoryBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.SecurityUtils;

@Controller
@RequestMapping("api/processInstances")
public class FlowsProcessInstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);

    @Inject
    private RestResponseFactory restResponseFactory;
    @Inject
    private HistoryService historyService;
    @Inject
    private ProcessInstanceResource processInstanceResource;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private ViewRepository viewRepository;

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
    @Secured(AuthoritiesConstants.ADMIN)
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
    @Secured(AuthoritiesConstants.ADMIN)
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
    @Timed
    public void exportCsv(
            HttpServletRequest req,
            HttpServletResponse res,
            @PathVariable("processInstanceId") String processInstanceId,
            @RequestParam("active") boolean active,
            @RequestParam("order") String order,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults) throws IOException {

        Map<String, Object> result = flowsProcessInstanceService.search(
                req, processInstanceId, active, order, firstResult, maxResults);

        flowsProcessInstanceService.buildCsv(
                (List<HistoricProcessInstanceResponse>) result.get("processInstances"),
                res.getWriter(), processInstanceId);
    }

    @RequestMapping(value = "/variable", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> setVariable(@RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("variableName") String variableName,
            @RequestParam("value") String value) {
        runtimeService.setVariable(processInstanceId, variableName, value);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/getProcessInstancesForTrasparenza", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesForTrasparenza(
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("startYear") Integer startYear,
            @RequestParam("endYear") Integer endYear) throws JSONException {

        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        startDate.clear();
        endDate.clear();
        startDate.set(startYear, 0, 0);
        endDate.set(endYear + 1, 0, 0);

        List<HistoricProcessInstance> processInstances  = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinition)
                .startedAfter(startDate.getTime())
                .startedBefore(endDate.getTime())
//                .finished().or().unfinished()
                .orderByProcessInstanceStartTime().asc()
                .includeProcessVariables()
                .list();

        List<String> exportTrasparenza = new ArrayList<>();
        View trasparenza = viewRepository.getViewByProcessidType("acquisti-trasparenza", "export-trasparenza");
        String view = trasparenza.getView();
        JSONArray fields = new JSONArray(view);
        for (int i = 0; i < fields.length(); i++) {
            exportTrasparenza.add(fields.getString(i));
        }

        List<Map<String, Object>> mappedProcessInstances = processInstances.stream()
                .map(instance -> trasformaVariabiliPerTrasparenza(instance, exportTrasparenza))
                .collect(Collectors.toList());

        return new ResponseEntity<>(mappedProcessInstances, HttpStatus.OK);
    }

    private static Map<String, Object> trasformaVariabiliPerTrasparenza(HistoricProcessInstance instance, List<String> viewExportTrasparenza) {
        Map<String, Object> variables = instance.getProcessVariables();
        Map<String, Object> mappedVariables = new HashMap<>();
        List<Map<String, Object>> documentiPubblicabili = new ArrayList<>();

        for (Entry<String, Object> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (viewExportTrasparenza.contains(key))
                mappedVariables.put(key, value);

            if (value instanceof FlowsAttachment) {
                FlowsAttachment attachment = (FlowsAttachment) value;
                if (attachment.getStati().contains(FlowsAttachment.Stato.Pubblicato)) {
                    Map<String, Object> metadatiDocumento = new HashMap<>();
                    metadatiDocumento.put("filename", attachment.getFilename());
                    metadatiDocumento.put("name", attachment.getName());
                    metadatiDocumento.put("url", "api/attachments/"+ instance.getId() +"/"+ key +"/data");
                    documentiPubblicabili.add(metadatiDocumento);
                }
            }
        }
        mappedVariables.put("documentiPubblicabili", documentiPubblicabili);

        return mappedVariables;
    }
}