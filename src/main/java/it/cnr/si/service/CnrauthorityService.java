package it.cnr.si.service;

import it.cnr.si.domain.Cnrauthority;
import it.cnr.si.repository.CnrauthorityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;

/**
 * Service Implementation for managing Cnrauthority.
 */
@Service
@Transactional
public class CnrauthorityService {

    private final Logger log = LoggerFactory.getLogger(CnrauthorityService.class);

    @Inject
    private CnrauthorityRepository cnrauthorityRepository;

    /**
     * Save a cnrauthority.
     *
     * @param cnrauthority the entity to save
     * @return the persisted entity
     */
    @CacheEvict(value = {"allGroups", "user"}, allEntries = true)
    public Cnrauthority save(Cnrauthority cnrauthority) {
        log.debug("Request to save Cnrauthority : {}", cnrauthority);
        return cnrauthorityRepository.save(cnrauthority);
    }

    /**
     *  Get all the cnrauthorities.
     *
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Cnrauthority> findAll(Pageable pageable) {
        log.debug("Request to get all Cnrauthorities");
        return cnrauthorityRepository.findAll(pageable);
    }

    /**
     *  Get one cnrauthority by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true)
    public Cnrauthority findOne(Long id) {
        log.debug("Request to get Cnrauthority : {}", id);
        return cnrauthorityRepository.findOneWithEagerRelationships(id);
    }

    /**
     *  Delete the  cnrauthority by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Cnrauthority : {}", id);
        cnrauthorityRepository.delete(id);
    }
}
