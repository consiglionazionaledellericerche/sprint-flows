package it.cnr.si.repository;

import it.cnr.si.domain.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for the Relationship entity.
 */
@SuppressWarnings("unused")
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

}
