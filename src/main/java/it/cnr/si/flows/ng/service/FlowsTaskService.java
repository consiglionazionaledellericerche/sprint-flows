package it.cnr.si.flows.ng.service;

import com.opencsv.CSVWriter;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.ProcessDefinitionAndTaskIdEmptyException;
import it.cnr.si.flows.ng.resource.FlowsAttachmentResource;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.*;
import org.activiti.engine.delegate.BpmnError;
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
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartHttpServletRequest;

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
	private static final String ERROR_MESSAGE = "message";
	public static final int LENGTH_TITOLO = 65;
	public static final int LENGTH_DESCTIZIONE = 75;
	public static final int LENGTH_INITIATOR = 45;
	public static final int LENGTH_FASE = 45;
	@Autowired
	@Qualifier("processEngine")
	protected ProcessEngine engine;
	@Inject
	private HistoryService historyService;
	@Inject
	private RestResponseFactory restResponseFactory;
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


	// TODO magari un giorno avremo degli array, ma per adesso ce lo facciamo andare bene cosi'
	private static Map<String, Object> extractParameters(MultipartHttpServletRequest req) {

		Map<String, Object> data = new HashMap<>();
		List<String> parameterNames = Collections.list(req.getParameterNames());
		parameterNames.stream().forEach(paramName -> {
			// se ho un json non aggiungo i suoi singoli campi (Ed escludo il parametro "cacheBuster")
			if ((!parameterNames.contains(paramName.split("\\[")[0] + "_json")) && (!paramName.equals("cacheBuster")))
				data.put(paramName, req.getParameter(paramName));
		});
		return data;
	}

	public DataResponse getMyTask(HttpServletRequest req, String processDefinition, int firstResult, int maxResults, String order) {
		String username = SecurityUtils.getCurrentUserLogin();

		TaskQuery taskQuery = taskService.createTaskQuery()
				.taskAssignee(username)
				.includeProcessVariables();

		if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
			taskQuery.processDefinitionKey(processDefinition);

		taskQuery = (TaskQuery) utils.searchParamsForTasks(req, taskQuery);

		utils.orderTasks(order, taskQuery);

		List<TaskResponse> tasksList = restResponseFactory.createTaskResponseList(taskQuery.listPage(firstResult, maxResults));

		//aggiungo ad ogni singola TaskResponse la variabile che indica se il task è restituibile ad un gruppo (true)
		// o se è stato assegnato ad un utente specifico "dal sistema" (false)
		addIsReleasableVariables(tasksList);

		DataResponse response = new DataResponse();
		response.setStart(firstResult);
		response.setSize(tasksList.size());
		response.setTotal(taskQuery.count());
		response.setData(tasksList);
		return response;
	}

	public Map<String, Object> search(Map<String, String> params, String processInstanceId, boolean active, String order, int firstResult, int maxResults) {
		Map<String, Object> result = new HashMap<>();
		HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery();

		if (!processInstanceId.equals(ALL_PROCESS_INSTANCES))
			taskQuery.processDefinitionKey(processInstanceId);

		setSearchTerms(params, taskQuery);

		if (active)
			taskQuery.unfinished();
		else
			taskQuery.finished();

		taskQuery = (HistoricTaskInstanceQuery) utils.orderTasks(order, taskQuery);

		long totalItems = taskQuery.count();
		result.put("totalItems", totalItems);

		List<HistoricTaskInstance> taskRaw = taskQuery.includeProcessVariables().listPage(firstResult, maxResults);
		List<HistoricTaskInstanceResponse> tasks = restResponseFactory.createHistoricTaskInstanceResponseList(taskRaw);
		result.put("tasks", tasks);
		return result;
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

//        String authorithiesList = userAuthorities.stream().map(s -> "'"+s+"'").collect(Collectors.joining(","));

//        NativeTaskQuery nativeTaskQuery = taskService.createNativeTaskQuery().sql(
//				"SELECT task.* FROM ACT_RU_TASK task, ACT_RU_IDENTITYLINK link " +
//						" WHERE task.ID_ = link.TASK_ID_ AND link.GROUP_ID_ IN("+ authorithiesList +")");

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
                                    il.getType() == IdentityLinkType.CANDIDATE && userAuthorities.contains(il.getGroupId()) )
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

		// attachments
		ResponseEntity<Map<String, FlowsAttachment>> attachementsEntity = attachmentResource.getAttachementsForTask(taskId);
		Map<String, FlowsAttachment> attachments = attachementsEntity.getBody();
		attachments.values().stream().forEach(e -> e.setBytes(null)); // il contenuto dei file non mi serve, e rallenta l'UI
		response.put("attachments", attachments);
		return response;
	}

	public ResponseEntity<Object> completeTask(MultipartHttpServletRequest req) throws IOException {
		String username = SecurityUtils.getCurrentUserLogin();

		String taskId = (String) req.getParameter("taskId");
		String definitionId = (String) req.getParameter("processDefinitionId");
		if (isEmpty(taskId) && isEmpty(definitionId))
			throw new ProcessDefinitionAndTaskIdEmptyException();

		Map<String, Object> data = extractParameters(req);
		attachmentService.extractAttachmentVariables(req, data);

		if (isEmpty(taskId)) {

			ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();
			try {
				String counterId = processDefinition.getName() + "-" + Calendar.getInstance().get(Calendar.YEAR);
				String key = counterId + "-" + counterService.getNext(counterId);

				//recupero l'idStruttura dell'utente che sta avviando il flusso
				List<GrantedAuthority> authorities = relationshipService.getAllGroupsForUser(username);
				List<String> groups = authorities.stream()
						.map(GrantedAuthority::<String>getAuthority)
						.map(Utils::removeLeadingRole)
						.filter(g -> g.startsWith("abilitati#"+ processDefinition.getKey() +"@"))
						.collect(Collectors.toList());

				if (groups.isEmpty()) {
					throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
				} else {
					// TODO la struttura va inserita nei listener specifico del flusso e non allo start
					//                    String gruppoAbilitati = groups.get(0);
					//                    String idStrutturaString = gruppoAbilitati.substring(gruppoAbilitati.lastIndexOf('@') + 1);

					data.put(initiator.name(), username);
					data.put(startDate.name(), new Date());
					data.put("key", key);

					ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);
					runtimeService.setVariable(instance.getId(), "processInstanceId", instance.getId());
					
					org.json.JSONObject name = new org.json.JSONObject();
					//                    name.put(idStruttura.name(), idStrutturaString);

					String titolo = (String) data.get(Enum.VariableEnum.titolo.name());
					name.put(Enum.VariableEnum.titolo.name(),
							 titolo.length() < LENGTH_TITOLO ? titolo: titolo.substring(0, LENGTH_TITOLO - 3) + "...");
					String descrizione = (String) data.get(Enum.VariableEnum.descrizione.name());
					name.put(Enum.VariableEnum.descrizione.name(),
							 descrizione.length() < LENGTH_DESCTIZIONE ? descrizione : descrizione.substring(0, LENGTH_DESCTIZIONE - 3) + "...");
					String initiator = (String) data.get(Enum.VariableEnum.initiator.name());
					name.put(Enum.VariableEnum.initiator.name(),
							 initiator.length() < LENGTH_INITIATOR ? initiator : initiator.substring(0, LENGTH_INITIATOR - 3) + "...");
					String taskName = taskService.createTaskQuery()
							.processInstanceId(instance.getProcessInstanceId())
							.singleResult().getName();
					name.put(stato.name(),
							 taskName.length() < LENGTH_FASE ? taskName : taskName.substring(0, LENGTH_FASE - 3) + "...");

					runtimeService.setProcessInstanceName(instance.getId(), name.toString());

					LOGGER.info("Avviata istanza di processo {}, id: {}", key, instance.getId());

					ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(instance);
					return new ResponseEntity<>(response, HttpStatus.OK);
				}
			} catch (Exception e) {
				String errorMessage = String.format("Errore nell'avvio della Process Instances di tipo %s con eccezione:", processDefinition);
				LOGGER.error(errorMessage, e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(ERROR_MESSAGE, errorMessage));
			}
		} else {
			try {
				// aggiungo l'identityLink che indica l'utente che esegue il task
				taskService.addUserIdentityLink(taskId, username, TASK_EXECUTOR);
				taskService.setVariablesLocal(taskId, data);
				taskService.complete(taskId, data);

				return new ResponseEntity<>(HttpStatus.OK);
			} catch (Exception e) {
				//Se non riesco a completare il task rimuovo l'identityLink che indica "l'esecutore" del task e restituisco un INTERNAL_SERVER_ERROR
				//l'alternativa sarebbe aggiungere l'identityLink dopo avwer completato il task ma posso creare identityLink SOLO di task attivi
				if(((BpmnError) e).getErrorCode() == "412"){
					String errorMessage = String.format("%s", e.getMessage());
					LOGGER.warn(errorMessage);
					taskService.deleteUserIdentityLink(taskId, username, TASK_EXECUTOR);
					return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(mapOf(ERROR_MESSAGE, errorMessage));
				} else {
					String errorMessage = String.format("Errore durante il tentativo di completamento del task %s da parte dell'utente %s: %s", taskId, username, e.getMessage());
					LOGGER.error(errorMessage);
					taskService.deleteUserIdentityLink(taskId, username, TASK_EXECUTOR);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(ERROR_MESSAGE, errorMessage));}
			}
		}
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

	private void addIsReleasableVariables(List<TaskResponse> tasks) {
		for (TaskResponse task : tasks) {
			RestVariable isUnclaimableVariable = new RestVariable();
			isUnclaimableVariable.setName("isReleasable");
			// if has candidate groups or users -> can release
			isUnclaimableVariable.setValue(taskService.getIdentityLinksForTask(task.getId())
												   .stream()
												   .anyMatch(l -> l.getType().equals(IdentityLinkType.CANDIDATE)));
			task.getVariables().add(isUnclaimableVariable);
		}
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
}