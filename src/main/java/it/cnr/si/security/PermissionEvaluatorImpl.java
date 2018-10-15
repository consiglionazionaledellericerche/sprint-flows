package it.cnr.si.security;

import it.cnr.si.flows.ng.resource.FlowsProcessDefinitionResource;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.rest.service.api.RestResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.Role.*;


/**
 * The type Permission evaluator.
 *
 * @author Paolo Enrico Cirone
 */
@Component("permissionEvaluator")
@Primary
public class PermissionEvaluatorImpl implements PermissionEvaluator {


    public static final String CNR_CODE = "0000";
    private final Logger log = LoggerFactory.getLogger(PermissionEvaluatorImpl.class);

    @Inject
    TaskService taskService;
    @Inject
    private FlowsProcessDefinitionResource flowsProcessDefinitionResource;
    @Inject
    RuntimeService runtimeService;
    @Inject
    HistoryService historyService;
    @Inject
    RestResponseFactory restResponseFactory;



    /**
     * Determina se un utente ha i permessi per visualizzare un Task.
     * *
     *
     * @param taskId                  l'id del taskd
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei
     *                               una dipendenza ciclica visto che anche le classi che lo richiamano potrebbero averlo iniettato
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canVisualizeTask(String taskId, FlowsUserDetailsService flowsUserDetailsService) {
        Optional<String> username = Optional.of(SecurityUtils.getCurrentUserLogin());
        List<String> authorities = getAuthorities(username.orElse(""), flowsUserDetailsService);

        return taskService.getIdentityLinksForTask(taskId).stream()
                .filter(link -> link.getType().equals(IdentityLinkType.CANDIDATE) || link.getType().equals("assignee"))
                .anyMatch(link -> authorities.contains(link.getGroupId()) || username.get().equals(link.getUserId()));
    }


    /**
     * Verifico che l'utente possa completare un task
     * o far partire un flusso.
     *
     * @param req                     La req da cui prendo i vari parametri (per comodità i parametri non sono specificati nella signature del metodo che richiama la verifica)
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei
     *                               una dipendenza ciclica visto che anche le classi che lo richiamano potrebbero averlo iniettato
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canCompleteTaskOrStartProcessInstance(MultipartHttpServletRequest req, FlowsUserDetailsService flowsUserDetailsService) {
        String taskId = req.getParameter("taskId");

        if (taskId != null) {
            String username = SecurityUtils.getCurrentUserLogin();
            String assignee = taskService.createTaskQuery()
                    .taskId(taskId)
                    .singleResult().getAssignee();

            // l'utente puo' completare il task se e' lui l'assegnatario
            if (assignee != null) {
                return assignee.equals(username);

            } else {
                // Se l'assegnatario non c'e', L'utente deve essere nei gruppi candidati
                List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
                List<String> authorities =
                        getAuthorities(username, flowsUserDetailsService);

                return identityLinks.stream()
                        .filter(l -> l.getType().equals(IdentityLinkType.CANDIDATE))
                        .anyMatch(l -> authorities.contains(l.getGroupId()));
            }
        } else {
            //Se non è valorizzato il taskId allora sto "avviando" un nuovo workflow e verifico di averne i permessi
            String definitionKey = req.getParameter("processDefinitionId").split(":")[0];
            return flowsProcessDefinitionResource.canStartProcesByDefinitionKey(definitionKey);
        }
    }


    /**
     * Verifica che l'utente abbia la visibilità sulla Process Instance.
     *
     * @param processInstanceId       Il process instance id
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei una dipendenza
     *                               ciclica visto che anche le classi che richiamano questo metodo potrebbero averlo iniettato
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canVisualize(String processInstanceId, FlowsUserDetailsService flowsUserDetailsService) {
        String userName = SecurityUtils.getCurrentUserLogin();
        List<String> authorities = getAuthorities(userName, flowsUserDetailsService);

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processInstanceId(processInstanceId)
                .singleResult();

        return canVisualize((String) historicProcessInstance.getProcessVariables().get("idStruttura"),
                            historicProcessInstance.getProcessDefinitionKey(),
                            processInstanceId, authorities, userName);
    }


    //controllo che l'utente abbia un authorities di tipo "supervisore" o "responsabile" della struttura o del tipo di flusso
    private boolean verifyAuthorities(String idStruttura, String processDefinitionKey, List<String> authorities) {
        Boolean canVisualize = false;
        return authorities.stream()
                .anyMatch(
                        a -> a.contains(supervisore + "@" + CNR_CODE) ||
                        a.contains(responsabile + "@" + CNR_CODE) ||
                        a.contains(supervisoreStruttura + "@" + idStruttura) ||
                        a.contains(responsabileStruttura + "@" + idStruttura) ||
                        a.contains(supervisore + "#" + processDefinitionKey + "@" + CNR_CODE) ||
                        a.contains(responsabile + "#" + processDefinitionKey + "@" + CNR_CODE) ||
                        a.contains(supervisore + "#" + processDefinitionKey + "@" + idStruttura) ||
                        a.contains(responsabile + "#" + processDefinitionKey + "@" + idStruttura) ||
                        a.contains(supervisore + "#flussi@" + CNR_CODE) ||
                        a.contains(responsabile + "#flussi@" + CNR_CODE) ||
                        a.contains(supervisore + "#flussi@" + idStruttura) ||
                        a.contains(responsabile + "#flussi@" + idStruttura));
    }


    /**
     * Verifica che l'utente abbia la visibilità sulla Process Instance.
     *
     * @param idStruttura          the id struttura
     * @param authorities          le authorities dell'utente loggato
     * @param currentUserLogin             userName loggato
     * @return the boolean
     */

