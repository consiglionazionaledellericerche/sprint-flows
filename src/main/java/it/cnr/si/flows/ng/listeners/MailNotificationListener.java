package it.cnr.si.flows.ng.listeners;

import javax.inject.Inject;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.cnr.si.service.MailService;

@Component
public class MailNotificationListener  implements ActivitiEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailNotificationListener.class);

    @Inject
    private MailService mailService;

    @Override
    public void onEvent(ActivitiEvent event) {
        if ( event.getType() == ActivitiEventType.TASK_CREATED ) {

            LOGGER.debug("Invio email a marcinireneusz.trycz@cnr.it");

            mailService.sendEmail (
                    "marcinireneusz.trycz@cnr.it",
                    "Vai Ganasso",
                    "Ganax is the way",
                    false,
                    false);

        }
    }

    @Override
    public boolean isFailOnException() {
        // TODO Auto-generated method stub
        return false;
    }
}
