package it.cnr.si.service;

import it.cnr.si.domain.Blacklist;
import it.cnr.si.repository.BlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Service Implementation for managing Blacklist.
 */
@Service
@Transactional
public class BlacklistService {

    private final Logger log = LoggerFactory.getLogger(BlacklistService.class);
    
    @Inject
    private BlacklistRepository blacklistRepository;

    /**
     * Save a blacklist.
     *
     * @param blacklist the entity to save
     * @return the persisted entity
     */
    public Blacklist save(Blacklist blacklist) {
        log.debug("Request to save Blacklist : {}", blacklist);
        Blacklist result = blacklistRepository.save(blacklist);
        return result;
    }

    /**
     *  Get all the blacklists.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<Blacklist> findAll(Pageable pageable) {
        log.debug("Request to get all Blacklists");
        Page<Blacklist> result = blacklistRepository.findAll(pageable);
        return result;
    }

    /**
     *  Get one blacklist by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public Blacklist findOne(Long id) {
        log.debug("Request to get Blacklist : {}", id);
        Blacklist blacklist = blacklistRepository.findById(id).get();
        return blacklist;
    }

    
    @Transactional(readOnly = true) 
    public Blacklist findOneByEmailAndKey(String email, String key) {
        Blacklist blacklist = blacklistRepository.findBlacklist(email, key);
        return blacklist;
    }
    
    /**
     *  Delete the  blacklist by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Blacklist : {}", id);
        blacklistRepository.deleteById(id);
    }
}