    public boolean canVisualize(String idStruttura, String processDefinitionKey, String processInstanceId, List<String> authorities, String currentUserLogin) {
        boolean canVisualize = false;

        if (authorities.contains("ADMIN") || verifyAuthorities(idStruttura, processDefinitionKey, authorities)) {
            canVisualize = true;
        } else {
            //controllo gli Identity Link "visualizzatore" (o "assignee" o "candidate") per gli user senza authorities di "supervisore" o "responsabile"
            Stream<HistoricIdentityLink> identityLinkStream = historyService.getHistoricIdentityLinksForProcessInstance(processInstanceId).stream();

            List<HistoricIdentityLink> ilv = identityLinkStream
                    .filter(il -> il.getType().equals("visualizzatore") || il.getType().equals("candidate") || il.getType().equals("assignee"))
                    .collect(Collectors.toList());
            //controllo gli Identity Link con userId(ad es.: rup in acquisti trasparenza)
            if (ilv.stream()
                    .filter(il -> il.getUserId() != null)
                    .anyMatch(il -> il.getUserId().equals(currentUserLogin))) {
                canVisualize = true;
            } else {
                //controllo gli Identity Link con groupId(tutti gli altri)
                //TODO controllo su supervisore/responsabile da rivedere
                if (ilv.stream()
                        .filter(il -> il.getGroupId() != null)
                        .filter(il -> !(il.getGroupId().startsWith(String.valueOf(responsabile)+"#") || il.getGroupId().startsWith(String.valueOf(supervisore)+"#") || il.getGroupId().startsWith(String.valueOf(responsabile)+"@") || il.getGroupId().startsWith(String.valueOf(supervisore)+"@") || il.getGroupId().startsWith(String.valueOf(responsabile)+"Struttura@") || il.getGroupId().startsWith(String.valueOf(supervisore)+"Struttura@")))
                        .anyMatch(il -> authorities.contains(il.getGroupId())))
                    canVisualize = true;
            }
        }
        return canVisualize;
    }


    /**
     * Verifica che l'utente possa assegnare un task a se stesso (prenderlo in carico)
     * o che possa rilasciarlo (restituirlo al gruppo assegnatario del task).
     *
     * @param taskId                  Il task id
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei
     *                               una dipendenza ciclica visto che anche le classi che lo richiamano potrebbero averlo iniettato
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canAssignTask(String taskId, FlowsUserDetailsService flowsUserDetailsService) {
        boolean result = false;
        String username = SecurityUtils.getCurrentUserLogin();
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        String assignee = task.getAssignee();

        if (assignee != null) {
            /*        unclaim         */
            // if has candidate groups or users -> can release
            boolean releasable = taskService.getIdentityLinksForTask(task.getId())
                    .stream()
                    .anyMatch(l -> l.getType().equals(IdentityLinkType.CANDIDATE));

