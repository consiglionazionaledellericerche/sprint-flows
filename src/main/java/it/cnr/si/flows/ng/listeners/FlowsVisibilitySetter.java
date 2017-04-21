package it.cnr.si.flows.ng.listeners;

import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowsVisibilitySetter implements ActivitiEventListener {

    private final Logger log = LoggerFactory.getLogger(FlowsVisibilitySetter.class);

    @Override
    public void onEvent(ActivitiEvent event) {
        if (event.getType().equals(ActivitiEventType.TASK_CREATED)) {
            String processInstanceId = event.getProcessInstanceId();
            Map<String, VariableInstance> variableInstancesByExecutionIds = event.getEngineServices().getRuntimeService().getVariableInstances(event.getExecutionId());

            String processDefinitionId = null;
            String currentTaskKey = null;
            String eventType;

            // TODO default e finire

            List<String> groups = VisibilityMapping.GroupVisibilityMappingForProcessInstance.get(processDefinitionId +"-"+ currentTaskKey);

            for (String group : groups) {
                event.getEngineServices().getRuntimeService().addGroupIdentityLink(processInstanceId, group, "visualizzatore");
            }

            List<String> users = VisibilityMapping.UserVisibilityMappingForProcessInstance.get(processDefinitionId +"-"+ currentTaskKey);

            for (String user : users) {
                event.getEngineServices().getRuntimeService().addGroupIdentityLink(processInstanceId, user, "visualizzatore");
            }
        }

    }

    @Override
    public boolean isFailOnException() {
        // TODO Auto-generated method stub
        return false;
    }

}
