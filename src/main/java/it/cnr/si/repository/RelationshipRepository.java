package it.cnr.si.repository;

import it.cnr.si.domain.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * Spring Data JPA repository for the Relationship entity.
 */
@SuppressWarnings("unused")
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    @Query("select relationship from Relationship relationship where relationship.groupName =:groupName")
    public Set<Relationship> findRelationshipGroup(@Param("groupName") String groupName);

    @Query("select relationship from Relationship relationship where relationship.groupName = CONCAT(:groupName, '@STRUTTURA')")
    public Set<Relationship> findRelationshipForStructure(@Param("groupName") String groupName);

    @Query("select relationship from Relationship relationship where relationship.groupRelationship =:groupRelationship")
    public Set<Relationship> getRelationshipsForGroupRelationship(@Param("groupRelationship") String groupRelationship);

    @Query("select relationship from Relationship relationship where relationship.groupRelationship = CONCAT(:groupRelationship, '@STRUTTURA')")
    public Set<Relationship> findRelationshipForStructureByGroupRelationship(@Param("groupRelationship") String groupRelationship);
}
