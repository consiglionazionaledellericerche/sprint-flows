package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.service.ExternalMessageService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;

import javax.inject.Inject;
import java.util.HashMap;

public class EndAcquisti {

    @Inject
    private ExternalMessageService externalMessageService;

    public void onEvent(ActivitiEvent event) {
        if (event.getType() == ActivitiEventType.PROCESS_COMPLETED) {
            externalMessageService.createExternalMessage("url", ExternalMessageVerb.POST, new HashMap());
        }
    }
}