            if (!releasable) {
                log.error("L'utente {} ha tentato di prendere in carico il  task {} CHE NON E' RILASCIABILE (non ha un gruppo candidate)", username, taskId);
            } else if (username.equals(assignee)) {
                result = true;
            }
        } else {
            /*        claim         */
            log.info("Do in carico il task {} a {}", taskId, username);
            List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
            List<String> authorities = getAuthorities(username, flowsUserDetailsService);

            boolean isInCandidates = identityLinks.stream()
                    .filter(l -> l.getType().equals(IdentityLinkType.CANDIDATE))
                    .anyMatch(l -> authorities.contains(l.getGroupId()));

            if (isInCandidates) {
                result = true;
            } else {
                log.error("L'utente {} ha tentato di prendere in carico il  task {} SENZA averne i permassi", username, taskId);
            }
        }
        return result;
    }



    /**
     * Verifico che l'utente che è anche "responsabile" del flusso possa assegnare
     * un task ad un utente che appartiene ai gruppi assegnatari dello stesso
     *
     * @param taskId   il task id
     * @param username lo username dell'utente a cui si vuole assegnare il task
     * @return risultato della verifica dei permessi (booleano)
     */
    /*        riassegnazione         */
    public boolean canAssignTask(String taskId, String username) {
        boolean result = false;
//        todo: da fare con Massimo?

///in caso di assegnazione ad altro utente
//        (può farlo solo il responsabile di struttura per fluso ->
//        responsabili#flusso x @ struttura
//        responsabili@ struttura (se è un flusso di struttura, c'è una variabile "vincolata" nel flusso)
//        responsabili#flusso x @ generico (flusso definito per più strutture)

//        può essere assegnato solo a membri dei gruppi candidate del task in esecuzione
        return result;
    }

    public boolean canUpdateAttachment(String processInstanceId, FlowsUserDetailsService flowsUserDetailsService) {

        if(SecurityUtils.isCurrentUserInRole("ROLE_ADMIN"))
            return true;

        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        String processDefinitionKey = instance.getProcessDefinitionKey();
        String idStruttura = runtimeService.getVariable(processInstanceId, "idStruttura", String.class);

        String username = SecurityUtils.getCurrentUserLogin();
        List<String> authorities = getAuthorities(username, flowsUserDetailsService);


        return authorities.stream()
                .anyMatch(
                        a ->    a.contains(responsabile + "@" + CNR_CODE) ||
                                a.contains(responsabileStruttura + "@" + idStruttura) ||
                                a.contains(responsabile + "#" + processDefinitionKey + "@" + CNR_CODE) ||
                                a.contains(responsabile + "#" + processDefinitionKey + "@" + idStruttura) ||
                                a.contains(responsabile + "#flussi@" + CNR_CODE) ||
                                a.contains(responsabile + "#flussi@" + idStruttura));
    }

    public boolean canPublishAttachment(String processInstanceId) {

        if(SecurityUtils.isCurrentUserInRole("ROLE_ADMIN"))
            return true;

        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        String username = SecurityUtils.getCurrentUserLogin();

        if (instance.getProcessDefinitionKey().equals(acquisti.getValue())) {

            String rup = runtimeService.getVariable(processInstanceId, "rup", String.class);
            if (username.equals(rup))
                return true;

            List<String> authorities = it.cnr.si.flows.ng.utils.SecurityUtils.getCurrentUserAuthorities();
            String idStruttura = String.valueOf(instance.getProcessVariables().get("idStruttura"));
            String nomeGruppoFirma = "responsabileFirmaAcquisti@" + idStruttura;

            if (authorities.contains(nomeGruppoFirma))
                return true;
        }

        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return true;
    }


    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }


    private List<String> getAuthorities(String username, FlowsUserDetailsService flowsUserDetailsService) {
        return flowsUserDetailsService.loadUserByUsername(username).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .collect(Collectors.toList());
    }
}