package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.config.MailConfguration;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.CnrgroupService;
import it.cnr.si.service.FlowsUserService;
import it.cnr.si.service.MailService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Service
@Primary
public class FlowsMailService extends MailService {

    public static final String FLOW_NOTIFICATION = "notificaFlow.html";
    public static final String PROCESS_NOTIFICATION = "notificaProcesso.html";
    public static final String TASK_NOTIFICATION = "notificaTask.html";
    public static final String TASK_ASSEGNATO_AL_GRUPPO_HTML = "taskAssegnatoAlGruppo.html";
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
    @Inject
    private AceService aceService;
    @Inject
    private FlowsUserService flowsUserService;
    @Autowired
    private FlowsUserDetailsService flowsUserDetailsService;

    @Async
    public void sendFlowEventNotification(String notificationType, Map<String, Object> variables, String taskName, String username, final String groupName) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        ctx.setVariable("username", username);

        ctx.setVariable("taskLink", variables.get("serverUrl") + "/#/details?processInstanceId="+ variables.get("processInstanceId") +"&taskId="+ variables.get("nextTaskId"));
        ctx.setVariable("processLink", variables.get("serverUrl") + "/#/details?processInstanceId="+ variables.get("processInstanceId"));

        // ${serverUrl}/#/details?processInstanceId=${processInstanceId}&amp;taskId=${nextTaskId}}

        if (groupName != null) {
            if (Arrays.asList(env.getActiveProfiles()).contains("oiv")) {
                ctx.setVariable("profile", "oiv");
                ctx.setVariable("groupname", cnrgroupService.findDisplayName(groupName));
            } else {
                ctx.setVariable("groupname", Optional.ofNullable(aceBridgeService)
                        .flatMap(aceBridgeService -> Optional.ofNullable(groupName))
                        .map(s -> aceBridgeService.getExtendedGroupNome(s))
                        .orElse(groupName));
            }
        }
        ctx.setVariable("taskName", taskName);
        if (Arrays.asList(env.getActiveProfiles()).contains("oiv")) {
            ctx.setVariable("profile", "oiv");
        } else {
            ctx.setVariable("profile", "cnr");
        }

        String htmlContent = templateEngine.process(notificationType, ctx);
        String mailUtente = aceService.getUtente(username).getEmail();

        LOGGER.info("Invio della mail all'utente "+ username +" con indirizzo "+ mailUtente);

        if (!mailConfig.isMailActivated()) {
            // Per le prove mando *tutte* le email agli indirizzi di prova (e non ai veri destinatari)
            mailConfig.getMailRecipients().stream()
                    .filter(s -> !s.isEmpty())
                    .forEach(s -> {
                        LOGGER.debug("Invio mail a {} con titolo Notifica relativa al flusso {} del tipo {} nello stato {} e con contenuto {}",
                                s,
                                variables.get("key"),
                                notificationType,
                                variables.get("stato"),
                                StringUtils.abbreviate(htmlContent, 30));
                        LOGGER.trace("Corpo email per intero: {}", htmlContent);
                        sendEmail(s, "Notifica relativa al flusso " + variables.get("key"), htmlContent, false, true);
                    });
        } else {
            // In produzione mando le email ai veri destinatari
            if(mailUtente != null)
                sendEmail(mailUtente,
                        "Notifica relativa al flusso " + variables.get("key"),
                        htmlContent,
                        false,
                        true);
        }
    }
}