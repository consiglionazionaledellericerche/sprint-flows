package it.cnr.si.security;

import it.cnr.si.flows.ng.resource.FlowsProcessDefinitionResource;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.SecurityService;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.rest.service.api.RestResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    public static final String ID_STRUTTURA = "idStruttura";
    public static final String ID_STRUTTURE_SUPERVISIONE = "idStruttureSupervisione";
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
    @Autowired
    private MembershipService membershipService;
    @Inject
    private SecurityService securityService;

    /**
     * Determina se un utente ha i permessi per visualizzare un Task.
     * *
     *
     * @param taskId                  l'id del taskd
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei
     *                               una dipendenza ciclica visto che anche le classi che lo richiamano potrebbero averlo iniettato
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canVisualizeTask(String taskId, org.springframework.security.core.userdetails.UserDetailsService flowsUserDetailsService) {
        Optional<String> username = Optional.of(securityService.getCurrentUserLogin());
        List<String> authorities = getAuthorities(username.orElse("")).stream()
                .map(Utils::removeImportoSpesa) // Non mi interessa filtrare per importo spesa
                .collect(Collectors.toList());

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
    public boolean canCompleteTaskOrStartProcessInstance(MultipartHttpServletRequest req, org.springframework.security.core.userdetails.UserDetailsService flowsUserDetailsService) {
        String taskId = req.getParameter("taskId");

        if (taskId != null) {
            return canCompleteTask(taskId, flowsUserDetailsService);
        } else {
            //Se non è valorizzato il taskId allora sto "avviando" un nuovo workflow e verifico di averne i permessi
            String definitionKey = req.getParameter("processDefinitionId").split(":")[0];
            return flowsProcessDefinitionResource.canStartProcesByDefinitionKey(definitionKey);
        }
    }

    public boolean canCompleteTask(String taskId, org.springframework.security.core.userdetails.UserDetailsService flowsUserDetailsService) {
        String username = securityService.getCurrentUserLogin();
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        if (task == null)
            return false;
        String assignee = task.getAssignee();
        List<String> authorities =
                getAuthorities(username);
        
        // l'utente puo' completare il task se e' lui l'assegnatario 
        // oppure se l'importo spesa della sua delega è superiore all'importo del flusso acquisti
        if (assignee != null) {
            return assignee.equals(username);
        } else {
            return isCandidatoDiretto(taskId, authorities) || canCompleteImportoSpesa(taskId, authorities);
        }
    }

    public boolean isCandidatoDiretto(String taskId, List<String> authorities) {
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        String username = securityService.getCurrentUserLogin();
        return identityLinks.stream()
                .filter(l -> l.getType().equals(IdentityLinkType.CANDIDATE))
                .anyMatch(l -> authorities.contains(l.getGroupId()) || username.equals(l.getUserId()));
    }

    public boolean canCompleteImportoSpesa(String taskId, List<String> authorities) {

        authorities = authorities.stream()
                .filter(role -> role.contains("$"))
                .collect(Collectors.toList());

        for (String authority : authorities) {
            String authoritySenzaImporto = Utils.removeImportoSpesa(authority);
            double importoLimite = Utils.getImportoSpesa(authority);
        
            double importo = taskService.getVariable(taskId, "importoTotaleLordo", Double.class);
            boolean isCandidateFirma = taskService.getIdentityLinksForTask(taskId)
                    .stream()
                    .anyMatch(il -> authoritySenzaImporto.equals(il.getGroupId()) && il.getType().equals(IdentityLinkType.CANDIDATE));
            boolean importoSuperiore = importoLimite >= importo;
            
            if (isCandidateFirma && importoSuperiore)
                return true; 
        }
        return false;
    }

    /**
     * Verifica che l'utente abbia la visibilità sulla Process Instance.
     *
     * @param processInstanceId       Il process instance id
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei una dipendenza
     *                               ciclica visto che anche le classi che richiamano questo metodo potrebbero averlo iniettato
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canVisualize(String processInstanceId, org.springframework.security.core.userdetails.UserDetailsService flowsUserDetailsService) {
        String userName = securityService.getCurrentUserLogin();
        List<String> authorities = getAuthorities(userName);

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processInstanceId(processInstanceId)
                .singleResult();

        return canVisualize((String) historicProcessInstance.getProcessVariables().get(ID_STRUTTURA),
                            historicProcessInstance.getProcessDefinitionKey(),
                            processInstanceId, authorities, userName);
    }


    //controllo che l'utente abbia un authorities di tipo "supervisore" o "responsabile" della struttura o del tipo di flusso
    private boolean verifyAuthorities(String idStruttura, String processDefinitionKey, List<String> authorities) {
        return authorities.stream()
                .anyMatch(
                        a -> a.contains(supervisore + "@" + CNR_CODE) ||
                                a.contains(responsabile + "@" + CNR_CODE) ||
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
     * @param processDefinitionKey      key
     * @param processInstanceId         id
     * @return the boolean
     */

    public boolean canVisualize(String idStruttura, String processDefinitionKey, String processInstanceId, List<String> authorities, String currentUserLogin) {

        if (authorities.contains("ADMIN") || verifyAuthorities(idStruttura, processDefinitionKey, authorities)) {
            return true;
            
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
                return true;
            } else {
                
                if (ilv.stream()
                        .filter(il -> il.getGroupId() != null)
                        .anyMatch(il -> authorities.contains(il.getGroupId())))
                    return true;
            }
        }
        return false;
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
    public boolean canClaimTask(String taskId, org.springframework.security.core.userdetails.UserDetailsService flowsUserDetailsService) {
        boolean result = false;
        String username = securityService.getCurrentUserLogin();
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
            boolean isInCandidates = false;
            try {
                List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);

                List<String> authorities = getAuthorities(username);

                isInCandidates = isCandidatoDiretto(taskId, authorities) || canCompleteImportoSpesa(taskId, authorities);

            } catch (Exception e){
                log.error("Errore nel recupero degli identity links della Task Id {} ", taskId);
            }
            if (isInCandidates) {
                result = true;
            } else {
                log.error("L'utente {} ha tentato di prendere in carico il  task {} SENZA averne i permessi", username, taskId);
            }
        }
        return result;
    }



    public boolean isResponsabile(String taskId, String processInstanceId, org.springframework.security.core.userdetails.UserDetailsService flowsUserDetailsService) {
        String user = securityService.getCurrentUserLogin();
        Set<String> groups = membershipService.getAllRolesForUser(user);
        Task task;
        if(!processInstanceId.isEmpty()){
            task = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .includeProcessVariables()
                    .singleResult();
        } else{
            task = taskService.createTaskQuery()
                    .taskId(taskId)
                    .includeProcessVariables()
                    .singleResult();
        }

        String idStruttura = (String) ((HashMap) task.getProcessVariables()).get(ID_STRUTTURA);

        String tipoFlusso = task.getProcessDefinitionId().split(":")[0];

        return (groups.contains("responsabile#flussi") ||
                groups.contains("responsabile#" + tipoFlusso + "@" + CNR_CODE)||
                groups.contains("responsabile#" + tipoFlusso + "@" + idStruttura));
    }



    public boolean canUpdateAttachment(String processInstanceId, org.springframework.security.core.userdetails.UserDetailsService flowsUserDetailsService) {

        if(securityService.isCurrentUserInRole("ROLE_ADMIN"))
            return true;

        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
        boolean isActive = instance.getEndTime() == null;

        String processDefinitionKey = instance.getProcessDefinitionKey();
        String idStruttura = String.valueOf(instance.getProcessVariables().get(ID_STRUTTURA));

        String username = securityService.getCurrentUserLogin();
        List<String> authorities = getAuthorities(username);

        boolean isResponsabile = authorities.stream()
                .anyMatch(
                        a -> a.contains(responsabile + "@" + CNR_CODE) ||
                                a.contains(responsabile + "#" + processDefinitionKey + "@" + CNR_CODE) ||
                                a.contains(responsabile + "#" + processDefinitionKey + "@" + idStruttura) ||
                                a.contains(responsabile + "#flussi@" + CNR_CODE) ||
                                a.contains(responsabile + "#flussi@" + idStruttura));
        boolean isRuoloFlusso = false;

        if (instance.getProcessDefinitionKey().equals(acquisti.getProcessDefinition())) {

            String rup = String.valueOf(instance.getProcessVariables().get("rup"));
            String nomeGruppoFirma = "responsabileFirmaAcquisti@" + idStruttura;

            isRuoloFlusso = username.equals(rup) || authorities.contains(nomeGruppoFirma);
        }

        return (isResponsabile || isRuoloFlusso) && isActive;
    }

    public boolean canPublishAttachment(String processInstanceId) {

        if(securityService.isCurrentUserInRole("ROLE_ADMIN"))
            return true;

        HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult();
        String username = securityService.getCurrentUserLogin();

        if (instance.getProcessDefinitionKey().equals(acquisti.getProcessDefinition())) {

            String rup = String.valueOf(instance.getProcessVariables().get("rup"));
            if (username.equals(rup))
                return true;

            String idStruttura = String.valueOf(instance.getProcessVariables().get(ID_STRUTTURA));
            String nomeGruppoFirma = "responsabileFirmaAcquisti@" + idStruttura;
            if (securityService.isCurrentUserInRole(nomeGruppoFirma))
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


    private List<String> getAuthorities(String username) {
        return securityService.getUser().get().getAuthorities().stream().map(String::valueOf).collect(Collectors.toList());
    }
}