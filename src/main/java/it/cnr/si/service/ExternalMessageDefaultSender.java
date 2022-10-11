package it.cnr.si.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Profile({"!cnr"})
@Configuration
public class ExternalMessageDefaultSender implements ExternalMessageSender{
    private final Logger log = LoggerFactory.getLogger(ExternalMessageDefaultSender.class);

    @Override
    public void sendMessages() {
        log.info("Deafault sendMessages->Non fa nulla");
    }

    @Override
    public void sendErrorMessages() {
        log.info("Deafault sendErrorMessages->Non fa nulla");
    }
}



