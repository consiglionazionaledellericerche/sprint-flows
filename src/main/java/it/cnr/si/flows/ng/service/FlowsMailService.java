package it.cnr.si.flows.ng.service;

import it.cnr.si.domain.Blacklist;
import it.cnr.si.flows.ng.config.MailConfguration;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.BlacklistService;
import it.cnr.si.service.CnrgroupService;
import it.cnr.si.service.FlowsUserService;
import it.cnr.si.service.MailService;
import it.cnr.si.service.MembershipService;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.cnr.si.flows.ng.utils.Utils.formatoDataUF;


@Service
@Primary
public class FlowsMailService extends MailService {

    public static final String FLOW_NOTIFICATION = "notificaFlow.html";
    public static final String PROCESS_NOTIFICATION = "notificaProcesso.html";
    public static final String PROCESS_COMPLETED_NOTIFICATION = "notificaProcessoCompletato.html";
    public static final String TASK_NOTIFICATION = "notificaTask.html";
    public static final String TASK_ASSEGNATO_AL_GRUPPO = "taskAssegnatoAlGruppo.html";
    public static final String TASK_IN_CARICO_ALL_UTENTE = "taskInCaricoAllUtente.html";
    public static final String NOTIFICA_RICORRENTE = "notificaRicorrente.html";

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsMailService.class);
    @Inject
    private TemplateEngine templateEngine;
    @Inject
    private MailConfguration mailConfig;
    @Inject
    private Environment env;
    @Inject
    private CnrgroupService cnrgroupService;
    @Autowired(required = false)
    private AceBridgeService aceBridgeService;
    @Autowired(required = false) //TODO
    private AceService aceService;
    @Inject
    private FlowsUserService flowsUserService;
    @Autowired
    private UserDetailsService flowsUserDetailsService;
    @Autowired
    private BlacklistService blacklistService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Autowired
    private MembershipService membershipService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private MailConfguration mailConfguration;

    @Async
    public void sendFlowEventNotification(String notificationType, Map<String, Object> variables, String taskName, String username, final String groupName, boolean hasNotificationRule) {
        try {

            LOGGER.info("Invio della mail all'utente "+ username);
            String key = (String)variables.get("key");

            Context ctx = new Context();
            ctx.setVariables(variables);
            ctx.setVariable("username", username);

            ctx.setVariable("taskLink", variables.get("serverUrl") + "/#/details?processInstanceId="+ variables.get("processInstanceId") +"&taskId="+ variables.get("nextTaskId"));
            ctx.setVariable("processLink", variables.get("serverUrl") + "/#/details?processInstanceId="+ variables.get("processInstanceId"));

            // ${serverUrl}/#/details?processInstanceId=${processInstanceId}&amp;taskId=${nextTaskId}}

            if (groupName != null) {
                if (Arrays.asList(env.getActiveProfiles()).contains("cnr")) {
                    ctx.setVariable("groupname", Optional.ofNullable(aceBridgeService)
                            .flatMap(aceBridgeService -> Optional.ofNullable(groupName))
                            .map(s -> aceBridgeService.getExtendedGroupNome(s))
                            .orElse(groupName));
                } else {
                    ctx.setVariable("profile", "oiv");
                    ctx.setVariable("groupname", cnrgroupService.findDisplayName(groupName));
                }
            }
            ctx.setVariable("taskName", taskName);
            if (Arrays.asList(env.getActiveProfiles()).contains("cnr")) {
                ctx.setVariable("profile", "cnr");
            } else if (Arrays.asList(env.getActiveProfiles()).contains("oiv")) {
                ctx.setVariable("profile", "oiv");
            } else if (Arrays.asList(env.getActiveProfiles()).contains("showcase")) {
                ctx.setVariable("profile", "showcase");
            }

            LOGGER.info("Recupero dell'email per l'utente "+ username);

            String htmlContent = templateEngine.process(notificationType, ctx);
            String mailUtente = aceService.getUtente(username).getEmail();

            LOGGER.info("Invio della mail all'utente "+ username +" con indirizzo "+ mailUtente);

            String subject = getCustomSubject(variables, key, hasNotificationRule);
            if (mailConfig.isMailActivated()) {
                // In produzione mando le email ai veri destinatari
                String procDefId = variables.get("processDefinitionId").toString().split(":")[0];
                Blacklist bl = blacklistService.findOneByEmailAndKey(mailUtente, procDefId);
                if (bl != null) {
                    LOGGER.info("L'utente {} ha richiesto di non ricevere notifiche per il flusso {}", mailUtente, key);
                } else {
                    if(mailUtente != null) {
                        sendEmail(mailUtente,
                                Optional.empty(),
                                Optional.empty(),
                                subject,
                                htmlContent,
                                false,
                                true);
                    } else {
                        LOGGER.warn("L'utente {} non ha un'email associata", username);
                    }
                }
            }

            // Per le prove mando *tutte* le email agli indirizzi di prova (e non ai veri destinatari)
            mailConfig.getMailRecipients().stream()
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> {
                        LOGGER.debug("Invio mail a {} con titolo Notifica relativa al flusso {} del tipo {} nello stato {} e con contenuto {}",
                                s,
                                key,
                                notificationType,
                                variables.get("stato"),
                                StringUtils.abbreviate(htmlContent, 30));
                        LOGGER.trace("Corpo email per intero: {}", htmlContent);
                        sendEmail(s, Optional.empty(), Optional.empty(), subject, htmlContent, false, true);
                    });
        } catch (Exception e) {
            LOGGER.error("Errore nell'invio della mail", e);
            throw e;
        }
    }

    private String getCustomSubject(Map<String, Object> variables, String key, boolean hasNotificationRule) {
        String subject;
        String processDefinition = ((String)variables.get("processDefinitionId")).split(":")[0];
        switch (processDefinition){
            case "covid19":
//Notifica FLUSSO Monitoraggio SW - PROGRAMMAZIONE maggio 2021 di massimo fraticelli
                subject = "Notifica FLUSSO Monitoraggio SW - " + variables.get("tipoAttivita") +
                        " " + variables.get("mese") + " " + variables.get("anno") +
                        " di " + variables.get("nomeCognomeUtente");
                break;
            case "missioni":
                String destinazione = (String) variables.get("destinazione");
                Object dataInizio = variables.get("dataInizioMissione");
                String sDataInizio;
                if (dataInizio instanceof Date) {
                    sDataInizio = formatoDataUF.format(dataInizio);
                } else {
                    sDataInizio = String.valueOf(dataInizio);
                }
//Notifica FLUSSO MISSIONI - ORDINE missione di massimo fraticelli in data 21-4-2020
                /* " + ordineMissione.getDestinazione()
                + " dal "+ DateUtils.getDefaultDateAsString(ordineMissione.getDataInizioMissione()) */
                subject = "Notifica FLUSSO MISSIONI - " + ((String)variables.get("tipologiaMissione")).toUpperCase() +" "+
                        variables.get("userNameUtenteMissione") +
                        (dataInizio != null ? " con inizio " + sDataInizio : "") + 
                        (destinazione != null ? " per "+ destinazione : "");
                break;
            case "accordi-internazionali-domande":
                if(hasNotificationRule)
//Notifica per sola conoscenza Flusso Accordi internazionali Domande - APROVAZIONE (Bando: CNR/CAS (Rep. Ceca) - triennio 2022-2024) di Massimo Fraticelli
                    subject = "Notifica per sola conoscenza FLUSSO Accordi Internazionali - " + variables.get("stato") +
                            " (Bando: " + variables.get("bando") +
                            ") di " + variables.get("nomeCognomeRichiedente");
                else
//Notifica Flusso Accordi internazionali Domande - APPROVAZIONE  ( Bando: CNR/CAS (Rep. Ceca) - triennio 2022-2024  ) di Massimo Fraticelli
                    subject = "Notifica FLUSSO Accordi Internazionali - " + variables.get("stato") +
                            " (Bando: " + variables.get("bando") +
                            ") di " + variables.get("nomeCognomeRichiedente");

                break;
            default:
                subject = "Notifica relativa al flusso " + key; //subject di default
        }
        return subject;
    }
    
    public void sendScheduledNotifications() {
        
        Map<String, List<ProcessInstance>> flussiPendentiPerUtente = new HashMap<>();
        
        List<ProcessInstance> activeInstances = runtimeService.createProcessInstanceQuery()
            .active()
            .processDefinitionKey("smart-working-domanda")
            .list();
        
        for (ProcessInstance activeInstance: activeInstances) {
            HistoricTaskInstance task = flowsProcessInstanceService.getCurrentTaskOfProcessInstance(activeInstance.getId());
            
            List<HistoricIdentityLink> identityLinksForProcessInstance = historyService.getHistoricIdentityLinksForTask(task.getId());
            identityLinksForProcessInstance.stream()
                .filter(il -> il.getType().equals("candidate"))
                .forEach(il -> {
                    if (il.getUserId() != null) {
                        flussiPendentiPerUtente.putIfAbsent(il.getUserId(), new ArrayList<ProcessInstance>());
                        flussiPendentiPerUtente.get(il.getUserId()).add(activeInstance);
                    } else if (il.getGroupId() != null) {
                        membershipService.getAllUsersInGroup(il.getGroupId()).forEach(user -> {
                            flussiPendentiPerUtente.putIfAbsent(user, new ArrayList<ProcessInstance>());
                            flussiPendentiPerUtente.get(user).add(activeInstance);
                        });
                    }
                });
        
        }
        
        flussiPendentiPerUtente.forEach((user, instances) -> {
            sendReminerToUserForInstances(user, instances);
        });
    }
    
    private void sendReminerToUserForInstances(String user, List<ProcessInstance> instances) {
        try {
            String mailUtente = aceService.getUtente(user).getEmail();
            LOGGER.info("Invio della mail all'utente "+ user +" con indirizzo "+ mailUtente +" per i flussi "+ instances);
            
            Context ctx = new Context();
            ctx.setVariable("instances", instances);
            ctx.setVariable("username", user);
            // ${serverUrl}/#/details?processInstanceId=${processInstanceId}&amp;taskId=${nextTaskId}}
            ctx.setVariable("serverUrl", mailConfguration.getMailUrl());
            List<String> instanceList = new ArrayList<String>();
            instances.forEach(i -> {
                String result = new String();
                JSONObject o = new JSONObject(i.getName());
                result = i.getBusinessKey() +" - "+ o.getString(Utils.TITOLO);
                instanceList.add(result);
            });
            ctx.setVariable("instanceList", instanceList);
            
            String htmlContent = templateEngine.process(NOTIFICA_RICORRENTE, ctx);

            String subject = "Notifica relativa ai flussi Smart Working";
            
            // Per le prove mando *tutte* le email agli indirizzi di prova (e non ai veri destinatari)
            mailConfig.getMailRecipients().stream()
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> {
                        LOGGER.debug("Invio mail di notifica ricorrente relativa ai flussi Smart Working a {} con contenuto {}",
                                s,
                                StringUtils.abbreviate(htmlContent, 30));
                        LOGGER.trace("Corpo email per intero: {}", htmlContent);
                        sendEmail(s, Optional.empty(), Optional.empty(), subject, htmlContent, false, true);
                    });
            
            if (mailConfig.isMailActivated()) {
                // In produzione mando le email ai veri destinatari
                Blacklist bl = blacklistService.findOneByEmailAndKey(mailUtente, "smart-working-domanda");
                if (bl != null) {
                    LOGGER.info("L'utente {} ha richiesto di non ricevere notifiche per il flusso smart-working-domanda", mailUtente);
                } else {
                    if(mailUtente != null) {
                        sendEmail(mailUtente,
                                Optional.empty(),
                                Optional.empty(),
                                subject,
                                htmlContent,
                                false,
                                true);
                    } else {
                        LOGGER.warn("L'utente {} non ha un'email associata", user);
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Errore nell'invio della mail", e);
            throw e;
        }
    }
        
}