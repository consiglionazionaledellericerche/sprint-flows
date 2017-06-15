package it.cnr.si.flows.ng.listeners;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SaveSummaryAtProcessCompletion implements ActivitiEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveSummaryAtProcessCompletion.class);

    @Override
    public void onEvent(ActivitiEvent event) {
        if ( event.getType() == ActivitiEventType.PROCESS_COMPLETED ) {
            LOGGER.info("Processo {} con nome {} completato. Salvo il summary.",
                    event.getExecutionId(),
                    event.getEngineServices().getRuntimeService().getVariable(event.getExecutionId(), "titolo"));

            event.getEngineServices().getRuntimeService().setVariable(event.getExecutionId(), "summary", "pippo");
        }
    }

    /**
     * Se per caso la creazione del summary non riesca, non e' un problema bloccante
     * Si puo' ricreare in un secondo momento
     */
    @Override
    public boolean isFailOnException() {
        return false;
    }


}
