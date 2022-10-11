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
import java.util.List;
import java.util.Map;

/**
 * Service Implementation for managing ExternalMessage.
 */
@Service
@Transactional
public class ExternalMessageService {

    private final Logger log = LoggerFactory.getLogger(ExternalMessageService.class);
    
    @Inject
    private ExternalMessageRepository externalMessageRepository;
   // @Inject
   // private ExternalMessageSender externalMessageSender;

    /**
     * Save a externalMessage.
     *
     * @param externalMessage the entity to save
     * @return the persisted entity
     */
    public ExternalMessage save(ExternalMessage externalMessage) {
        log.debug("Request to save ExternalMessage : {}", externalMessage);
        ExternalMessage result = externalMessageRepository.save(externalMessage);
        return result;
    }

    /**
     * Get all the externalMessages.
     *
     * @param pageable         the pagination information
     * @param status           the status
     * @param application      the application
     * @param payload          the payload
     * @param lastErrorMessage the lastErrorMessage
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<ExternalMessage> findAllBySearchTerms(Pageable pageable, String status, String application, String payload, String lastErrorMessage) {
        log.debug("Request to get all ExternalMessages");

        return externalMessageRepository.findAllBySearchTerms(status, application, payload, lastErrorMessage, pageable);
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
        ExternalMessage externalMessage = externalMessageRepository.findById(id).get();
        return externalMessage;
    }

    /**
     *  Delete the  externalMessage by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete ExternalMessage : {}", id);
        externalMessageRepository.deleteById(id);
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

        // per ora disabilito questo comportamento perche' causa problemi di soncronicita'
        // i messaggi verranno inviati in modo asincrono
        // tento un primo invio non appena il messaggio viene inserito
//        externalMessageSender.send(msg);
//        save(msg);
    }

    public List<ExternalMessage> getNewExternalMessages() {
        return externalMessageRepository.getNewExternalMessages();
    }

    public List<ExternalMessage> getFailedExternalMessages() {
        return externalMessageRepository.getFailedExternalMessages();
    }

}
