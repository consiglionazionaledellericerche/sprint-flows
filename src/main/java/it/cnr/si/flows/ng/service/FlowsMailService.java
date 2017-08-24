package it.cnr.si.flows.ng.service;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import it.cnr.si.flows.ng.config.MailConfguration;
import it.cnr.si.service.MailService;

@Service
@Primary
public class FlowsMailService extends MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsMailService.class);

    public static final String FLOW_NOTIFICATION = "notificaFlow.html";
    public static final String PROCESS_NOTIFICATION = "notificaProcesso.html";
    public static final String TASK_NOTIFICATION = "notificaTask.html";
    public static final String TASK_ASSEGNATO_AL_GRUPPO_HTML = "taskAssegnatoAlGruppo.html";

    @Inject
    private TemplateEngine templateEngine;
    @Inject
    private MailConfguration mailConfig;
    @Inject
    private AceBridgeService aceService;
    
    @Async
    public void sendFlowEventNotification(String notificationType, Map<String, Object> variables, String taskName, String username, String groupName) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        ctx.setVariable("username", username);
        if(groupName != null){
            String groupDenominazione = aceService.getExtendedGroupNome(groupName);
            groupName = groupDenominazione;
        }
        ctx.setVariable("groupname", groupName);
        ctx.setVariable("taskName", taskName);

        String htmlContent = templateEngine.process(notificationType, ctx);

        LOGGER.info("Invio mail a {} con titolo {} del tipo {} e con contenuto {}", username+"@cnr.it", "Notifica relativa al flusso "+ variables.get("title"), notificationType, htmlContent);

        if (!mailConfig.isMailActivated()) {
            mailConfig.getMailRecipients()
            .forEach(r -> {
                sendEmail(r, "Notifica relativa al flusso "+ variables.get("title"), htmlContent, false, true);
            });
        } else {
            // TODO recuperare la mail da LDAP (vedi issue #66)
            // TODO scommentare per la produzione
//            sendEmail(username, "Notifica relativa al flusso "+ variables.get("title"), htmlContent, false, true);
        }
    }
}