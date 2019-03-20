package it.cnr.si.service;

import com.google.gson.Gson;
import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.repository.ExternalMessageRepository;
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
     *  Get all the externalMessages.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<ExternalMessage> findAll(Pageable pageable) {
        log.debug("Request to get all ExternalMessages");
        Page<ExternalMessage> result = externalMessageRepository.findAll(pageable);
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
        ExternalMessage externalMessage = externalMessageRepository.findOne(id);
        return externalMessage;
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

    public void createExternalMessage(String url, ExternalMessageVerb verb, Map payload) {

        Gson gson = new Gson();
        String payloadString = gson.toJson(payload);

        ExternalMessage msg = new ExternalMessage();

        msg.setUrl(url);
        msg.setVerb(verb);
        msg.setPayload(payloadString);
        msg.setStatus(ExternalMessageStatus.NEW);
        msg.setRetries(0);
        msg.setLastErrorMessage(null);

        save(msg);
    }

    public List<ExternalMessage> getNewExternalMessages() {
        return externalMessageRepository.getNewExternalMessages();
    }

    public List<ExternalMessage> getFailedExternalMessages() {
        return externalMessageRepository.getFailedExternalMessages();
    }

}
