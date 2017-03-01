package it.cnr.si.flows.ng.config;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ActivitiLoggingEventListener implements ActivitiEventListener {

    private final Logger log = LoggerFactory.getLogger(ActivitiLoggingEventListener.class);


    @Override
    public void onEvent(ActivitiEvent event) {
        log.info(
                event.getType() +" - "+
                event.getExecutionId() +" - "+
                event.getProcessDefinitionId() +" - "+
                event.getProcessInstanceId());

        if (event.getExecutionId() != null)
            log.info(""+ event.getEngineServices().getRuntimeService().getVariable(event.getExecutionId(), "titolo"));

    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

}
