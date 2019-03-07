package it.cnr.si.flows.ng.service;

import com.opencsv.CSVWriter;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.resource.FlowsAttachmentResource;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.*;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.*;
import static it.cnr.si.flows.ng.utils.Utils.*;


@Service
public class FlowsTaskService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTaskService.class);
	public static final int LENGTH_TITOLO = 65;
	public static final int LENGTH_DESCTIZIONE = 75;
	public static final int LENGTH_INITIATOR = 45;
	public static final int LENGTH_FASE = 45;

	@Autowired @Qualifier("processEngine")
	protected ProcessEngine engine;
	@Inject
	private HistoryService historyService;
	@Inject
	private TaskService taskService;
	@Autowired(required = false)
	private AceBridgeService aceBridgeService;
	@Inject
	private FlowsAttachmentResource attachmentResource;
	@Inject
	private FlowsAttachmentService attachmentService;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private CounterService counterService;
	@Inject
	private RepositoryService repositoryService;
	@Inject
	private FlowsUserDetailsService flowsUserDetailsService;
	@Inject
	private PermissionEvaluatorImpl permissionEvaluator;
	@Inject
	private RelationshipService relationshipService;
	@Inject
	private ViewRepository viewRepository;
	@Inject
	private Utils utils;
	@Inject
	private RestResponseFactory restResponseFactory;

	public DataResponse search(Map<String, String> params, String processInstanceId, boolean active, String order, int firstResult, int maxResults) {
		HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery();

		if (!processInstanceId.equals(ALL_PROCESS_INSTANCES))
			taskQuery.processDefinitionKey(processInstanceId);

		setSearchTerms(params, taskQuery);

		if (active)
			taskQuery.unfinished();
		else
			taskQuery.finished();

		taskQuery = (HistoricTaskInstanceQuery) utils.orderTasks(order, taskQuery);

		List<HistoricTaskInstance> taskRaw = taskQuery.includeProcessVariables().listPage(firstResult, maxResults);

		// TODO
		DataResponse response = new DataResponse();
		response.setStart(firstResult);
		response.setSize(taskRaw.size());// numero di task restituiti
		response.setTotal(taskQuery.count()); //numero totale di task avviati da me
		response.setData(restResponseFactory.createHistoricTaskInstanceResponseList(taskRaw));

		return response;
	}

	// TODO questo metodo e' duplicato di uno in utils (controllare)
	private void setSearchTerms(Map<String, String> params, HistoricTaskInstanceQuery taskQuery) {

		params.forEach((key, typevalue) -> {

			if (key.equals("taskCompletedGreat"))
				taskQuery.taskCompletedAfter(javax.xml.bind.DatatypeConverter.parseDateTime(typevalue).getTime());
			else if (key.equals("taskCompletedLess"))
				taskQuery.taskCompletedBefore(javax.xml.bind.DatatypeConverter.parseDateTime(typevalue).getTime());
			else if (typevalue.contains("=")) {
				String type = typevalue.substring(0, typevalue.indexOf('='));
				String value = typevalue.substring(typevalue.indexOf('=')+1);

				if (key.contains("initiator") || key.contains("oggetto"))
					taskQuery.processVariableValueLikeIgnoreCase(key, "%" + value + "%");
				else if (key.contains("Fase"))
					taskQuery.taskNameLikeIgnoreCase("%" + value + "%");
                else if (key.contains("titolo"))
                    taskQuery.processVariableValueLike("titolo", "%" + value + "%");

                else {
					//wildcard ("%") di default ma non a TUTTI i campi
					switch (type) {
						case "textEqual":
							taskQuery.taskVariableValueEquals(key, value);
							break;
						case "boolean":
							// gestione variabili booleane
							taskQuery.taskVariableValueEquals(key, Boolean.valueOf(value));
							break;
						case "date":
							processDate(taskQuery, key, value);
							break;
						default:
							//variabili con la wildcard  (%value%)
							if (!value.isEmpty())
								taskQuery.taskVariableValueLikeIgnoreCase(key, "%" + value + "%");
							break;
					}
				}
			}
		});
	}

	private HistoricTaskInstanceQuery processDate(HistoricTaskInstanceQuery taskQuery, String key, String value) {
		try {
			Date date = utils.parsaData(value);

			if (key.contains(LESS)) {
				taskQuery.taskVariableValueLessThanOrEqual(key.replace(LESS, ""), date);
			} else if (key.contains(GREAT))
				taskQuery.taskVariableValueGreaterThanOrEqual(key.replace(GREAT, ""), date);
		} catch (ParseException e) {
			LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
		}
		return taskQuery;
	}

	public DataResponse getAvailableTask(HttpServletRequest req, String processDefinition, int firstResult, int maxResults, String order) {
		String username = SecurityUtils.getCurrentUserLogin();
		List<String> authorities =
				SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
						.map(GrantedAuthority::getAuthority)
						.map(Utils::removeLeadingRole)
						.collect(Collectors.toList());

		TaskQuery taskQuery = taskService.createTaskQuery()
				.taskCandidateUser(username)
				.taskCandidateGroupIn(authorities)
				.includeProcessVariables();

		taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskQuery);

		if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
			taskQuery.processDefinitionKey(processDefinition);

		utils.orderTasks(order, taskQuery);

		List<TaskResponse> list = restResponseFactory.createTaskResponseList(taskQuery.listPage(firstResult, maxResults));

		DataResponse response = new DataResponse();
		response.setStart(firstResult);
		response.setSize(list.size());
		response.setTotal(taskQuery.count());
		response.setData(list);
		return response;
	}

	public DataResponse taskAssignedInMyGroups(HttpServletRequest req, String processDefinition, int firstResult, int maxResults, String order) {
		String username = SecurityUtils.getCurrentUserLogin();

        List<String> userAuthorities = SecurityUtils.getCurrentUserAuthorities();

		TaskQuery taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskService.createTaskQuery().includeProcessVariables());

		if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
			taskQuery.processDefinitionKey(processDefinition);

		utils.orderTasks(order, taskQuery);

		////		TODO: da analizzare se le prestazioni sono migliori rispetto a farsi dare la lista di task attivi e ciclare per quali il member è l'assignee (codice di Martin sottostante)
		List<TaskResponse> result = new ArrayList<>();

		List<String> usersInMyGroups = relationshipService.getUsersInMyGroups(username);

		//      prendo i task assegnati agli utenti trovati
		for (String user : usersInMyGroups) {
            List<Task> tasks = taskQuery.taskAssignee(user).list()
                    .stream()
                    .filter(t ->
                            taskService.getIdentityLinksForTask(t.getId()).stream().anyMatch(il ->
                                    il.getType().equals(IdentityLinkType.CANDIDATE) && userAuthorities.contains(il.getGroupId()) )
                    ).collect(Collectors.toList());

            result.addAll(restResponseFactory.createTaskResponseList(tasks));
        }


		List<TaskResponse> responseList = result.subList(firstResult <= result.size() ? firstResult : result.size(),
														 maxResults <= result.size() ? maxResults : result.size());
		DataResponse response = new DataResponse();
		response.setStart(firstResult);
		response.setSize(responseList.size());
		response.setTotal(result.size());
		response.setData(responseList);
		return response;
	}

	public Map<String, Object> getTask(@PathVariable("id") String taskId) {
		Map<String, Object> response = new HashMap<>();
		Task taskRaw = taskService.createTaskQuery().taskId(taskId).includeProcessVariables().singleResult();

		// task + variables
		TaskResponse task = restResponseFactory.createTaskResponse(taskRaw);
		response.put("task", task);

		// attachments TODO
		ResponseEntity<Map<String, FlowsAttachment>> attachementsEntity = attachmentResource.getAttachementsForTask(taskId);
		Map<String, FlowsAttachment> attachments = attachementsEntity.getBody();

		response.put("attachments", attachments);
		return response;
	}

	public ProcessInstance startProcessInstance(Map<String, Object> data, String definitionId, String key) throws IOException {

		String username = SecurityUtils.getCurrentUserLogin();
		data.put(initiator.name(), username);
		data.put(startDate.name(), new Date());
		data.put("key", key);

		ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);
		runtimeService.setVariable(instance.getId(), "processInstanceId", instance.getId());

		// metadati da visualizzare in ricerca, li metto nel Name per comodita' in ricerca
		org.json.JSONObject name = new org.json.JSONObject();

		String titolo = (String) data.get(Enum.VariableEnum.titolo.name());
		name.put(Enum.VariableEnum.titolo.name(), ellipsis(titolo, LENGTH_TITOLO) );
		String descrizione = (String) data.get(Enum.VariableEnum.descrizione.name());
		name.put(Enum.VariableEnum.descrizione.name(), ellipsis(descrizione, LENGTH_DESCTIZIONE) );
		String initiator = (String) data.get(Enum.VariableEnum.initiator.name());
		name.put(Enum.VariableEnum.initiator.name(), initiator);
		String taskName = taskService.createTaskQuery()
				.processInstanceId(instance.getProcessInstanceId())
				.singleResult().getName();
		name.put(stato.name(), ellipsis(taskName, LENGTH_FASE) );

		runtimeService.setProcessInstanceName(instance.getId(), name.toString());

		LOGGER.info("Avviata istanza di processo {}, id: {}", key, instance.getId());
		return instance;
	}

	public DataResponse getTasksCompletedByMe(HttpServletRequest req, @RequestParam("processDefinition") String processDefinition, @RequestParam("firstResult") int firstResult, @RequestParam("maxResults") int maxResults, @RequestParam("order") String order) {
		String username = SecurityUtils.getCurrentUserLogin();

		HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(username)
				.includeProcessVariables().includeTaskLocalVariables();

		query = (HistoricTaskInstanceQuery) utils.searchParamsForTasks(req, query);

		if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
			query.processDefinitionKey(processDefinition);

		query = (HistoricTaskInstanceQuery) utils.orderTasks(order, query);
		//seleziono solo i task in cui il TASK_EXECUTOR sia l'utente che sta facendo la richiesta
		List<HistoricTaskInstance> taskList = new ArrayList<>();
		for (HistoricTaskInstance task : query.list()) {
			List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
			for (HistoricIdentityLink hil : identityLinks) {
				if (hil.getType().equals(TASK_EXECUTOR) && hil.getUserId().equals(username))
					taskList.add(task);
			}
		}
		List<HistoricTaskInstanceResponse> resultList = restResponseFactory.createHistoricTaskInstanceResponseList(
				taskList.subList(firstResult, (firstResult + maxResults <= taskList.size()) ? firstResult + maxResults : taskList.size()));

		DataResponse response = new DataResponse();
		response.setStart(firstResult);
		response.setSize(resultList.size());// numero di task restituiti
		response.setTotal(taskList.size()); //numero totale di task avviati da me
		response.setData(resultList);
		return response;
	}


	//    todo: testare il funzionamento
	public void buildCsv(List<HistoricTaskInstanceResponse> taskInstance, PrintWriter printWriter, String processDefinitionKey) throws IOException {
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
		for (HistoricTaskInstanceResponse task : taskInstance) {
			//            todo: riscrivere perchè adatto alle process instances
			List<RestVariable> variables = task.getVariables();
			ArrayList<String> tupla = new ArrayList<>();
			//field comuni a tutte le Task Instances (name , Start date)
			//            tupla.add(task.getBusinessKey());
			tupla.add(task.getName());
			tupla.add(utils.formattaDataOra(task.getStartTime()));

			//field specifici per ogni procesDefinition
			if (view != null) {
				JSONArray fields = new JSONArray(view.getView());
				for (int i = 0; i < fields.length(); i++) {
					JSONObject field = fields.getJSONObject(i);
					tupla.add(Utils.filterProperties(variables, field.getString("varName")));
					//solo per il primo ciclo, prendo le label dei field specifici
					if (!hasHeaders)
						headers.add(field.getString("label"));
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

	private static String ellipsis(String in, int length) {
		return in.length() < length ? in: in.substring(0, length - 3) + "...";
	}
}