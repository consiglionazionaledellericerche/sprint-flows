package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.SecurityUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceActionRequest;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResource;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Pubblicato;
import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;

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
    @Inject
    FlowsUserDetailsService flowsUserDetailsService;
    @Inject
    private Utils utils;


    private static Object mapVariable(HistoricProcessInstance instance, String field) {
        if (instance.getProcessVariables().get(field) == null)
            return null;
//        todo: metodo di Martin da scrivere meglio(doppio return e catch vuoti)?
        if (field.endsWith("_json")) {
            try {
                return new ObjectMapper().readValue((String) instance.getProcessVariables().get(field), List.class);
            } catch (IOException e) {
            }
            try {
                return new ObjectMapper().readValue((String) instance.getProcessVariables().get(field), Map.class);
            } catch (IOException e) {
            }
        }
        return instance.getProcessVariables().get(field);
    }

    private static List<Map<String, Object>> getDocumentiPubblicabili(HistoricProcessInstance instance) {
        List<Map<String, Object>> documentiPubblicabili = new ArrayList<>();
        for (Entry<String, Object> entry : instance.getProcessVariables().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof FlowsAttachment) {
                FlowsAttachment attachment = (FlowsAttachment) value;
                if (attachment.getStati().contains(Pubblicato)) {

                    Map<String, Object> metadatiDocumento = new HashMap<>();
                    metadatiDocumento.put("filename", attachment.getFilename());
                    metadatiDocumento.put("name", attachment.getName());
                    metadatiDocumento.put("url", "api/attachments/" + instance.getId() + "/" + key + "/data");
                    documentiPubblicabili.add(metadatiDocumento);
                }
            }
        }
        return documentiPubblicabili;
    }

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
            historicProcessInstanceQuery.variableValueEquals(Enum.VariableEnum.initiator.name(), username)
                    .unfinished()
                    .includeProcessVariables();
        } else {
            historicProcessInstanceQuery.variableValueEquals(Enum.VariableEnum.initiator.name(), username)
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


    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<Map<String, Object>> getProcessInstanceById(HttpServletRequest req, @RequestParam("processInstanceId") String processInstanceId) {
        Map<String, Object> result = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);

        return new ResponseEntity<>(result, HttpStatus.OK);
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
     * Restituisce le Process Instances attive o terminate.
     *
     * @param active boolean active
     * @return le process Instance attive o terminate
     */
    @RequestMapping(value = "/getProcessInstances", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
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

        List<HistoricProcessInstance> historicProcessInstances = historicProcessQuery.listPage(firstResult, maxResults);

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(historicProcessInstances.size());// numero di task restituiti
        response.setTotal(historicProcessQuery.count()); //numero totale di task avviati da me
        response.setData(restResponseFactory.createHistoricProcessInstanceResponseList(historicProcessInstances));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/variable", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> setVariable(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("variableName") String variableName,
            @RequestParam("value") String value) {
        runtimeService.setVariable(processInstanceId, variableName, value);
        return ResponseEntity.ok().build();
    }

    private static Map<String, Object> trasformaVariabiliPerTrasparenza(HistoricProcessInstance instance, List<String> viewExportTrasparenza) {
        Map<String, Object> mappedVariables = new HashMap<>();

        viewExportTrasparenza.stream().forEach(field -> {
            mappedVariables.put(field, mapVariable(instance, field));
        });
        mappedVariables.put("documentiPubblicabili", getDocumentiPubblicabili(instance));

        return mappedVariables;
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

        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinition)
                .startedAfter(startDate.getTime())
                .startedBefore(endDate.getTime())
                //                .finished().or().unfinished()
                .orderByProcessInstanceStartTime().asc()
                .includeProcessVariables()
                .list();

        List<String> exportTrasparenza = new ArrayList<>();
        View trasparenza = viewRepository.getViewByProcessidType(acquisti.getValue(), "export-trasparenza");
        String view = trasparenza.getView();
        JSONArray fields = new JSONArray(view);
        for (int i = 0; i < fields.length(); i++) {
            exportTrasparenza.add(fields.getString(i));
        }

        List<Map<String, Object>> mappedProcessInstances = historicProcessInstances.stream()
                .map(instance -> trasformaVariabiliPerTrasparenza(instance, exportTrasparenza))
                .collect(Collectors.toList());

        return new ResponseEntity<>(mappedProcessInstances, HttpStatus.OK);
    }

    @RequestMapping(value = "/getProcessInstancesForCigs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesForCigs(
            @RequestParam("cigs") String cigs) throws JSONException {

        List<String> cigsList = new ArrayList<String>(Arrays.asList(cigs.split(",")));

        List<String> exportTrasparenza = new ArrayList<>();

        List<HistoricProcessInstance> historicProcessInstances = null;
        List<Map<String, Object>> mappedProcessInstances = null;
        boolean mappaFlag = false;

        for (int i = 0; i < cigsList.size(); i++) {
            String currentCig = cigsList.get(i);
            historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                    .variableValueEquals("cig", currentCig)
                    .includeProcessVariables()
                    .list();


            View trasparenza = viewRepository.getViewByProcessidType(acquisti.getValue(), "export-trasparenza");
            String view = trasparenza.getView();
            JSONArray fields = new JSONArray(view);
            for (int j = 0; j < fields.length(); j++) {
                exportTrasparenza.add(fields.getString(j));
            }

            List<Map<String, Object>> mappedProcessInstancesNew = historicProcessInstances.stream()
                    .map(instance -> trasformaVariabiliPerTrasparenza(instance, exportTrasparenza))
                    .collect(Collectors.toList());

            if (!mappaFlag) {
                mappedProcessInstances = mappedProcessInstancesNew;
                mappaFlag = true;
            } else {
                mappedProcessInstances.addAll(mappedProcessInstancesNew);
            }
        }

        return new ResponseEntity<>(mappedProcessInstances, HttpStatus.OK);
    }
}