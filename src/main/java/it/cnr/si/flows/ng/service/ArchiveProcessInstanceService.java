package it.cnr.si.flows.ng.service;

import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static it.cnr.si.flows.ng.utils.Utils.DESC;
import static it.cnr.si.flows.ng.utils.Utils.DESCRIZIONE;
import static it.cnr.si.flows.ng.utils.Utils.INITIATOR;
import static it.cnr.si.flows.ng.utils.Utils.TITOLO;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.ReadOnlyProcessDefinition;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.PermissionEvaluatorImpl;


@Service
public class ArchiveProcessInstanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveProcessInstanceService.class);
    @Inject
    private RestResponseFactory restResponseFactory;
    @Autowired(required = false)
    private AceBridgeService aceBridgeService;
    @Inject
    PermissionEvaluatorImpl permissionEvaluator;
    @Inject
    private UserDetailsService flowsUserDetailsService;
    @Inject @Qualifier("archiveProcessEngine")
    private ProcessEngine archiveProcessEngine;


    public HistoricProcessInstance getProcessInstance(String processInstanceId) {
        return archiveProcessEngine.getHistoryService().createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    public Map<String, Object> getProcessInstanceWithDetails(String processInstanceId) {
        Map<String, Object> result = new HashMap<>();

        HistoryService historyService = archiveProcessEngine.getHistoryService();
        RepositoryService repositoryService = archiveProcessEngine.getRepositoryService();
        TaskService taskService = archiveProcessEngine.getTaskService();
        // PrecessInstance metadata
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();

        //Durante la fase di creazione di una Process Instance viene richiamato questo metodo ma la query sarà vuota perchè la Pi effettivamente non è stata ancora creata
        if(processInstance != null) {
            HistoricProcessInstanceResponse entity = restResponseFactory.createHistoricProcessInstanceResponse(processInstance);
            result.put("entity", entity);

            Map<String, RestVariable> variabili = new HashMap<>();
            entity.getVariables().forEach(v -> variabili.put(v.getName(), v));
            result.put("variabili", variabili); // Modifica per vedere piu' comodamente le variabili

            HistoricVariableInstance links = historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName("linkToOtherWorkflows")
                    .singleResult();


            if (links != null) {

                List<Map<String, Object>> linkedFlows = new ArrayList<>();
                String value = (String) links.getValue();
                String[] values = value.split(",");

                for (String linkedProcessId : values) {
                    if(permissionEvaluator.canVisualize(linkedProcessId, flowsUserDetailsService)) {
                        HistoricProcessInstance linkedProcessInstance = historyService
                                .createHistoricProcessInstanceQuery()
                                .processInstanceId(linkedProcessId)
                                .includeProcessVariables()
                                .singleResult();

                        if (linkedProcessInstance != null) {
                            String key = linkedProcessInstance.getBusinessKey();

                            Map<String, Object> linkedObject = new HashMap<>();
                            linkedObject.put("id", linkedProcessId);
                            linkedObject.put("key", key);
                            linkedObject.put("titolo", linkedProcessInstance.getProcessVariables().get("titolo"));

                            linkedFlows.add(linkedObject);
                        }
                    }
                }
                if (!linkedFlows.isEmpty())
                    result.put("linkedProcesses", linkedFlows);
            }
        }

        // ProcessDefinition (static) metadata
        ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

        final Map<String, Object> identityLinks = new LinkedHashMap<>();
        Map<String, Object> processLinks = new HashMap<>();
        processLinks.put("links", restResponseFactory.createHistoricIdentityLinkResponseList(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceId)));
        identityLinks.put("process", processLinks);

        List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        //History
        ArrayList<Map<String, Object>> history = new ArrayList<>();

        taskList
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

                    // se il task è quello attivo prendo anche i gruppi o gli utenti assegnee/candidate
                    if(task.getEndTime() == null) {
                        Map<String, Object> identityLink = new HashMap<>();
                        String taskDefinitionKey = task.getTaskDefinitionKey();
                        PvmActivity taskDefinition = processDefinition.findActivity(taskDefinitionKey);
                        TaskDefinition taskDef = (TaskDefinition) taskDefinition.getProperty("taskDefinition");
                        List<IdentityLink> taskLinks = taskService.getIdentityLinksForTask(task.getId());

                        identityLink.put("links", restResponseFactory.createRestIdentityLinks(taskLinks));
                        identityLink.put("assignee", taskDef.getAssigneeExpression());
                        identityLink.put("candidateGroups", taskDef.getCandidateGroupIdExpressions());
                        identityLink.put("candidateUsers", taskDef.getCandidateUserIdExpressions());

                        identityLinks.put(task.getId(), identityLink);
                    }
                });

        result.put("identityLinks", identityLinks);

        result.put("history", history);

        // permessi aggiuntivi
        result.put("canPublish", permissionEvaluator.canPublishAttachment(processInstanceId));
        result.put("canUpdateAttachments", permissionEvaluator.canUpdateAttachment(processInstanceId, flowsUserDetailsService));


        return result;
    }


    public DataResponse search(Map<String, String> searchParams, String processDefinitionKey, boolean active, String order, int firstResult, int maxResults, boolean includeVariables) {

        HistoryService historyService = archiveProcessEngine.getHistoryService();
        HistoricProcessInstanceQuery processQuery = historyService.createHistoricProcessInstanceQuery();
        setSearchTerms(searchParams, processQuery);

//        List<String> authorities = Utils.getCurrentUserAuthorities();

        // solo l'admin e se sto facendo una query per "flussi avvaiti da me" IGNORO LE REGOLE DI VISIBILITÀ
//        if (!authorities.contains("ADMIN") || searchParams.containsKey(Utils.INITIATOR) ) {
//            processQuery.setVisibleToGroups(authorities);
//            processQuery.setVisibleToUser(SecurityContextHolder.getContext().getAuthentication().getName());
//        }

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

        List<HistoricProcessInstance> processesRaw;
        if (firstResult != -1 && maxResults != -1)
            processesRaw = processQuery.listPage(firstResult, maxResults);
        else
            processesRaw = processQuery.list();

        if(includeVariables) {
            processesRaw.stream().forEach(hpi -> {
                HistoricProcessInstanceEntity hpie = (HistoricProcessInstanceEntity) hpi; 
                List list = historyService.createHistoricVariableInstanceQuery().processInstanceId(hpie.getProcessInstanceId()).list();
                hpie.setQueryVariables(list);
            });
        }
        

        DataResponse response = new DataResponse();
        response.setStart(firstResult);
        response.setSize(processesRaw.size());// numero di task restituiti
        response.setTotal(processQuery.count()); //numero totale di task avviati da me
        response.setData(restResponseFactory.createHistoricProcessInstanceResponseList(processesRaw));

        return response;
    }


    private void setSearchTerms(Map<String, String> params, HistoricProcessInstanceQuery processQuery) {

        String title = params.remove("title");
        String titolo = params.remove(TITOLO);
        String initiator = params.remove(INITIATOR);
        String descrizione = params.remove(DESCRIZIONE);

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
            if (typevalue != null && typevalue.contains("=")) {
                String type = typevalue.substring(0, typevalue.indexOf('='));
                String value = typevalue.substring(typevalue.indexOf('=')+1);
                if(!value.isEmpty()) {
                    if (key.equals("businessKey")) {
                        processQuery.processInstanceBusinessKey(value);
                    } else {
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
                    }
                }
            }else {
                //per <input type="date"' non funziona "input-prepend" quindi rimetto la vecchia implementazione
                if ("startDateGreat".equals(key) || "startDateLess".equals(key)) {
                    processDate(processQuery, key, typevalue);
                }
            }
        });
    }

    private void processDate(HistoricProcessInstanceQuery processQuery, String key, String value) {
        // TODO remove deprecated api javax.xml
        Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(value);

        if (key.contains("Less"))
            processQuery.startedBefore(calendar.getTime());
        else if (key.contains("Great"))
            processQuery.startedAfter(calendar.getTime());
    }
}
