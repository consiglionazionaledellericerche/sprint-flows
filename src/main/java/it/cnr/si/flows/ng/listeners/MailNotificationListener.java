package it.cnr.si.flows.ng.listeners;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Override
    public void onEvent(ActivitiEvent event) {
        if ( event.getType() == ActivitiEventType.TASK_CREATED ) {
            ActivitiEntityEvent taskEvent = (ActivitiEntityEvent) event;
            TaskEntity task = (TaskEntity) taskEvent.getEntity();
            Map<String, Object> variables = runtimeService.getVariables(event.getExecutionId());
            Set<IdentityLink> candidates = ((TaskEntity)taskEvent.getEntity()).getCandidates();

            candidates.forEach(c -> {
                if (c.getGroupId() != null) {

                    List<String> members = membershipService.findMembersInGroup(c.getGroupId());

                    members.forEach(m -> {
                        mailService.sendTaskAvailableNotification(variables, task.getName(), m, c.getGroupId());
                    });


                }
            });

        }
    }

    @Override
    public boolean isFailOnException() {
        // TODO Auto-generated method stub
        return false;
    }
}
