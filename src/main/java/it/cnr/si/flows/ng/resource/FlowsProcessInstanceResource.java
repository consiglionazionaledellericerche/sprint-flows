package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.security.SecurityUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.impl.ActivitiProcessCancelledEventImpl;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceActionRequest;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResource;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.Stato.PubblicatoTrasparenza;
import static it.cnr.si.flows.ng.utils.Utils.DESC;
import static it.cnr.si.flows.ng.utils.Utils.DESCRIZIONE;
import static it.cnr.si.flows.ng.utils.Utils.INITIATOR;
import static it.cnr.si.flows.ng.utils.Utils.TITOLO;

@Controller
@RequestMapping("api/processInstances")
public class FlowsProcessInstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
    public static final String EXPORT_TRASPARENZA = "export-trasparenza";
	public static final String STATO_FINALE_DOMANDA = "statoFinaleDomanda";

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
    private FlowsUserDetailsService flowsUserDetailsService;
    @Inject
    private PermissionEvaluatorImpl permissionEvaluator;
    @Inject
    private Utils utils;




    /**
     * Restituisce le Process Instances attive o terminate.
     *
     * @param active boolean active
     * @return le process Instance attive o terminate
     */
    @PostMapping(value = "/getProcessInstances", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity getProcessInstances(
            HttpServletRequest req,
            @RequestParam("active") boolean active,
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam("order") String order) {
        HistoricProcessInstanceQuery historicProcessQuery = flowsProcessInstanceService.getProcessInstances(req, active, processDefinition, order);

        List<HistoricProcessInstance> historicProcessInstances = historicProcessQuery.listPage(firstResult, maxResults);

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(historicProcessInstances.size());// numero di task restituiti
        response.setTotal(historicProcessQuery.count()); //numero totale di task avviati da me
        response.setData(restResponseFactory.createHistoricProcessInstanceResponseList(historicProcessInstances));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    /**
     * Restituisce le Processs Instances avviate dall'utente loggato
     *
     * @param firstResult          il primo elemento da restituire
     * @param maxResults           l`ultimo elemento da restituire
     * @param order                l`ordine di presentazione dei risultati (DESC/ASC)
     * @param active               provessi attivi/terminati
     * @param processDefinitionKey the process definition key
     * @param params               i paramnetri della ricerca
     * @return the my processes
     */
    @PostMapping(value = "/myProcessInstances")
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity getMyProcessInstances(
            @PathParam("firstResult") int firstResult,
            @PathParam("maxResults") int maxResults,
            @PathParam("order") String order,
            @PathParam("active") boolean active,
            @PathParam("processDefinitionKey") String processDefinitionKey,
            @RequestBody Map<String, String> params) {

        params.put("initiator", SecurityUtils.getCurrentUserLogin());
        DataResponse response = flowsProcessInstanceService.search(params, processDefinitionKey, active, order, firstResult, maxResults, true);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    // TODO questo metodo restituisce ResponseEntity di due tipi diversi - HistoricProcessInstance e Map<String, Object>
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity getProcessInstanceById(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam(value = "detail", required = false, defaultValue = "true") Boolean detail) {
        if (!detail) {
            return new ResponseEntity(flowsProcessInstanceService.getProcessInstance(processInstanceId), HttpStatus.OK);
        } else {
            return new ResponseEntity(flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId), HttpStatus.OK);
        }
    }



    @GetMapping(value = "/currentTask", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<HistoricTaskInstance> getCurrentTaskProcessInstanceById(@RequestParam("processInstanceId") String processInstanceId) {
        HistoricTaskInstance result = flowsProcessInstanceService.getCurrentTaskOfProcessInstance(processInstanceId);

        return new ResponseEntity(result, HttpStatus.OK);
    }



    @DeleteMapping(value = "deleteProcessInstance", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity delete(
            @RequestParam(value = "processInstanceId", required = true) String processInstanceId,
            @RequestParam(value = "deleteReason", required = true) String deleteReason) {

        runtimeService.setVariable(processInstanceId, STATO_FINALE_DOMANDA, "ELIMINATO");
        runtimeService.setVariable(processInstanceId, "motivazioneEliminazione", deleteReason);
		flowsProcessInstanceService.updateSearchTerms(flowsProcessInstanceService.getCurrentTaskOfProcessInstance(processInstanceId).getExecutionId(), processInstanceId, "ELIMINATO");

		runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
        return new ResponseEntity(HttpStatus.OK);
    }

    // TODO ???
    @DeleteMapping(value = "suspendProcessInstance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.isResponsabile(#taskId, #processInstanceId, @flowsUserDetailsService)")
    @Timed
    public ProcessInstanceResponse suspend(
            HttpServletRequest request,
            @RequestParam(value = "processInstanceId", required = true) String processInstanceId) {
        ProcessInstanceActionRequest action = new ProcessInstanceActionRequest();
        action.setAction(ProcessInstanceActionRequest.ACTION_SUSPEND);
        return processInstanceResource.performProcessInstanceAction(processInstanceId, action, request);
    }



    @PostMapping(value = "/variable", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> setVariable(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("variableName") String variableName,
            @RequestParam("value") String value) {
        runtimeService.setVariable(processInstanceId, variableName, value);
        return ResponseEntity.ok().build();
    }



    /**
     * Restituisce l'istanza della variabile della Process Instance
     *
     * @param processInstanceId il process instance id della ProcessInstance di cui si vuole "recuperare la variabile
     * @param variableName      il nome della variable
     * @return la variableInstance
     */
    @GetMapping(value = "/variable", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity getVariable(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("variableName") String variableName) {

        return new ResponseEntity<>(runtimeService.getVariableInstance(processInstanceId, variableName), HttpStatus.OK);
    }



    /**
     * Gets process instances for trasparenza.
     *
     * @param processDefinition la process definition (es; "acquisti")
     * @param startYear         anno di inizio dell`intervallo temporale
     * @param endYear           anno di fine dell`intervallo temporale
     * @param firstResult       il primo risultato che si vuole recuperare
     * @param maxResults        il numero (massimo) di risultati che si vuole recuperare
     * @param order             l`ordine (ASC o DESC) in base alla data di start del flusso (non richiesto, può anche essere nullo)
     * @return le process instances da esportare in trasparenza
     * @throws ParseException the parse exception
     */
    @PostMapping(value = "/getProcessInstancesForTrasparenza", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesForTrasparenza(
            @RequestParam("processDefinition") String processDefinition,
            @RequestParam("startYear") int startYear,
            @RequestParam("endYear") int endYear,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam(name = "order", required = false) String order) throws ParseException {

        DateFormat formatoData = new SimpleDateFormat("dd-MM-yyyy");
        List<HistoricProcessInstance> historicProcessInstances;

        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinition)
                .startedAfter(formatoData.parse("01-01-" + startYear))
                .startedBefore(formatoData.parse("31-12-" + endYear))
                .includeProcessVariables();
        if(order == DESC){
            historicProcessInstances = historicProcessInstanceQuery
                    .orderByProcessInstanceStartTime().desc()
                    .listPage(firstResult, maxResults);
        } else {
//        	default
            historicProcessInstances = historicProcessInstanceQuery
                    .orderByProcessInstanceStartTime().asc()
                    .listPage(firstResult, maxResults);
        }

        List<String> exportTrasparenza = new ArrayList<>();
        View trasparenza = viewRepository.getViewByProcessidType(acquisti.getValue(), EXPORT_TRASPARENZA);
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



    @PostMapping(value = "/getProcessInstancesForCigs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesForCigs(
            @RequestParam("cigs") String cigs) {

        List<String> cigsList = new ArrayList(Arrays.asList(cigs.split(",")));

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


            View trasparenza = viewRepository.getViewByProcessidType(acquisti.getValue(), EXPORT_TRASPARENZA);
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



    @PostMapping(value = "/getProcessInstancesbyProcessInstanceIds", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @Timed
    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesbyProcessInstanceIds(
            @RequestParam("processInstanceIds") String processInstanceIds) {

        Set<String> processInstanceIdsList = new HashSet(Arrays.asList(processInstanceIds.split(",")));

        List<String> exportTrasparenza = new ArrayList<>();

        List<HistoricProcessInstance> historicProcessInstances = null;
        List<Map<String, Object>> mappedProcessInstances = null;

        historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(processInstanceIdsList)
                .includeProcessVariables()
                .list();


        View trasparenza = viewRepository.getViewByProcessidType(acquisti.getValue(), EXPORT_TRASPARENZA);
        String view = trasparenza.getView();
        JSONArray fields = new JSONArray(view);
        for (int j = 0; j < fields.length(); j++) {
            exportTrasparenza.add(fields.getString(j));
        }

        mappedProcessInstances = historicProcessInstances.stream()
                .map(instance -> trasformaVariabiliPerTrasparenza(instance, exportTrasparenza))
                .collect(Collectors.toList());

        return new ResponseEntity<>(mappedProcessInstances, HttpStatus.OK);
    }



    @PostMapping(value = "/identityLinks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> setIdentityLink(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("identityLinkType") String identityLinkType,
            @RequestParam(value = "groupId", required = false) String groupId,
            @RequestParam(value = "userId", required = false) String userId) {

        if (groupId != null && !groupId.isEmpty()) {
            LOGGER.info("Aggiunta IdentityLink - Pi: {}, groupId: {}, type: {}", processInstanceId, groupId, identityLinkType);
            runtimeService.addGroupIdentityLink(processInstanceId, groupId, identityLinkType);
        } else {
            if (userId != null && !userId.isEmpty()){
                LOGGER.info("Aggiunta IdentityLink - Pi: {}, userId: {}, type: {}", processInstanceId, userId, identityLinkType);
                runtimeService.addUserIdentityLink(processInstanceId,userId,identityLinkType);
            }
        }
        return ResponseEntity.ok().build();
    }



    @DeleteMapping(value = "/identityLinks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteIdentityLink(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("identityLinkType") String identityLinkType,
            @RequestParam(value = "groupId", required=false) String groupId,
            @RequestParam(value = "userId", required=false) String userId) {

        if(groupId != null && !groupId.isEmpty()) {
            LOGGER.info("Cancellazione IdentityLink - Pi: {}, groupId: {}, type: {}", processInstanceId, groupId, identityLinkType);
            runtimeService.deleteGroupIdentityLink(processInstanceId, groupId, identityLinkType);
        }else{
            if(userId != null && !userId.isEmpty()) {
                LOGGER.info("Cancellazione IdentityLink - Pi: {}, userId: {}, type: {}", processInstanceId, userId, identityLinkType);
                runtimeService.deleteUserIdentityLink(processInstanceId, userId, identityLinkType);
            }
        }
        return ResponseEntity.ok().build();
    }



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
                if (attachment.getStati().contains(PubblicatoTrasparenza)) {

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

    private static Map<String, Object> trasformaVariabiliPerTrasparenza(HistoricProcessInstance instance, List<String> viewExportTrasparenza) {
        Map<String, Object> mappedVariables = new HashMap<>();

        viewExportTrasparenza.stream().forEach(field -> {
            mappedVariables.put(field, mapVariable(instance, field));
        });
        mappedVariables.put("documentiPubblicabili", getDocumentiPubblicabili(instance));

        return mappedVariables;
    }
}