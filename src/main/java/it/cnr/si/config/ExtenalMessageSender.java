package it.cnr.si.config;

import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.service.ExternalMessageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@EnableScheduling
@Configuration
public class ExtenalMessageSender {

    private final Logger log = LoggerFactory.getLogger(ExtenalMessageSender.class);

    @Inject
    private ExternalMessageService externalMessageService;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
    }

    @Scheduled(fixedDelay = 600000, initialDelay = 10000) // 10m
    public void sendMessages() {
        log.debug("Processo le rest ExternalMessage");
        externalMessageService.getNewExternalMessages().forEach(this::send);
    }

    @Scheduled(fixedDelay = 21600000, initialDelay = 60000) // 6h
    public void sendErrorMessages() {
        log.debug("Processo le rest ExternalMessage in errore");
        externalMessageService.getFailedExternalMessages().forEach(this::send);
    }

    private void send(ExternalMessage msg) {

        log.debug("Tentativo della rest {}", msg);

        ResponseEntity<String> response = null;
        try {

             response = restTemplate.exchange(
                    msg.getUrl(),
                    msg.getVerb().value(),
                    new HttpEntity<>(msg.getPayload()),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK)
                throw new Exception();

            msg.setStatus(ExternalMessageStatus.SENT);
            msg.setLastErrorMessage(StringUtils.substring(response.getBody(), 0, 254));
            externalMessageService.save(msg);
            log.debug("Rest eseguita con successo {} ", msg);

        } catch (Exception e) {

            String responseMessage;
            if (response == null)
                responseMessage = e.getMessage();
            else if (response.getBody() == null)
                responseMessage = String.valueOf(response.getStatusCodeValue());
            else
                responseMessage = response.getBody();

            log.error("Rest fallita con messaggio {} {} ", responseMessage, msg, e);

            msg.setStatus(ExternalMessageStatus.ERROR);
            msg.setRetries(msg.getRetries() + 1);
            msg.setLastErrorMessage(StringUtils.substring(responseMessage, 0, 254));
            externalMessageService.save(msg);
        }
    }

}



