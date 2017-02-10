package it.cnr.si.flows.ng.config;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowsVisibilitySetter implements ActivitiEventListener {

    private final Logger log = LoggerFactory.getLogger(FlowsVisibilitySetter.class);

    @Override
    public void onEvent(ActivitiEvent event) {
        if (event.getType().equals(ActivitiEventType.TASK_CREATED)) {
            String processInstanceId = event.getProcessInstanceId();
            Map<String, VariableInstance> variableInstancesByExecutionIds = event.getEngineServices().getRuntimeService().getVariableInstances(event.getExecutionId());


        }

    }

    @Override
    public boolean isFailOnException() {
        // TODO Auto-generated method stub
          return false;
    }

}
