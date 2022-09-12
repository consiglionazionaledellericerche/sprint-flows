package it.cnr.si.flows.ng.aop;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import it.cnr.si.service.SecurityService;

import javax.inject.Inject;
import java.util.Map;

import static it.cnr.si.flows.ng.service.FlowsPdfService.TITLE;

@Aspect
@Component
public class AuditingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private AuditEventRepository repo;
    @Inject
    private SecurityService securityService;

    @Pointcut("execution(* it.cnr.si.flows.ng.service.FlowsMailService.sendFlowEventNotification(..))")
    private void inFlowsMailService() {}

    @Before("inFlowsMailService() && args(notificationType, variables, taskName, username, groupName)")
    public void auditMailSendAttempt(JoinPoint joinPoint, String notificationType, Map<String, Object> variables, String taskName, String username, String groupName) {
        Object[] args = joinPoint.getArgs();

        log.info("Tentativo di inviare la mail {} a {} del gruppo {}, task {}", notificationType, username, groupName, taskName );

        AuditEvent event = new AuditEvent(username, "EMAIL_SEND_ATTEMPT", "username="+ username, "groupName="+ groupName, "title="+ variables.get("title"), "notificationType="+ notificationType);
        repo.add(event);
    }

    @AfterReturning("inFlowsMailService() && args(notificationType, variables, taskName, username, groupName)")
    public void auditMailSendSuccess(JoinPoint joinPoint, String notificationType, Map<String, Object> variables, String taskName, String username, String groupName) {

        Object[] args = joinPoint.getArgs();

        log.info("Tentativo di inviare la mail riuscito {} a {} del gruppo {}, task {}", notificationType, username, taskName, groupName);

        AuditEvent event = new AuditEvent(username, "EMAIL_SEND_SUCCESS", "username="+ username, "groupName="+ groupName, "title="+ variables.get("title"), "notificationType="+ notificationType);
        repo.add(event);

    }

    @AfterThrowing(pointcut="inFlowsMailService() && args(notificationType, variables, taskName, username, groupName)", 
    	    throwing="excep")
    public void auditMailsendFailure(JoinPoint joinPoint, String notificationType, Map<String, Object> variables, String taskName, String username, String groupName, Throwable excep) {

        Object[] args = joinPoint.getArgs();

        log.info("Tentativo di inviare la mail fallito {} a {} del gruppo {}, task {}", notificationType, username, taskName, groupName);
        log.error(excep.getMessage(), excep);
        
        AuditEvent event = new AuditEvent(username, "EMAIL_SEND_FAILURE", "username="+ username, "groupName=" + groupName, TITLE + "=" + variables.get(TITLE), "notificationType=" + notificationType);
        repo.add(event);
    }

    // ---- //


    @Pointcut("execution(public * it.cnr.si.flows.ng.resource.*.*(..))")
    public void inFlowsEndpoints() {}

//    @Before("inFlowsEndpoints() && @annotation(org.springframework.web.bind.annotation.RequestMapping)")
//    public void auditFlowsEndointAction(JoinPoint joinPoint) {
//
//        String username = securityService.getCurrentUserLogin();
//        org.springframework.security.core.context.SecurityContext securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
//        Authentication authentication = securityContext.getAuthentication();
//        if (authentication.getAuthorities().contains(SwitchUserFilter.ROLE_PREVIOUS_ADMINISTRATOR))
//            username = "admin as "+ username;
//
//        String methodname = joinPoint.getSignature().getName();
//
//        AuditEvent event = new AuditEvent(username, "REST_ACCESS", "methodname="+ methodname);
//        repo.add(event);
//
//    }



}
