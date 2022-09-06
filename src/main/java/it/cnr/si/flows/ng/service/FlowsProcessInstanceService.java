package it.cnr.si.flows.ng.service;

import com.opencsv.CSVWriter;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.repository.FlowsHistoricProcessInstanceQuery;

import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.SecurityService;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.cnr.si.flows.ng.utils.Enum.Stato.Annullato;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Revocato;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.*;
import static it.cnr.si.flows.ng.utils.Utils.*;

/**
 * Created by cirone on 15/06/17.
 */
@Service
public class FlowsProcessInstanceService {

    public static final Map<String, String> processiRevocabili = new HashMap<String, String>() {{
       // put("smart-working-domanda", "smart-working-revoca");
    }};
    
    public static final Map<String, List<String>> abilitatiAllaRevoca = new HashMap<String, List<String>>() {{
        put("smart-working-domanda", Arrays.asList("rs", "responsabile-struttura"));
    }};
    
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
    private UserDetailsService flowsUserDetailsService;
    @Inject
    private Utils utils;
    @Inject
    private FlowsAttachmentService attachmentService;
    @Inject
    private MembershipService membershipService;
    @Inject
    private SecurityService securityService;
    
    
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

    public Map<String, Object> getProcessInstanceWithDetails(String processInstanceId, boolean whitTaskList) {
        Map<String, Object> result = new HashMap<>();

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

            List<String> values = getLinkedProcessIds(processInstanceId);

            List<Map<String, Object>> linkedFlows = new ArrayList<>();

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

        // ProcessDefinition (static) metadata
        ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

        final Map<String, Object> identityLinks = new LinkedHashMap<>();
        Map<String, Object> processLinks = new HashMap<>();
        processLinks.put("links", restResponseFactory.createHistoricIdentityLinkResponseList(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceId)));
        identityLinks.put("process", processLinks);

        result.put("identityLinks", identityLinks);

        // permessi aggiuntivi
        result.put("canPublish", permissionEvaluator.canPublishAttachment(processInstanceId));
        result.put("canUpdateAttachments", permissionEvaluator.canUpdateAttachment(processInstanceId, flowsUserDetailsService));
        result.put("isRevocabile", isRevocabile(processInstanceId));
        
