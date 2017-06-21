package it.cnr.si.flows.ng.service;

import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import it.cnr.si.service.MailService;

@Service
@Primary
public class FlowsMailService extends MailService {

    @Inject
    private TemplateEngine templateEngine;
    @Inject
    private MailService mailService;

    @Async
    public void sendTaskAvailableNotification(Map<String, Object> variables, String taskName, String username, String groupName) {

        Context ctx = new Context();
        ctx.setVariables(variables);
        ctx.setVariable("taskName", taskName);
        ctx.setVariable("username", username);
        ctx.setVariable("groupname", groupName);

        String htmlContent = templateEngine.process("taskAssegnatoAlGruppo.html", ctx);
        mailService.sendEmail("marcinireneusz.trycz@cnr.it", "Compito assegnato a uno dei tuoi gruppi", htmlContent, false, true);
    }

}
