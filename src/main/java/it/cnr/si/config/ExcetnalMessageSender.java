package it.cnr.si.config;

import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.repository.ExternalMessageRepository;
import it.cnr.si.service.ExternalMessageService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@EnableScheduling
public class ExcetnalMessageSender {

    @Inject
    private ExternalMessageService externalMessageService;

    @Inject
    private RestTemplate restTemplate;

    @Scheduled(fixedDelay = 10) // 10m
    public void sendMessages() {
        externalMessageService.getNewExternalMessages().forEach(this::send);
    }

    @Scheduled(fixedDelay = 21600) // 6h
    public void sendErrorMessages() {
        externalMessageService.getFailedExternalMessages().forEach(this::send);
    }

    private void send(ExternalMessage msg) {
        try {
//                        restTemplate.exchange();

            msg.setStatus(ExternalMessageStatus.SENT);
            externalMessageService.save(msg);

        } catch (Exception e) { //TODO

            msg.setStatus(ExternalMessageStatus.ERROR);
            msg.setRetries(msg.getRetries() + 1);
            msg.setLastErrorMessage("pippo");
            externalMessageService.save(msg);
        }
    }


}



