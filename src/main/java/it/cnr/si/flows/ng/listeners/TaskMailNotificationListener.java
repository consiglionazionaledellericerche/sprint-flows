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
public class TaskMailNotificationListener  implements ActivitiEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskMailNotificationListener.class);

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
        if ( event.getType() == ActivitiEventType.TASK_CREATED ) {
            ActivitiEntityEvent taskEvent = (ActivitiEntityEvent) event;
            TaskEntity task = (TaskEntity) taskEvent.getEntity();
            Map<String, Object> variables = runtimeService.getVariables(event.getExecutionId());
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
        } else {

        }
    }

    @Override
    public boolean isFailOnException() {
        // TODO Auto-generated method stub
        return false;
    }
}
