package it.cnr.si.flows.ng.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.repository.FlowsHistoricProcessInstanceQuery;
import it.cnr.si.flows.ng.resource.FlowsAttachmentResource;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.service.DraftService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.SecurityService;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.initiator;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.startDate;
import static it.cnr.si.flows.ng.utils.Utils.*;


@Service
public class FlowsTaskService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTaskService.class);
	public static final int LENGTH_TITOLO = 65;
	public static final int LENGTH_DESCRIZIONE = 75;
	public static final int LENGTH_INITIATOR = 45;
	public static final int LENGTH_STATO = 45;

	@Autowired @Qualifier("processEngine")
	protected ProcessEngine engine;
	@Inject
	private HistoryService historyService;
	@Inject
	private TaskService taskService;
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
	private UserDetailsService flowsUserDetailsService;
	@Inject
	private PermissionEvaluatorImpl permissionEvaluator;
	@Inject
	private RelationshipService relationshipService;
	@Inject
	private MembershipService membershipService;
	@Inject
	private ViewRepository viewRepository;
	@Inject
	private Utils utils;
	@Inject
	private RestResponseFactory restResponseFactory;
	@Inject
	private Environment env;
	@Inject
	private DraftService draftService;
	@Inject
	private ManagementService managementService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private SecurityService securityService;
	@Inject
	private SecurityUtils securityUtils;



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

	public Task getActiveTaskForProcessInstance(String processInstanceId) {
		List<Task> tasks = taskService.createTaskQuery().active().processInstanceId(processInstanceId).list();

		if (tasks.size() == 0)
			throw new UnexpectedResultException("Nessun task attivo per il processo: "+ processInstanceId);
		if (tasks.size() > 1)
			throw new UnexpectedResultException("Risulta piu' di un task attivo per il processo: "+ processInstanceId);

		return tasks.get(0);
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

	public DataResponse getAvailableTask(JSONArray searchParams, String processDefinition, int firstResult, int maxResults, String order) {
		String username = securityService.getCurrentUserLogin();
//		List<String> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
//				.map(GrantedAuthority::getAuthority)
//				.map(Utils::removeLeadingRole)
//				.map(Utils::removeImportoSpesa)
//				.collect(Collectors.toList());

		List<String> authorities = securityService.getUser().get().getAuthorities()
		        .stream()
		        .map(GrantedAuthority::getAuthority)
		        .collect(Collectors.toList());
		
		TaskQuery taskQuery = taskService.createTaskQuery()
				.taskCandidateUser(username)
				.taskCandidateGroupIn(authorities)
				.includeProcessVariables();

		taskQuery = (TaskQuery) utils.searchParams(searchParams, taskQuery);

		if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
			taskQuery.processDefinitionKey(processDefinition);

		utils.orderTasks(order, taskQuery);

		List<Task> tasks = taskQuery.listPage(firstResult, maxResults);
		int rimossi = rimuoviTaskImportoSpesa(tasks);

		List<TaskResponse> list = restResponseFactory.createTaskResponseList(tasks);

		DataResponse response = new DataResponse();
		response.setStart(firstResult);
		response.setSize(list.size() - rimossi);
		response.setTotal(taskQuery.count() - rimossi);
		response.setData(list);
		return response;
	}

	private int rimuoviTaskImportoSpesa(List<Task> list) {

		int removed = 0;

        List<String> authorities = securityService.getUser().get().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

		Iterator<Task> i = list.iterator();
		while (i.hasNext()) {
			Task task = i.next();
			if (!permissionEvaluator.isCandidatoDiretto(task.getId(), authorities) &&
					!permissionEvaluator.canCompleteImportoSpesa(task.getId(), authorities)) {
				i.remove();
				removed++;
			}
		}


		return removed;
	}


	public DataResponse taskAssignedInMyGroups(JSONArray searchParams, String processDefinition, int firstResult, int maxResults, String order) {

		String username = securityService.getCurrentUserLogin();
		List<String> userAuthorities = securityUtils.getCurrentUserAuthorities();
		Set<String> ruoliUtente = membershipService.getAllRolesForUser(username);

		FlowsHistoricProcessInstanceQuery processQuery = new FlowsHistoricProcessInstanceQuery(managementService);
		processQuery.setVisibleToGroups(userAuthorities);
		processQuery.setVisibleToUser(username);
		processQuery.unfinished();

		//trasformo i searchParams (da JSONArray a Hashmap) togliendo il "type"
		Map<String, String> mapParams = new HashMap<>();
		for (int i = 0; i < searchParams.length(); i++) {
			JSONObject appo = (JSONObject) searchParams.get(i);
			mapParams.put(appo.getString("key"), appo.getString("value"));
		}

		flowsProcessInstanceService.setSearchTerms(mapParams, processQuery);
		if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
			processQuery.processDefinitionKey(processDefinition);
		if (order.equals(ASC))
			processQuery.orderByProcessInstanceStartTime().asc();
		else if (order.equals(DESC))
			processQuery.orderByProcessInstanceStartTime().desc();

		List<HistoricProcessInstance> pil = processQuery.list();

		//per ogni Pi prendo il task attivo e costruisco la response
		List<Task> result = pil.stream()
				.map(pi ->  taskService.createTaskQuery().active().processInstanceId(pi.getId())
						.includeProcessVariables().list().get(0))
				.collect(Collectors.toList());

		List<TaskResponse> responseList = new ArrayList();
		List<TaskResponse> taskList = restResponseFactory.createTaskResponseList(result);

		for (TaskResponse task : taskList) {
			List<HistoricIdentityLink> identityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
			boolean assigneeFlag = false;
			boolean candidateFlag = false;

			for (HistoricIdentityLink hil : identityLinks) {
				if (hil.getType().equals("assignee")) {
					if (!hil.getUserId().equals(username))
						assigneeFlag = true;
				}
				if (hil.getType().equals("candidate")) {
					if (hil.getUserId() != null && hil.getUserId().equals(username))
						candidateFlag = true;
				}
				if (hil.getType().equals("candidate")) {
					if (hil.getGroupId() != null && ruoliUtente.contains(hil.getGroupId()))
						candidateFlag = true;
				}
			}
			if (candidateFlag && assigneeFlag)
				responseList.add(task);
		}
		responseList.subList(firstResult <= responseList.size() ? firstResult : responseList.size(),
							 maxResults <= responseList.size() ? maxResults : responseList.size());

		DataResponse response = new DataResponse();
		response.setStart(firstResult);
		response.setSize(0);
		response.setTotal(responseList.size());
		response.setData(responseList);
		return response;
	}


	public DataResponse getMyTasks(JSONArray searchParams, String processDefinition, int firstResult, int maxResults, String order) {
		TaskQuery taskQuery = (TaskQuery) utils.searchParams(searchParams, taskService.createTaskQuery());
		taskQuery.taskAssignee(securityService.getCurrentUserLogin())
				.includeProcessVariables();

		if (!processDefinition.equals(ALL_PROCESS_INSTANCES))
			taskQuery.processDefinitionKey(processDefinition);

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

	public ProcessInstance startProcessInstance(String definitionId, Map<String, Object> data) {

		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();
		String counterId = processDefinition.getName() + "-" + Calendar.getInstance().get(Calendar.YEAR);
		String key = counterId + "-" + counterService.getNext(counterId);
		data.put("key", key);
		//L`utenza reale (admin o ROLE_amministratori-supporto-tecnico@0000)
		// non può avviare il flusso quindi ho bisogno dell`utenza "fittizia"/impersonata
		data.put(initiator.name(), securityService.getCurrentUserLogin());
		data.put(startDate.name(), new Date());

		ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);
		runtimeService.setVariable(instance.getId(), "processInstanceId", instance.getId());

		String statoPI;
		if (taskService.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).count() == 0) {
			statoPI = utils.ellipsis("START", LENGTH_STATO);
		} else {
			String taskName = taskService.createTaskQuery()
					.processInstanceId(instance.getProcessInstanceId())
					.singleResult().getName();
			statoPI = utils.ellipsis(taskName, LENGTH_STATO);
		}
		utils.updateJsonSearchTerms(null, instance.getProcessInstanceId(), statoPI);

		return instance;
	}



	public ProcessInstance startProcessInstanceAsApplication(String definitionId, Map<String, Object> data, String applicationName) {

		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitionId).singleResult();
		String counterId = processDefinition.getName() + "-" + Calendar.getInstance().get(Calendar.YEAR);
		String key = counterId + "-" + counterService.getNext(counterId);
		data.put("key", key);

		String username = securityService.getCurrentUserLogin();

		data.put(applicationName, username);
		data.put(startDate.name(), new Date());

		ProcessInstance instance = runtimeService.startProcessInstanceById(definitionId, key, data);
		runtimeService.setVariable(instance.getId(), "processInstanceId", instance.getId());

