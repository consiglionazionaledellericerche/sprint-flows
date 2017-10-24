package it.cnr.si.service;

import it.cnr.si.domain.Cnrgroup;
import it.cnr.si.domain.Relationship;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.CnrgroupRepository;
import it.cnr.si.repository.RelationshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service Implementation for managing Relationship.
 */
@Service
@Transactional
public class RelationshipService {

    private final Logger log = LoggerFactory.getLogger(RelationshipService.class);

    @Inject
    private RelationshipRepository relationshipRepository;
    @Inject
    private AceBridgeService aceService;
    @Inject
    private CnrgroupRepository cnrgroupRepository;

    /**
     * Save a relationship.
     *
     * @param relationship the entity to save
     * @return the persisted entity
     */
    public Relationship save(Relationship relationship) {
        log.debug("Request to save Relationship : {}", relationship);
        Relationship result = relationshipRepository.save(relationship);
        return result;
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
        Page<Relationship> result = relationshipRepository.findAll(pageable);
        return result;
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
        Relationship relationship = relationshipRepository.findOne(id);
        return relationship;
    }

    /**
     * Delete the  relationship by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Relationship : {}", id);
        relationshipRepository.delete(id);
    }


    @Cacheable("additionalAuthorities")
    public List<GrantedAuthority> getAllAdditionalGroupsForUser(String username) {
        Set<String> aceGroup = getACEGroupsForUser(username);
        Set<String> aceGropupWithParents = getAllACEParents(aceGroup);

        return Stream.concat(aceGropupWithParents.stream(), getAllRelationship(aceGropupWithParents).stream())
                .distinct()
                .map(Utils::addLeadingRole)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }


    public Set<String> getAllACEParents(Set<String> groups) {
        Set<String> parents = getACEParents(groups);
        Set<String> groupAndParents = new HashSet<>();
        //calcolo anche i padri "ricorsivamente", risalendio l'albero delle strutture
        while (!parents.isEmpty()) {
            groupAndParents = Stream.concat(groups.stream(), parents.stream()
                    .distinct())
                    .collect(Collectors.toSet());
            parents = getACEParents(parents);
        }
        return groupAndParents;
    }


    private Set<String> getAllRelationship(Set<String> aceGropupWithParents) {
        Set<Relationship> result = new HashSet<>();
        for (String group : aceGropupWithParents) {
            //match esatto (ad es.: ra@2216 -> supervisore#acquistitrasparenza@STRUTTURA)
            result.addAll(relationshipRepository.findRelationshipGroup(group));

            //match "@STRUTTURA" (ad es. relationship: ra@STRUTTURA -> supervisore#acquistitrasparenza@STRUTTURA)
            String role = group.substring(0, group.indexOf('@'));
            Set<Relationship> relationshipGroupForStruttura = relationshipRepository.findRelationshipGroupForStruttura(
                    group.contains("@") ? role : group);

            // rimpiazzo "@STRUTTURA" nella relationship con il codice della struttura (ad es:
            result.addAll(relationshipGroupForStruttura.stream()
                                  .map(a -> {
                                      String struttura = group.substring(group.indexOf('@'), group.length());
                                      a.setGroupRelationship(Utils.replaceStruttura(a, struttura));
                                      return a;
                                  })
                                  .collect(Collectors.toSet()));
        }
        //mapping in modo da recuperare il set di gruppi in relationships (stringhe)
        return result.stream()
                .distinct()
                .map(Relationship::getGroupRelationship)
                .collect(Collectors.toSet());
    }


    public Set<String> getACEGroupsForUser(String username) {
        return new HashSet<>(aceService.getAceGroupsForUser(username));
    }


    private Set<String> getACEParents(Set<String> groups) {
        Set<String> parents = new HashSet<>();

        for (String groupString : groups) {
            Cnrgroup group = cnrgroupRepository.findOneWithEagerRelationshipsByName(groupString);
            if (group != null) {
                parents.addAll(group.getParents().stream()
                                       .map(Cnrgroup::getName)
                                       .collect(Collectors.toSet()));
            }
        }
        return parents.stream().distinct().collect(Collectors.toSet());
    }
}
