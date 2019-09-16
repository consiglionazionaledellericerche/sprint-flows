package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.security.SecurityUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
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
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.Stato.PubblicatoTrasparenza;
import static it.cnr.si.flows.ng.utils.Enum.Stato.PubblicatoUrp;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;

@RestController
@RequestMapping("api/processInstances")
public class FlowsProcessInstanceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
    public static final String EXPORT_TRASPARENZA = "export-trasparenza";
    public static final String EXPORT_URP = "export-urp";


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
    private UserDetailsService flowsUserDetailsService;
    @Inject
    private PermissionEvaluatorImpl permissionEvaluator;
    @Inject
    private Utils utils;
    @Inject
    private Environment env;



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

        runtimeService.setVariable(processInstanceId, statoFinaleDomanda.name(), "ELIMINATO");
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
    public ResponseEntity<HistoricVariableInstance> getVariable(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("variableName") String variableName) {

        return new ResponseEntity<HistoricVariableInstance>(
                historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .variableName(variableName)
                        .list()
                        .stream()
                        .sorted((a, b) -> b.getLastUpdatedTime().compareTo(a.getLastUpdatedTime()) )
                        .findFirst().orElse(null),
                HttpStatus.OK);
    }


    /**
     * Gets process instances for trasparenza.
     *
     * @param firstResult       il primo risultato che si vuole recuperare
     * @param maxResults        il numero (massimo) di risultati che si vuole recuperare
     * @param order             l`ordine (ASC o DESC) in base alla data di start del flusso (non richiesto, può anche essere nullo)
     * @return le process instances da esportare in trasparenza
     */
    @PostMapping(value = "/getProcessInstancesForTrasparenza", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @PreAuthorize("hasRole('ROLE_applicazione-portalecnr@0000')")
    @Timed
    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesForTrasparenza(
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam(name = "order", required = false) String order) throws ParseException {

        List<HistoricProcessInstance> historicProcessInstances =
                flowsProcessInstanceService.getProcessInstancesForTrasparenza(firstResult, maxResults, order);

        return new ResponseEntity<>(getMappedPI("acquisti", historicProcessInstances, EXPORT_TRASPARENZA), HttpStatus.OK);
    }


    @PostMapping(value = "/getProcessInstancesForURP", produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    @PreAuthorize("hasRole('ROLE_applicazione-portalecnr@0000')")
    @Timed
    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesForURP(
            @RequestParam("terminiRicorso") int terminiRicorso,
            @RequestParam(name = "avvisiScaduti", required = false) Boolean avvisiScaduti,
            @RequestParam(name = "gareScadute", required = false) Boolean gareScadute,
            @RequestParam("firstResult") int firstResult,
            @RequestParam("maxResults") int maxResults,
            @RequestParam(name = "order", required = false) String order) {

        List<HistoricProcessInstance> historicProcessInstances =
                flowsProcessInstanceService.getProcessInstancesForURP(terminiRicorso, avvisiScaduti, gareScadute, firstResult, maxResults, order);

        return new ResponseEntity<>(getMappedPI("acquisti", historicProcessInstances, EXPORT_URP), HttpStatus.OK);
    }



//    @PostMapping(value = "/getProcessInstancesForCigs", produces = MediaType.APPLICATION_JSON_VALUE)
//    @Secured(AuthoritiesConstants.ADMIN)
//    @PreAuthorize("hasRole('ROLE_applicazione-portalecnr@0000')")
//    @Timed
//    public ResponseEntity<List<Map<String, Object>>> getProcessInstancesForCigs(
//            @RequestParam("cigs") String cigs) {
//
//        List<String> cigsList = new ArrayList(Arrays.asList(cigs.split(",")));
//
//        List<String> exportTrasparenza = new ArrayList<>();
//
//        List<HistoricProcessInstance> historicProcessInstances = null;
//        List<Map<String, Object>> mappedProcessInstances = null;
//        boolean mappaFlag = false;
//
//        for (int i = 0; i < cigsList.size(); i++) {
//            String currentCig = cigsList.get(i);
//            historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
//                    .variableValueEquals("cig", currentCig)
//                    .includeProcessVariables()
//                    .list();
//
//
//            View trasparenza = viewRepository.getViewByProcessidType(acquisti.getValue(), EXPORT_TRASPARENZA);
//            String view = trasparenza.getView();
//            JSONArray fields = new JSONArray(view);
//            for (int j = 0; j < fields.length(); j++) {
//                exportTrasparenza.add(fields.getString(j));
//            }
//
//            List<Map<String, Object>> mappedProcessInstancesNew = historicProcessInstances.stream()
//                    .map(instance -> trasformaVariabiliPerTrasparenza(instance, exportTrasparenza))
//                    .collect(Collectors.toList());
//
//            if (!mappaFlag) {
//                mappedProcessInstances = mappedProcessInstancesNew;
//                mappaFlag = true;
//            } else {
//                mappedProcessInstances.addAll(mappedProcessInstancesNew);
//            }
//        }
//
//        return new ResponseEntity<>(mappedProcessInstances, HttpStatus.OK);
//    }



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


    private List<Map<String, Object>> getMappedPI(String processDefinition, List<HistoricProcessInstance> historicProcessInstances, String typeView) {
        String view = viewRepository.getViewByProcessidType(processDefinition, typeView).getView();
        JSONArray jsonFieldsToExport = new JSONArray(view);

        List<Map<String, Object>> response = null;

        if(typeView != null && typeView.equals(EXPORT_URP)){
            response =historicProcessInstances.stream()
                    .map(instance -> trasformaVariabili(instance, jsonFieldsToExport, false))
                    .collect(Collectors.toList());
        } else if(typeView != null && typeView.equals(EXPORT_TRASPARENZA)){
            response = historicProcessInstances.stream()
                    .map(instance -> trasformaVariabili(instance, jsonFieldsToExport, true))
                    .collect(Collectors.toList());
        }
        return response;
    }


    private Object mapVariable(HistoricProcessInstance instance, String field) {
        Object ret = null;
        switch (field) {
            case "stato":
                ret = new JSONObject(instance.getName()).getString("stato");
                break;
            case "terminata":
                if (instance.getEndTime() != null)
                    ret = true;
                else
                    ret = false;
                break;
            case "impegni_json":
                try {
                    if (instance.getProcessVariables().get(field) != null)
                        ret = new ObjectMapper().readValue((String) instance.getProcessVariables().get(field), List.class);
                } catch (IOException e) {
                    LOGGER.error("Errore nel mapping delle variabili di tipo \"impegni_json\"", e);
                }
                break;
            default:
                if (instance.getProcessVariables().get(field) != null)
                    ret = instance.getProcessVariables().get(field);
                break;
        }
        return ret;
    }


    private  List<Map<String, Object>> getDocumentiPubblicabiliTrasparenza(HistoricProcessInstance instance) {
        List<Map<String, Object>> documentiPubblicabili = new ArrayList<>();
        for (Entry<String, Object> entry : instance.getProcessVariables().entrySet()) {
            Object value = entry.getValue();

            if (value instanceof FlowsAttachment) {
                FlowsAttachment attachment = (FlowsAttachment) value;
                if (attachment.getStati().contains(PubblicatoTrasparenza)) {

                    Map<String, Object> metadatiDocumento = new HashMap<>();
                    metadatiDocumento.put("filename", attachment.getFilename());
                    metadatiDocumento.put("name", attachment.getName());
                    metadatiDocumento.put("label", attachment.getLabel());
                    metadatiDocumento.put("key", attachment.getUrl());
                    metadatiDocumento.put("path", attachment.getPath());
                    metadatiDocumento.put("download", env.getProperty("repository.base.url") + "d/a/workspace/SpacesStore/" + attachment.getUrl().split(";")[0] + "/" + attachment.getName());
                    documentiPubblicabili.add(metadatiDocumento);
                }
            }
        }
        return documentiPubblicabili;
    }


    private List<Map<String, Object>> getDocumentiPubblicabiliURP(HistoricProcessInstance instance) {
        List<Map<String, Object>> documentiPubblicabili = new ArrayList<>();
        for (Entry<String, Object> entry : instance.getProcessVariables().entrySet()) {
            Object value = entry.getValue();

            if (value instanceof FlowsAttachment) {
                FlowsAttachment attachment = (FlowsAttachment) value;
                if (attachment.getStati().contains(PubblicatoUrp)) {

                    Map<String, Object> metadatiDocumento = new HashMap<>();
                    metadatiDocumento.put("filename", attachment.getFilename());
                    metadatiDocumento.put("name", attachment.getName());
                    metadatiDocumento.put("label", attachment.getLabel());
                    metadatiDocumento.put("key", attachment.getUrl());
                    metadatiDocumento.put("path", attachment.getPath());
                    metadatiDocumento.put("download", env.getProperty("repository.base.url") + "d/a/workspace/SpacesStore/" + attachment.getUrl().split(";")[0] + "/" + attachment.getName());
                    // recupero la data di pubblicvazione in URP
                    Optional<HistoricDetailVariableInstanceUpdateEntity> dataPubblicazione = historyService.createHistoricDetailQuery()
                            .processInstanceId(instance.getId())
                            .variableUpdates()
                            .orderByVariableRevision()
                            .excludeTaskDetails()
                            .asc()
                            .list()
                            .stream()
                            .map(h -> (HistoricDetailVariableInstanceUpdateEntity) h)
                            .filter(h -> h.getName().equals(attachment.getName()) &&
                                    ((FlowsAttachment) h.getValue()).getMetadati().containsKey("azione") &&
                                    ((Enum.Azione)((FlowsAttachment) h.getValue()).getMetadati().get("azione")).name() == (Enum.Azione.PubblicazioneUrp.name()))
                            .findAny();

                    if(dataPubblicazione.isPresent())
                        metadatiDocumento.put("dataPubblicazione", dataPubblicazione.get().getTime());

                    documentiPubblicabili.add(metadatiDocumento);
                }
            }
        }
        return documentiPubblicabili;
    }


    private Map<String, Object> trasformaVariabili(HistoricProcessInstance instance, JSONArray viewExport, boolean isTrasparenza) {
        Map<String, Object> mappedVariables = new HashMap<>();
        viewExport.forEach(field -> {
            Object value = mapVariable(instance, field.toString());
            if(value != null){
                mappedVariables.put(field.toString(), value.toString());
            }
        });

        if(isTrasparenza)
            mappedVariables.put("documentiPubblicabiliInTrasparenza", getDocumentiPubblicabiliTrasparenza(instance));
        else
            mappedVariables.put("documentiPubblicabiliInURP", getDocumentiPubblicabiliURP(instance));

        return mappedVariables;
    }
}