package it.cnr.si.flows.ng.aop;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.stereotype.Component;

import it.cnr.si.security.SecurityUtils;

@Aspect
@Component
public class AuditingAspect {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private AuditEventRepository repo;

    @Pointcut("execution(* it.cnr.si.flows.ng.service.FlowsMailService.sendNotification(..))")
    private void inFlowsMailService() {}

    @AfterReturning("inFlowsMailService() && args(notificationType, variables, taskName, username, groupName)")
    public void auditMailsendSuccess(JoinPoint joinPoint, String notificationType, Map<String, Object> variables, String taskName, String username, String groupName) {

        Object[] args = joinPoint.getArgs();
        log.info("Arguments to sendNotification: "+ Arrays.toString(joinPoint.getArgs()));

        AuditEvent event = new AuditEvent(username, "EMAIL_SEND_SUCCESS", "groupName="+ groupName, "title="+ variables.get("title"), "notificationType="+ notificationType);
        repo.add(event);

    }

    @AfterThrowing("inFlowsMailService() && args(notificationType, variables, taskName, username, groupName)")
    public void auditMailsendFailure(JoinPoint joinPoint, String notificationType, Map<String, Object> variables, String taskName, String username, String groupName) {

        Object[] args = joinPoint.getArgs();
        log.info("Arguments to sendNotification: "+ Arrays.toString(joinPoint.getArgs()));

        AuditEvent event = new AuditEvent(username, "EMAIL_SEND_FAILURE", "groupName="+ groupName, "title="+ variables.get("title"), "notificationType="+ notificationType);
        repo.add(event);
    }

    // ---- //


    @Pointcut("execution(public * it.cnr.si.flows.ng.resource.*.*(..))")
    public void inFlowsEndpoints() {}

    @Before("inFlowsEndpoints() && @annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void auditFlowsEndointAction(JoinPoint joinPoint) {

        String username = SecurityUtils.getCurrentUserLogin();
        org.springframework.security.core.context.SecurityContext securityContext = org.springframework.security.core.context.SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        if (authentication.getAuthorities().contains(SwitchUserFilter.ROLE_PREVIOUS_ADMINISTRATOR))
            username = "admin as "+ username;

        String methodname = joinPoint.getSignature().getName();

        AuditEvent event = new AuditEvent(username, "REST_ACCESS", "methodname="+ methodname);
        repo.add(event);

    }



}
