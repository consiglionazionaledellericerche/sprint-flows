package it.cnr.si.flows.ng.listeners;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.task.IdentityLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.service.FlowsMailService;
import it.cnr.si.repository.NotificationRuleRepository;
import it.cnr.si.service.MembershipService;

@Component
public class MailNotificationListener  implements ActivitiEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailNotificationListener.class);

    @Inject
    private FlowsMailService mailService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private MembershipService membershipService;
    @Inject
    private NotificationRuleRepository notificationService;

    @Override
    public void onEvent(ActivitiEvent event) {
        String executionId = event.getExecutionId();

        if (executionId != null) {
            sendNotification(event, executionId);
        }
    }


    private void sendNotification(ActivitiEvent event, String executionId) {

        ActivitiEventType type = event.getType();

        sendStandardCandidateNotification(event, type, executionId);
        sendRuleNotification(event, type, executionId);

    }


    private Map<String, Object> sendStandardCandidateNotification(ActivitiEvent event, ActivitiEventType type,
            String executionId) {
        Map<String, Object> variables = runtimeService.getVariables(executionId);

        if ( type == ActivitiEventType.TASK_CREATED ) {
            ActivitiEntityEvent taskEvent = (ActivitiEntityEvent) event;
            TaskEntity task = (TaskEntity) taskEvent.getEntity();
            Set<IdentityLink> candidates = ((TaskEntity)taskEvent.getEntity()).getCandidates();

            // Scenario predefinito: I membri dei gruppi assegnatari vengono notificati di un compito
            candidates.forEach(c -> {
                if (c.getGroupId() != null) {
                    List<String> members = membershipService.findMembersInGroup(c.getGroupId());
                    members.forEach(m -> {
                        mailService.sendTaskAvailableNotification(variables, task.getName(), m, c.getGroupId());
                    });
                }
            });
        }
        return variables;
    }

    private void sendRuleNotification(ActivitiEvent event, ActivitiEventType type, String executionId) {

        Map<String, Object> variables = runtimeService.getVariables(executionId);

        // Notifiche personalizzate
        String groupsString = null;

        String processDefinitionId = event.getProcessDefinitionId();
        String processDefinitionKey;
        String notificationType = null;

        switch (type) {
        case TASK_CREATED:
        case TASK_ASSIGNED:
        case TASK_COMPLETED:
            ActivitiEntityEvent taskEvent = (ActivitiEntityEvent) event;
            TaskEntity task = (TaskEntity) taskEvent.getEntity();
            processDefinitionKey = processDefinitionId.split(":")[0];
            groupsString = notificationService.findGroupsByProcessIdEventTypeTaskName(processDefinitionKey, type.toString(), task.getName());
            notificationType = FlowsMailService.TASK_NOTIFICATION;
            break;

        case SEQUENCEFLOW_TAKEN:

            processDefinitionKey = processDefinitionId.split(":")[0];
            groupsString = notificationService.findGroupsByProcessIdEventType(processDefinitionKey, type.toString());
            notificationType = FlowsMailService.FLOW_NOTIFICATION;
            break;

        case PROCESS_STARTED:
        case PROCESS_COMPLETED:
        case PROCESS_CANCELLED:
            processDefinitionKey = processDefinitionId.split(":")[0];
            groupsString = notificationService.findGroupsByProcessIdEventType(processDefinitionKey, type.toString());
            notificationType = FlowsMailService.PROCESS_NOTIFICATION;
            break;

        default:
            // no action
            break;
        }

        if (groupsString != null && notificationType != null) {
            String nt = notificationType;
            Stream.of(groupsString.split(","))
            .map(s -> s.trim())
            .forEach(groupVariableName -> {
                String groupValueName = (String) variables.get(groupVariableName);
                List<String> members = membershipService.findMembersInGroup(groupValueName);
                members.forEach(m -> {
                    mailService.sendNotificationRuleNotification(nt, variables, m, groupValueName);
                });
            });
        }
    }


    @Override
    public boolean isFailOnException() {
        // TODO Auto-generated method stub
        return false;
    }
}
