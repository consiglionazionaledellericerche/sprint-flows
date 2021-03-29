package it.cnr.si.service;

import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.repository.ExternalMessageRepository;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

/**
 * Service Implementation for managing ExternalMessage.
 */
@Service
@Transactional
public class ExternalMessageService {

    private final Logger log = LoggerFactory.getLogger(ExternalMessageService.class);
    
    @Inject
    private ExternalMessageRepository externalMessageRepository;
    @Inject
    private ExternalMessageSender externalMessageSender;

    /**
     * Save a externalMessage.
     *
     * @param externalMessage the entity to save
     * @return the persisted entity
     */
    public ExternalMessage save(ExternalMessage externalMessage) {
        log.debug("Request to save ExternalMessage : {}", externalMessage);
        return externalMessageRepository.save(externalMessage);
    }

    /**
     *  Get all the externalMessages.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<ExternalMessage> findAll(String searchTerms, Pageable pageable) {
        log.debug("Request to get all ExternalMessages");
        String[] searchTermsArray = new String[2];
        if(searchTerms.contains(" "))
            searchTermsArray = searchTerms.split(" ");
        else
            searchTermsArray[0] = searchTerms;

        Page<ExternalMessage> result = null;
        if(searchTermsArray[0] != null)
            result = externalMessageRepository.findAllByUrlOrPayload(searchTermsArray[0], pageable);
        else
            result = externalMessageRepository.findAllByUrlsOrPayloads(searchTermsArray[0], searchTermsArray[1], pageable);

        return result;
    }

    /**
     *  Get one externalMessage by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public ExternalMessage findOne(Long id) {
        log.debug("Request to get ExternalMessage : {}", id);
        return externalMessageRepository.findOne(id);
    }

    /**
     *  Delete the  externalMessage by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete ExternalMessage : {}", id);
        externalMessageRepository.delete(id);
    }

    public void createExternalMessage(String url, ExternalMessageVerb verb, Map<String, Object> payload, ExternalApplication app) {

        JSONObject payloadString = new JSONObject(payload);
        ExternalMessage msg = new ExternalMessage();

        if (app != null)
            msg.setApplication(app);
        else
            msg.setApplication(ExternalApplication.GENERIC);

        msg.setUrl(url);
        msg.setVerb(verb);
        msg.setPayload(payloadString.toString());
        msg.setStatus(ExternalMessageStatus.NEW);
        msg.setRetries(0);
        msg.setLastErrorMessage(null);

        // probabilmente il save prima del primo invio non serve
        // ma lo faccio lo stesso nel caso qualcosa vada storto nell'invio
        save(msg);

        // tento un primo invio non appena il messaggio viene inserito
        externalMessageSender.send(msg);
        save(msg);
    }

    public List<ExternalMessage> getNewExternalMessages() {
        return externalMessageRepository.getNewExternalMessages();
    }

    public List<ExternalMessage> getFailedExternalMessages() {
        return externalMessageRepository.getFailedExternalMessages();
    }

}
