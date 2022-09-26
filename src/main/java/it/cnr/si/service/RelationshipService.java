package it.cnr.si.service;

import it.cnr.si.domain.Relationship;
import it.cnr.si.repository.RelationshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Set;

@Service
@Transactional
public class RelationshipService {

    private final Logger log = LoggerFactory.getLogger(RelationshipService.class);

    @Inject
    private RelationshipRepository relationshipRepository;

    /**
     * Save a relationship.
     *
     * @param relationship the entity to save
     * @return the persisted entity
     */
    public Relationship save(Relationship relationship) {
        log.debug("Request to save Relationship : {}", relationship);
        return relationshipRepository.save(relationship);
    }

    /**
     * Get all the relationships.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<Relationship> findAll(Pageable pageable) {
        log.debug("Request to get all Relationships");
        return relationshipRepository.findAll(pageable);
    }

    /**
     * Get one relationship by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Relationship findOne(Long id) {
        log.debug("Request to get Relationship : {}", id);
        return relationshipRepository.findById(id).get();
    }

    /**
     * Delete the  relationship by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Relationship : {}", id);
        relationshipRepository.deleteById(id);
    }

    public Set<Relationship> getAllRelationshipForGroup(String group) {
        return relationshipRepository.findRelationshipGroup(group);
    }

    public Set<Relationship> getRelationshipsForGroupRelationship(String groupRelationship) {
        return relationshipRepository.getRelationshipsForGroupRelationship(groupRelationship);
    }

    public Set<Relationship> findRelationshipForStructure(String groupName) {
        return relationshipRepository.findRelationshipForStructure(groupName);
    }

    public Set<Relationship> findRelationshipForStructureByGroupRelationship(@Param("groupRelationship") String groupRelationship) {
        return relationshipRepository.findRelationshipForStructureByGroupRelationship(groupRelationship);
    }


}
