package it.cnr.si.service;

import it.cnr.si.domain.Cnrgroup;
import it.cnr.si.repository.CnrgroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

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
    @CacheEvict(value = {"allGroups", "user"}, allEntries = true)
    public Cnrgroup save(Cnrgroup cnrgroup) {
        log.debug("Request to save Cnrgroup : {}", cnrgroup);
        return cnrgroupRepository.save(cnrgroup);
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
        return cnrgroupRepository.findAll(pageable);
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
        return cnrgroupRepository.findOneWithEagerRelationships(id);
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
