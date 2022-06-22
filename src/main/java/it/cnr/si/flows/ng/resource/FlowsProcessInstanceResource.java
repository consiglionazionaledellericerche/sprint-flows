package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.PermissionEvaluatorImpl;

import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.SecurityService;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
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
import javax.websocket.server.PathParam;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
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
    private RepositoryService repositoryService;
	@Inject
	private RestResponseFactory restResponseFactory;
	@Inject
	private HistoryService historyService;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private ViewRepository viewRepository;
	@Inject
	private UserDetailsService flowsUserDetailsService;
	@Inject
	private FlowsTaskService flowsTaskService;
	@Inject
	private PermissionEvaluatorImpl permissionEvaluator;
	@Inject
	private Utils utils;
	@Inject
	private MembershipService membershipService;
	@Inject
	private Environment env;
	@Inject
	private SecurityService securityService;



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
	public ResponseEntity<DataResponse> getMyProcessInstances(
			@PathParam("firstResult") int firstResult,
			@PathParam("maxResults") int maxResults,
			@PathParam("order") String order,
			@PathParam("active") boolean active,
			@PathParam("processDefinitionKey") String processDefinitionKey,
			@RequestBody Map<String, String> params) {

		params.put("initiator", securityService.getCurrentUserLogin());
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
		if (detail) {
			return new ResponseEntity<>(flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId, true), HttpStatus.OK);
		} else {
			return new ResponseEntity<>(flowsProcessInstanceService.getProcessInstance(processInstanceId), HttpStatus.OK);
		}
	}



	@GetMapping(value = "/currentTask", produces = MediaType.APPLICATION_JSON_VALUE)
	@Secured(AuthoritiesConstants.USER)
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
	@Timed
	public ResponseEntity<HistoricTaskInstance> getCurrentTaskProcessInstanceById(@RequestParam("processInstanceId") String processInstanceId) {
		HistoricTaskInstance result = flowsProcessInstanceService.getCurrentTaskOfProcessInstance(processInstanceId);

		return new ResponseEntity<>(result, HttpStatus.OK);
	}



	@DeleteMapping(value = "deleteProcessInstance", produces = MediaType.APPLICATION_JSON_VALUE)
	@Secured(AuthoritiesConstants.ADMIN)
	@Timed
	public ResponseEntity<String> delete(
			@RequestParam(value = "processInstanceId", required = true) String processInstanceId,
			@RequestParam(value = "deleteReason", required = true) String deleteReason) {

		runtimeService.setVariable(processInstanceId, statoFinaleDomanda.name(), "ELIMINATO");
		runtimeService.setVariable(processInstanceId, "motivazioneEliminazione", deleteReason);
		try {
			utils.updateJsonSearchTerms(flowsProcessInstanceService.getCurrentTaskOfProcessInstance(processInstanceId).getExecutionId(), processInstanceId, "ELIMINATO");
		} catch(RuntimeException  error1) {
			return new ResponseEntity<>("Errore nell`aggiornamento di stato, DESCRIZIONE, TITOLO, INITIATOR nel \"name\" della PI", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping(value = "eraseFinishedProcessInstance", produces = MediaType.APPLICATION_JSON_VALUE)
	@Secured(AuthoritiesConstants.ADMIN)
	@Timed
	public ResponseEntity<String> erase(@RequestParam(value = "processInstanceId", required = true) String processInstanceId) {
		historyService.deleteHistoricProcessInstance(processInstanceId);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	// TODO martin
	// TODO questo metodo implmentato apparentemente senza motivo
	// TODO oggi, 05/02/2020 lo commento
	// TODO se entro 05/05/2020 non gli abbiamo trovato un uso, eliminarlo
	//	@DeleteMapping(value = "suspendProcessInstance", produces = MediaType.APPLICATION_JSON_VALUE)
	//	@PreAuthorize("hasRole('ROLE_ADMIN') || @permissionEvaluator.isResponsabile(#taskId, #processInstanceId, @flowsUserDetailsService)")
	//	@Timed
	//	public ProcessInstanceResponse suspend(
	//			HttpServletRequest request,
	//			@RequestParam(value = "processInstanceId", required = true) String processInstanceId) {
	//
	//		runtimeService.suspendProcessInstanceById(processInstanceId);
	//		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).includeProcessVariables().singleResult();
	//		ProcessInstanceResponse response =  restResponseFactory.createProcessInstanceResponse(processInstance);
	//		return response;
	//	}

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

		return new ResponseEntity<>(
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
	@GetMapping(value = "/getProcessInstancesForTrasparenza", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('applicazione-portalecnr@0000','ROLE_ADMIN')")
	@Timed
	public ResponseEntity<Map<String, Object>> getProcessInstancesForTrasparenza(
			@RequestParam("firstResult") int firstResult,
			@RequestParam("maxResults") int maxResults,
			@RequestParam(name = "searchField", required = false) String searchField,
			@RequestParam(name = "order", required = false) String order) {

		HistoricProcessInstanceQuery query = flowsProcessInstanceService.getProcessInstancesForTrasparenza(order, searchField);
		//popolo la listPi con tutti i campi associati alla view "export-trasparenza"
		HashMap<String, Object> responseMap = new HashMap<>();

		responseMap.put("data", mappingPI(acquisti, query.listPage(firstResult, maxResults), EXPORT_TRASPARENZA, false));

		//numero totale di Pi della query (per la paginazione dal lato del portale del CNR)
		responseMap.put("totalNumItems", query.count());

		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}


	/**
	 * Gets process instances for trasparenza.
	 *
	 * @param processInstanceId       Process Instance Id
	 * @return le process instances da esportare in trasparenza
	 */
	@GetMapping(value = "/getProcessInstanceForPartaleCnrById", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('applicazione-portalecnr@0000','ROLE_ADMIN')")
	@Timed
	public ResponseEntity<Map<String, Object>> getProcessInstanceForPartaleCnrById(
			@RequestParam("processInstanceId") String processInstanceId,
			@RequestParam("service") String service) {

		HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery()
				.includeProcessVariables().processInstanceId(processInstanceId);
		//popolo la listPi con tutti i campi associati alla view "export-trasparenza"
		HashMap<String, Object> responseMap = new HashMap<>();

		if(service.equals(EXPORT_TRASPARENZA.split("-")[1])) {
			responseMap.put("data", mappingPI(acquisti, query.list(), EXPORT_TRASPARENZA, true));
		} else if(service.equals(EXPORT_URP.split("-")[1])){
			responseMap.put("data", mappingPI(acquisti, query.list(), EXPORT_URP, true));
		}
		return new ResponseEntity<>(responseMap, HttpStatus.OK);
	}


	@GetMapping(value = "/getProcessInstancesForURP", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyRole('ROLE_applicazione-portalecnr@0000','ROLE_ADMIN')")
	@Timed
	public ResponseEntity<Map<String, Object>> getProcessInstancesForURP(
			@RequestParam("terminiRicorso") int terminiRicorso,
			@RequestParam(name = "avvisiScaduti", required = false) Boolean avvisiScaduti,
			@RequestParam(name = "gareScadute", required = false) Boolean gareScadute,
			@RequestParam("firstResult") int firstResult,
			@RequestParam("maxResults") int maxResults,
			@RequestParam(name = "order", required = false) String order) {

		HistoricProcessInstanceQuery query = flowsProcessInstanceService.getProcessInstancesForURP(terminiRicorso, avvisiScaduti, gareScadute, order);
		//popolo la listPi con tutti i campi associati alla view "export-trasparenza"
		HashMap<String, Object> responseMap = new HashMap<>();
		responseMap.put("data", mappingPI(acquisti, query.listPage(firstResult, maxResults), EXPORT_TRASPARENZA, false));

		//numero totale di Pi della query (per la paginazione dal lato del portale del CNR)
		responseMap.put("totalNumItems", query.count());

		return new ResponseEntity<>(responseMap, HttpStatus.OK);
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


	@GetMapping(value = "/getHistoryForPi", produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<List<Map<String, Object>>> getHistoryForPi(
			@RequestParam("processInstanceId") String processInstanceId){

		ArrayList<Map<String, Object>> historicProcessInstances = flowsProcessInstanceService
				.getHistoryForPi(processInstanceId);
		return new ResponseEntity<>(historicProcessInstances, HttpStatus.OK);
	}

	/**
	 * Deve esserci una process definition definita in 
	 * FlowsProcessInstanceService.processiRevocabili
	 * agganciata alla process definition del flusso che si sta revocando
	 * (per esempio "smart-working-domanda"-"smart-working-revoca")
	 * 
	 * Inserire tutte le variabili necessarie al nuovo flusso nella mappa `data`
	 * 
	 */
	@PostMapping(value = "/revoca", produces = MediaType.APPLICATION_JSON_VALUE)
	@Timed
	public ResponseEntity<Void> revoca(
			@RequestParam("processInstanceId") String processInstanceId){

		HistoricProcessInstance oldProcessInstance =  historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
		
		
		if (oldProcessInstance == null)
			throw new IllegalArgumentException("Il processo non e' stato trovato");
		if (!flowsProcessInstanceService.isRevocabile(processInstanceId))
			throw new IllegalArgumentException("Il processo non e' recovabile");

		Map<String, Object> data = new HashMap<>();
		String definitionKey = FlowsProcessInstanceService.processiRevocabili.get(oldProcessInstance.getProcessDefinitionKey());
		String definitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(definitionKey).latestVersion().singleResult().getId();
		data.put("processDefinitionId", definitionId);

		//MAPPATURA VARIABILI
		String idStruttura = oldProcessInstance.getProcessVariables().get("idStruttura").toString();
		data.put("idNsipRichiedente", oldProcessInstance.getProcessVariables().get("idNsipRichiedente"));
		data.put("idAceStrutturaDomandaRichiedente", oldProcessInstance.getProcessVariables().get("idAceStrutturaDomandaRichiedente"));
		data.put("userNameDomanda", oldProcessInstance.getProcessVariables().get("userNameProponente"));
		data.put("idDomanda", oldProcessInstance.getProcessVariables().get("idDomanda"));
		data.put("idStruttura", idStruttura);
		String currentUser = securityService.getCurrentUserLogin();
		String idAceStrutturaDomandaRichiedente = oldProcessInstance.getProcessVariables().get("idAceStrutturaDomandaRichiedente").toString();

		Set<String> ruoliCurrentUser = membershipService.getAllRolesForUser(currentUser); 

		// SE LA RICHIESTA VIENE DA UN DIRETTORE DEVE ESSERE PRESA IN VIASIONE DALLA SEGRETERIA
		if (ruoliCurrentUser.contains("responsabile-struttura@" + idAceStrutturaDomandaRichiedente)) {
			data.put("tipologiaRichiedente", "direttore-responsabile");
		} else {
			// SE LA RICHIESTA NON VIENE DA UN DIRETTORE (SEGRETERIA O APP.SIPER DEVE ESSERE PRESA IN VIASIONE DAL DIRETTORE
			data.put("tipologiaRichiedente", "segreteria");
		}
		flowsTaskService.startProcessInstance(definitionId, data);

		return new ResponseEntity<>(HttpStatus.OK);
	}	


	private List<Map<String, Object>> mappingPI(Enum.ProcessDefinitionEnum processDefinition, List<HistoricProcessInstance> historicProcessInstances, String typeView, boolean includeDocs) {
		String view = viewRepository.getViewByProcessidType(processDefinition.getProcessDefinition(), typeView).getView();
		JSONArray jsonFieldsToExport = new JSONArray(view);

		List<Map<String, Object>> response = new ArrayList<>();

		if(typeView != null) {
			response = historicProcessInstances.stream()
					.map(instance -> trasformaVariabili(instance, jsonFieldsToExport, typeView.equals(EXPORT_TRASPARENZA), includeDocs))
					.collect(Collectors.toList());
		}
		return response;
	}


	private Map<String, Object> trasformaVariabili(HistoricProcessInstance instance, JSONArray viewExport, boolean isTrasparenza, boolean includeDocs) {
		HashMap<String, Object> mappedVariables = new HashMap<>();

		if(includeDocs) {
			if (isTrasparenza)
				mappedVariables.put("documentiPubblicabiliInTrasparenza", getDocumentiPubblicabiliTrasparenza(instance));
			else
				mappedVariables.put("documentiPubblicabiliInURP",
						getDocumentiPubblicabiliURP(instance));
		}

		viewExport.forEach(field -> {
			Object variable = instance.getProcessVariables().get(field);
			switch (field.toString()) {
			case "businessKey":
				mappedVariables.put(field.toString(), instance.getBusinessKey());
				break;
			case "stato":
				mappedVariables.put(field.toString(), new JSONObject(instance.getName()).getString("stato"));
				break;
			case "terminata":
				mappedVariables.put(field.toString(), (instance.getEndTime() != null));
				break;
			case "impegni_json":
				try {
					mappedVariables.put(field.toString(), new ObjectMapper().readValue(variable != null ? (String) variable : "", List.class));
				} catch (IOException e) {
					LOGGER.error("Errore nel mapping delle variabili di tipo \"impegni_json\"", e);
				}
				break;
			default:
				mappedVariables.put(field.toString(), variable != null ? variable.toString():"");
				break;
			}
		});

		return mappedVariables;
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
					metadatiDocumento.put("download", env.getProperty("cnr.doc-cnr.url") + "rest/content?nodeRef=" + attachment.getUrl());
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
									((Enum.Azione)((FlowsAttachment) h.getValue()).getMetadati().get("azione")).name().equals(Enum.Azione.PubblicazioneUrp.name()))
							.findAny();

					if(dataPubblicazione.isPresent())
						metadatiDocumento.put("dataPubblicazione", dataPubblicazione.get().getTime());

					documentiPubblicabili.add(metadatiDocumento);
				}
			}
		}
		return documentiPubblicabili;
	}
}