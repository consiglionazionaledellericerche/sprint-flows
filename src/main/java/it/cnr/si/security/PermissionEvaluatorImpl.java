package it.cnr.si.security;

import it.cnr.si.flows.ng.resource.FlowsProcessDefinitionResource;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
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


/**
 * The type Permission evaluator.
 *
 * @author Paolo Enrico Cirone
 */
@Component("permissionEvaluator")
@Primary
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    private final Logger LOGGER = LoggerFactory.getLogger(PermissionEvaluatorImpl.class);

    @Inject
    TaskService taskService;
    @Inject
    private FlowsProcessDefinitionResource flowsProcessDefinitionResource;


    public PermissionEvaluatorImpl() {
    }


    /**
     * Determino se un utente ha i permessi per visualizzare un Task.
     * *
     *
     * @param taskId                  l'id del taskd
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei                                una dipendenza ciclica visto che anche le classi che lo richiamano potrebbero averlo iniettato
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canVisualizeTask(String taskId, FlowsUserDetailsService flowsUserDetailsService) {
        Optional<String> username = Optional.of(SecurityUtils.getCurrentUserLogin());
        List<String> authorities = flowsUserDetailsService.loadUserByUsername(username.orElse("")).getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .collect(Collectors.toList());
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        return identityLinks.stream().anyMatch(link -> authorities.contains(link.getGroupId()));
    }


    /**
     * Verifico che l'utente possa completare un task
     * o far partire un flusso.
     *
     * @param req                     La req da cui prendo i vari parametri (per comodità i parametri non sono specificati nella signature del metodo che richiama la verifica)
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei
     * @return risultato della verifica dei permessi (booleano)
     */
    public boolean canCompleteTaskOrStartProcessInstance(MultipartHttpServletRequest req, FlowsUserDetailsService flowsUserDetailsService) {
        String taskId = (String) req.getParameter("taskId");
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
                        flowsUserDetailsService.loadUserByUsername(username).getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .map(Utils::removeLeadingRole)
                                .collect(Collectors.toList());

                return identityLinks.stream()
                        .filter(l -> l.getType().equals("candidate"))
                        .anyMatch(l -> authorities.contains(l.getGroupId()));
            }
        } else {
            //Se non è valorizzato il taskId allora sto "avviando" un nuovo workflow e verifico di averne i permessi
            String definitionKey = (String) req.getParameter("processDefinitionId").split(":")[0];
            return flowsProcessDefinitionResource.canStartProcesByDefinitionKey(definitionKey);
        }
    }


    /**
     * Verifico che l'utente possa assegnare un task a se stesso (prenderlo in  carico)
     * o che possa rilasciarlo (restituirlo al gruppo assegnatario del task).
     *
     * @param taskId                  Il task id
     * @param flowsUserDetailsService flowsUserDetailService: non posso "iniettarlo" nella classe perchè altrimenti avrei
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
                LOGGER.error("L'utente {} ha tentato di prendere in carico il  task {} CHE NON E' RILASCIABILE (non ha un gruppo candidate)", username, taskId);
            } else if (username.equals(assignee)) {
                result = true;
            }
        } else {
            /*        claim         */
            LOGGER.info("Do in carico il task {} a {}", taskId, username);
            List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
            List<String> authorities = flowsUserDetailsService.loadUserByUsername(username).getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(Utils::removeLeadingRole)
                    .collect(Collectors.toList());

            boolean isInCandidates = identityLinks.stream()
                    .filter(l -> l.getType().equals("candidate"))
                    .anyMatch(l -> authorities.contains(l.getGroupId()));

            if (isInCandidates) {
                result = true;
            } else {
                LOGGER.error("L'utente {} ha tentato di prendere in carico il  task {} SENZA averne i permassi", username, taskId);
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
        boolean resut = false;
//        todo: da fare insieme all'interfaccia grafica da cui richiamare la funzione

///in caso di assegnazione ad altro utente
//        (può farlo solo il responsabile di struttura per fluso ->
//        responsabili#flusso x @ struttura
//        responsabili@ struttura (se è un flusso di struttura, c'è una variabile "vincolata" nel flusso)
//        responsabili#flusso x @ generico (flusso definito per più strutture)

//        può essere assegnato solo a membri dei gruppi candidate del task in esecuzione
        return resut;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return true;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}