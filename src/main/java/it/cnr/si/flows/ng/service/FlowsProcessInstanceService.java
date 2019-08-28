package it.cnr.si.flows.ng.service;

import com.opencsv.CSVWriter;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.repository.FlowsHistoricProcessInstanceQuery;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.PermissionEvaluatorImpl;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricIdentityLinkResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.cnr.si.flows.ng.utils.Utils.*;


/**
 * Created by cirone on 15/06/17.
 */
@Service
public class FlowsProcessInstanceService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceService.class);
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private HistoryService historyService;
	@Inject
	private RestResponseFactory restResponseFactory;
	@Inject
	private RepositoryService repositoryService;
	@Inject
	private TaskService taskService;
	@Inject
	private ViewRepository viewRepository;
	@Inject
	private ManagementService managementService;
	@Inject
	private RuntimeService runtimeService;
	@Autowired(required = false)
	private AceBridgeService aceBridgeService;
	@Inject
	PermissionEvaluatorImpl permissionEvaluator;
	@Inject
	private FlowsUserDetailsService flowsUserDetailsService;
    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
	private Utils utils;

	public HistoricTaskInstance getCurrentTaskOfProcessInstance(String processInstanceId) {
		return historyService.createHistoricTaskInstanceQuery()
				.processInstanceId(processInstanceId)
				.list()
				.stream()
				.filter(historicTaskInstance -> !Optional.ofNullable(historicTaskInstance.getEndTime()).isPresent())
				.findAny()
				.orElseThrow(() -> new RuntimeException("Nessun Task attivo"));
	}

	public HistoricProcessInstance getProcessInstance(String processInstanceId) {
		return historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId)
				.singleResult();
	}

	public Map<String, Object> getProcessInstanceWithDetails(String processInstanceId) {
		Map<String, Object> result = new HashMap<>();

		// PrecessInstance metadata
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId)
				.includeProcessVariables()
				.singleResult();
		result.put("entity", restResponseFactory.createHistoricProcessInstanceResponse(processInstance));

		// ProcessDefinition (static) metadata
		ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

		// Attachments
		Map<String, FlowsAttachment> attachements = flowsAttachmentService.getAttachementsForProcessInstance(processInstanceId);
		// in visualizzazione dettagli non mi servono i dati binari degli allegati
		attachements.values().stream().forEach(a -> a.setBytes(null));
		result.put("attachments", attachements);

		final Map<String, Object> identityLinks = new LinkedHashMap<>();
		Map<String, Object> processLinks = new HashMap<>();
		processLinks.put("links", restResponseFactory.createHistoricIdentityLinkResponseList(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceId)));
		identityLinks.put("process", processLinks);
		taskService.createTaskQuery().processInstanceId(processInstanceId).active().list().forEach(
				task -> {
					Map<String, Object> identityLink = new HashMap<>();
					String taskDefinitionKey = task.getTaskDefinitionKey();
					PvmActivity taskDefinition = processDefinition.findActivity(taskDefinitionKey);
					TaskDefinition taskDef = (TaskDefinition) taskDefinition.getProperty("taskDefinition");
					List<IdentityLink> links = taskService.getIdentityLinksForTask(task.getId());

					identityLink.put("links", restResponseFactory.createRestIdentityLinks(links));
					identityLink.put("assignee", taskDef.getAssigneeExpression());
					identityLink.put("candidateGroups", taskDef.getCandidateGroupIdExpressions());
					identityLink.put("candidateUsers", taskDef.getCandidateUserIdExpressions());

					identityLinks.put(task.getId(), identityLink);
				});
		result.put("identityLinks", identityLinks);

		//History
		ArrayList<Map<String, Object>> history = new ArrayList<>();
		historyService.createHistoricTaskInstanceQuery()
				.includeTaskLocalVariables()
				.processInstanceId(processInstanceId)
				.list()
				.forEach(
						task -> {
							List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(task.getId());
							HashMap<String, Object> entity = new HashMap<>();
							entity.put("historyTask", restResponseFactory.createHistoricTaskInstanceResponse(task));

							// Sostituisco l'id interno del gruppo con la dicitura estesa
							entity.put("historyIdentityLink", Optional.ofNullable(links)
									.map(historicIdentityLinks -> restResponseFactory.createHistoricIdentityLinkResponseList(historicIdentityLinks))
									.filter(historicIdentityLinkResponses -> !historicIdentityLinkResponses.isEmpty())
									.map(historicIdentityLinkResponses -> historicIdentityLinkResponses.stream())
									.orElse(Stream.empty())
									.map(h -> {
										if (Optional.ofNullable(aceBridgeService).isPresent()) {
											h.setGroupId(aceBridgeService.getExtendedGroupNome(h.getGroupId()));
										}
										return h;
									}).collect(Collectors.toList()));
							history.add(entity);
						});
		result.put("history", history);
		return result;
	}


	public Map<String, Object> search(Map<String, String> req, String processDefinitionKey, boolean active, String order, int firstResult, int maxResults) {

		FlowsHistoricProcessInstanceQuery processQuery = new FlowsHistoricProcessInstanceQuery(managementService);

		setSearchTerms(req, processQuery);

		List<String> authorities = Utils.getCurrentUserAuthorities();

		// solo l'admin ignora le regole di visibilita'
		if (!authorities.contains("ADMIN")) {
			processQuery.setVisibleToGroups(authorities);
			processQuery.setVisibleToUser(SecurityContextHolder.getContext().getAuthentication().getName());
		}

		if (!processDefinitionKey.equals(ALL_PROCESS_INSTANCES))
			processQuery.processDefinitionKey(processDefinitionKey);

		if (active)
			processQuery.unfinished();
		else
			processQuery.finished();

		if (order.equals(ASC))
			processQuery.orderByProcessInstanceStartTime().asc();
		else if (order.equals(DESC))
			processQuery.orderByProcessInstanceStartTime().desc();


		Map<String, Object> result = new HashMap<>();

		long totalItems = processQuery.count();
		result.put("totalItems", totalItems);

		List<HistoricProcessInstance> processesRaw;

		if (firstResult != -1 && maxResults != -1)
			processesRaw = processQuery.listPage(firstResult, maxResults);
		else
			processesRaw = processQuery.list();

		List<HistoricProcessInstanceResponse> processes = restResponseFactory.createHistoricProcessInstanceResponseList(processesRaw);
		result.put("processInstances", processes);

		return result;
	}


	private void setSearchTerms(Map<String, String> params, FlowsHistoricProcessInstanceQuery processQuery) {

		String title = params.remove("title");
		String titolo = params.remove("titolo");
		String initiator = params.remove("initiator");
		String descrizione = params.remove("descrizione");

		//i campi "titolo, "title", "initiator", "descrizione" sono salvati in un json in name e non come variabili di Process Instance
		if (title != null || titolo != null || initiator != null || descrizione != null) {
			String appo = "";
			//l'ordine delle field di ricerca è importante nella query sul campo singolo "name"
			//todo: è una porcata ma avere i campi in "name" migliora di moltissimo le prestazioni della ricerca
			if (descrizione != null)
				appo += "%\"descrizione\":\"%" + descrizione.substring(descrizione.indexOf('=') + 1) + "%\"%";
			if (titolo != null)
				appo += "%\"titolo\":\"%" + titolo.substring(titolo.indexOf('=') + 1) + "%\"%";
			if (initiator != null)
				appo += "%\"initiator\":\"%" + initiator.substring(initiator.indexOf('=') + 1) + "%\"%";
			if (title != null)
				appo += "%\"title\":\"%" + title.substring(title.indexOf('=') + 1) + "%\"%";

			processQuery.processInstanceNameLikeIgnoreCase(appo);
		}

		params.forEach((key, typevalue) -> {
			if (typevalue.contains("=")) {
				String type = typevalue.substring(0, typevalue.indexOf('='));
				String value = typevalue.substring(typevalue.indexOf('=')+1);

				switch (type) {
					case "textEqual":
						processQuery.variableValueEquals(key, value);
						break;
					case "boolean":
						// gestione variabili booleane
						processQuery.variableValueEquals(key, Boolean.valueOf(value));
						break;
					default:
						//variabili con la wildcard  (%value%)
						processQuery.variableValueLikeIgnoreCase(key, "%" + value + "%");
						break;
				}
			} else {
				//per <input type="date"' non funziona "input-prepend" quindi rimetto la vecchia implementazione
				if ("startDateGreat".equals(key) || "startDateLess".equals(key)) {
					processDate(processQuery, key, typevalue);
				}
			}
		});
	}


	public void buildCsv(List<HistoricProcessInstanceResponse> processInstances, PrintWriter printWriter, String processDefinitionKey) throws IOException {
		// vista (campi e variabili) da inserire nel csv in base alla tipologia di flusso selezionato
		View view = null;
		if (!processDefinitionKey.equals(ALL_PROCESS_INSTANCES)) {
			view = viewRepository.getViewByProcessidType(processDefinitionKey, "export-csv");
		}
		CSVWriter writer = new CSVWriter(printWriter, '\t');
		ArrayList<String[]> entriesIterable = new ArrayList<>();
		boolean hasHeaders = false;
		ArrayList<String> headers = new ArrayList<>();
		headers.add("Business Key");
		headers.add("Start Date");
		for (HistoricProcessInstanceResponse pi : processInstances) {
			Map<String, Object> map = flowsProcessInstanceService.getProcessInstanceWithDetails(pi.getId());
	        HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) map.get("entity");
	        List<RestVariable> variables = processInstance.getVariables();
			//List<RestVariable> variables = pi.getVariables();
			ArrayList<String> tupla = new ArrayList<>();
			//field comuni a tutte le Process Instances (Business Key, Start date)
			tupla.add(pi.getBusinessKey());
			tupla.add(utils.formattaDataOra(pi.getStartTime()));

			//field specifici per ogni procesDefinition
			if (view != null) {
				try {
					JSONArray fields = new JSONArray(view.getView());
					for (int i = 0; i < fields.length(); i++) {
						JSONObject field = fields.getJSONObject(i);
						tupla.add(Utils.filterProperties(variables, field.getString("varName")));
						//solo per il primo ciclo, prendo le label dei field specifici
						if (!hasHeaders)
							headers.add(field.getString("label"));
					}
				} catch (JSONException e) {
					LOGGER.error("Errore nel processamento del JSON", e);
					throw new IOException(e);
				}
			}
			if (!hasHeaders) {
				//inserisco gli headers come intestazione dei field del csv
				entriesIterable.add(0, utils.getArray(headers));
				hasHeaders = true;
			}
			entriesIterable.add(utils.getArray(tupla));
		}
		writer.writeAll(entriesIterable);
		writer.close();
	}


	private void processDate(HistoricProcessInstanceQuery processQuery, String key, String value) {
		Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(value);

		if (key.contains("Less"))
			processQuery.startedBefore(calendar.getTime());
		else if (key.contains("Great"))
			processQuery.startedAfter(calendar.getTime());
	}
}
