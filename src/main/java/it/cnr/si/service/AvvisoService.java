package it.cnr.si.service;

import it.cnr.si.domain.Avviso;
import it.cnr.si.repository.AvvisoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Service Implementation for managing Avviso.
 */
@Service
@Transactional
public class AvvisoService {

    private final Logger log = LoggerFactory.getLogger(AvvisoService.class);
    
    @Inject
    private AvvisoRepository avvisoRepository;

    /**
     * Save a avviso.
     *
     * @param avviso the entity to save
     * @return the persisted entity
     */
    public Avviso save(Avviso avviso) {
        log.debug("Request to save Avviso : {}", avviso);
        Avviso result = avvisoRepository.save(avviso);
        return result;
    }

    /**
     *  Get all the avvisos.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<Avviso> findAll(Pageable pageable) {
        log.debug("Request to get all Avvisos");
        Page<Avviso> result = avvisoRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get one avviso by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public Avviso findOne(Long id) {
        log.debug("Request to get Avviso : {}", id);
        Avviso avviso = avvisoRepository.findById(id).get();
        return avviso;
    }

    /**
     *  Delete the  avviso by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Avviso : {}", id);
        avvisoRepository.deleteById(id);
    }
}