//todo: da testare
		String statoPI;
		if (taskService.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).count() == 0) {
			statoPI = utils.ellipsis("START", LENGTH_STATO);
		} else {
			String taskName = taskService.createTaskQuery()
					.processInstanceId(instance.getProcessInstanceId())
					.singleResult().getName();
			statoPI = utils.ellipsis(taskName, LENGTH_STATO);
		}

		//utils.updateJsonSearchTerms( null, instance.getProcessInstanceId(), statoPI);

		LOGGER.info("Avviata istanza di processo {}, id: {}", key, instance.getId());
		return instance;
	}


	public void completeTask(String taskId, Map<String, Object> data) {

		// TODO
		// String username = SecurityUtils.getRealUserLogged();
		String username = securityService.getCurrentUserLogin();

		// aggiungo l'identityLink che indica l'utente che esegue il task
		taskService.setVariablesLocal(taskId, data);
		taskService.addUserIdentityLink(taskId, username, TASK_EXECUTOR);
		try {
			taskService.complete(taskId, data);

			draftService.deleteDraftByTaskId(Long.valueOf(taskId));
		} catch (Exception e) {
			if (e instanceof ActivitiObjectNotFoundException)
				LOGGER.error("Task {} NON trovato", taskId);

			taskService.deleteUserIdentityLink(taskId, username, TASK_EXECUTOR);
			throw e;
		}
	}

	public DataResponse getTasksCompletedByMe(JSONArray searchParams, @RequestParam("processDefinition") String processDefinition, @RequestParam("firstResult") int firstResult, @RequestParam("maxResults") int maxResults, @RequestParam("order") String order) {
		String username = securityService.getCurrentUserLogin();

		HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery().taskInvolvedUser(username)
				.includeProcessVariables().includeTaskLocalVariables();

		query = (HistoricTaskInstanceQuery) utils.searchParams(searchParams, query);

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
		headers.add("processInstanceId");
		headers.add("Identificativo Flusso");
		headers.add("Titolo");
		headers.add("Descrizione");
		headers.add("Utente che ha avviato il flusso");
		headers.add("Stato");
		headers.add("Data Inizio");
		headers.add("Data Fine");

		for (HistoricProcessInstanceResponse processInstance : processInstances) {
			List<RestVariable> variables = processInstance.getVariables();
			ArrayList<String> tupla = new ArrayList<>();
			//field comuni a tutte le Process Instances (name , Start date)
			tupla.add(processInstance.getId());
			tupla.add(processInstance.getBusinessKey());

			// inizio spacchettamento fields
			Map<String, String> name = new ObjectMapper().readValue(processInstance.getName(), new TypeReference<Map<String, String>>() {});
			tupla.add(name.get("titolo"));
			tupla.add(name.get("descrizione"));
			tupla.add(name.get("initiator"));
			tupla.add(name.get("stato"));
			//fine spacchettamento fields

			tupla.add(utils.formattaDataOra(processInstance.getStartTime()));
			tupla.add(utils.formattaDataOra(processInstance.getEndTime()));

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
}