        if(whitTaskList){
            result.put("history", getHistoryForPi(processInstanceId));
        }
        return result;
    }


    public ArrayList<Map<String, Object>> getHistoryForPi(String processInstanceId){
        // PrecessInstance metadata
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        final Map<String, Object> identityLinks = new LinkedHashMap<>();


        ReadOnlyProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).
                getDeployedProcessDefinition(processInstance.getProcessDefinitionId());

        List<HistoricTaskInstance> taskList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeTaskLocalVariables()
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

        return history;
    }


    public DataResponse search(Map<String, String> searchParams, String processDefinitionKey, boolean active, String order, int firstResult, int maxResults, boolean includeVariables) {

        FlowsHistoricProcessInstanceQuery processQuery = new FlowsHistoricProcessInstanceQuery(managementService);

        if (firstResult != -1 && maxResults != -1) {
            processQuery.setFirstResult(firstResult);
            processQuery.setMaxResults(maxResults);
        }
        setSearchTerms(searchParams, processQuery);

        List<String> authorities = securityService.getUser().get().getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // solo l'admin e se sto facendo una query per "flussi avviati da me" IGNORO LE REGOLE DI VISIBILITÀ
        if (!authorities.contains("ADMIN") || searchParams.containsKey(Utils.INITIATOR) ) {
            processQuery.setVisibleToGroups(authorities);
            processQuery.setVisibleToUser(securityService.getCurrentUserLogin());
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


    public void setSearchTerms(Map<String, String> params, FlowsHistoricProcessInstanceQuery processQuery) {

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
            List<RestVariable> variables = pi.getVariables();
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

    public HistoricProcessInstanceQuery getProcessInstancesForURP(int terminiRicorso, Boolean avvisiScaduti, Boolean gareScadute, String order) {

        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processDefinitionKey("acquisti")
                .or()
                .variableValueNotEquals(statoFinaleDomanda.name(), Annullato.name())
                .variableValueNotEquals(statoFinaleDomanda.name(), Revocato.name())
                .endOr();
        Calendar dataTerminiRicorso = Calendar.getInstance();
        dataTerminiRicorso.setTime(new Date());

        String now = utils.formattaData(new Date());
        if(gareScadute != null){
            historicProcessInstanceQuery
            .variableValueLike("strumentoAcquisizione", "PROCEDURA SELETTIVA%");
            if(gareScadute){
                // GARE SCADUTE IN ATTESA DI ESITO: data scadenza presentazione offerta < NOW  && data scadenza presentazione offerta - termini di ricorso >= NOW
                if(terminiRicorso != 0)
                    dataTerminiRicorso.add(Calendar.DAY_OF_MONTH, -terminiRicorso);

                historicProcessInstanceQuery
                .variableValueLessThan(dataScadenzaBando.name(), now)
                .variableValueGreaterThanOrEqual(dataScadenzaBando.name(), utils.formattaData(dataTerminiRicorso.getTime()));
                LOGGER.info("SCADUTE IN ATTESA DI ESITO nr flussi {}", historicProcessInstanceQuery.count());

            } else {
                // GARE IN CORSO data scadenza presentazione offerta >= NOW
                historicProcessInstanceQuery
                .variableValueGreaterThanOrEqual(dataScadenzaBando.name(), now);
                LOGGER.info("GARE IN CORSO nei flussi {}", historicProcessInstanceQuery.count());

            }
        }

        if(avvisiScaduti != null){
            if(avvisiScaduti){
                // AVVISI SCADUTI: data scadenza presentazione offerta  < NOW && data scadenza presentazione offerta + terminiRicorso >= NOW
                dataTerminiRicorso.add(Calendar.DAY_OF_MONTH, -terminiRicorso);

                historicProcessInstanceQuery
                .variableValueLessThan(dataScadenzaAvvisoPreDetermina.name(), now)
                .variableValueGreaterThanOrEqual(dataScadenzaAvvisoPreDetermina.name(), utils.formattaData(dataTerminiRicorso.getTime()));
            }else{
                // AVVISI IN CORSO: data scadenza presentazione offerta >= NOW
                historicProcessInstanceQuery
                .variableValueGreaterThanOrEqual(dataScadenzaAvvisoPreDetermina.name(), now);
            }
        }
        utils.orderProcess(order, historicProcessInstanceQuery);

        return historicProcessInstanceQuery;
    }



    public HistoricProcessInstanceQuery getProcessInstancesForTrasparenza(String order, String searchField) {

        HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processDefinitionKey("acquisti")
                .unfinished()
                .variableValueEquals(flagIsTrasparenza.name(), "true")
                .or()
                .variableValueLikeIgnoreCase("nomeStruttura", "%" + searchField + "%")
                .variableValueLikeIgnoreCase("cig", "%" + searchField + "%")
                .variableValueLikeIgnoreCase("descrizione", "%" + searchField + "%") //todo: descrizione
                .endOr();

        utils.orderProcess(order, historicProcessInstanceQuery);

        return historicProcessInstanceQuery;
    }

    /** 
     * Una domanda accettata e' revocabile se
     * 1. e' accettata (il flusso e' concluso)
     * 2. il flusso e' di tipo revocabile
     * 3. l'utente loggato e' il boss del richiedente
     * 4. la domanda non e' stata gia' revocata
     */
    public boolean isRevocabile(String processInstanceId) {
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
        
        // 1. e' accettata (il flusso e' concluso)
        if (processInstance.getEndTime() == null)
            return false;
        if (!"VALIDATA".equals(processInstance.getProcessVariables().get("statoFinaleDomanda")) && !"PRESA_VISIONE".equals(processInstance.getProcessVariables().get("statoFinaleDomanda")))
            return false;
        
        // 2. il flusso e' di tipo revocabile
        if (!processiRevocabili.containsKey(processInstance.getProcessDefinitionKey()))
            return false;
        
        // 3. l'utente loggato e' abilitato alla Revoca
        String currentUser = securityService.getCurrentUserLogin();
        Set<String> allRolesForUser = membershipService.getAllRolesForUser(currentUser);
        String idAceStrutturaDomandaRichiedente = String.valueOf(processInstance.getProcessVariables().get("idAceStrutturaDomandaRichiedente"));
        if ( abilitatiAllaRevoca.get(processInstance.getProcessDefinitionKey()).stream()
                .noneMatch(ruoloRevoca -> allRolesForUser.contains(ruoloRevoca + "@" + idAceStrutturaDomandaRichiedente)) )
            return false;
        
        // 4. la domanda non e' stata gia' revocata e non è in corso di revoca
        List<HistoricProcessInstance> revoche = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processiRevocabili.get(processInstance.getProcessDefinitionKey()))
                .variableValueEquals("idDomanda", processInstance.getProcessVariables().get("idDomanda"))
                .includeProcessVariables()
                .list();
        for (HistoricProcessInstance revoca : revoche) {
            String statoFinale = (String) revoca.getProcessVariables().get("statoFinaleDomanda");
            boolean isInCorsoDiRevoca = "APERTA".equals(statoFinale);
            boolean isGiaRevocata = "REVOCATA".equals(statoFinale);
            if (isInCorsoDiRevoca || isGiaRevocata)
                return false;
        }

        // se nessuno dei controlli è fallito, il flusso è revocabile
        return true;
    }

    
    private List<String> getLinkedProcessIds(String processInstanceId) {
        
        HistoricVariableInstance links = historyService
            .createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName("linkToOtherWorkflows")
            .excludeTaskVariables()
            .singleResult();
        
        if (links != null) {

            List<Map<String, Object>> linkedFlows = new ArrayList<>();
            String value = (String) links.getValue();
            String[] values = value.split(",");
            
            return Arrays.asList(values);

        } else {
            return new ArrayList<String>();
        }
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
