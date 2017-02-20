package it.cnr.si.service;

import it.cnr.si.domain.Cnrgroup;
import it.cnr.si.repository.CnrgroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Service Implementation for managing Cnrgroup.
 */
@Service
@Transactional
public class CnrgroupService {

    private final Logger log = LoggerFactory.getLogger(CnrgroupService.class);
    
    @Inject
    private CnrgroupRepository cnrgroupRepository;

    /**
     * Save a cnrgroup.
     *
     * @param cnrgroup the entity to save
     * @return the persisted entity
     */
    public Cnrgroup save(Cnrgroup cnrgroup) {
        log.debug("Request to save Cnrgroup : {}", cnrgroup);
        Cnrgroup result = cnrgroupRepository.save(cnrgroup);
        return result;
    }

    /**
     *  Get all the cnrgroups.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<Cnrgroup> findAll(Pageable pageable) {
        log.debug("Request to get all Cnrgroups");
        Page<Cnrgroup> result = cnrgroupRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get one cnrgroup by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public Cnrgroup findOne(Long id) {
        log.debug("Request to get Cnrgroup : {}", id);
        Cnrgroup cnrgroup = cnrgroupRepository.findOneWithEagerRelationships(id);
        return cnrgroup;
    }

    /**
     *  Delete the  cnrgroup by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Cnrgroup : {}", id);
        cnrgroupRepository.delete(id);
    }
}
