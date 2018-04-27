package it.cnr.si.repository;

import it.cnr.si.domain.Cnrgroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Cnrgroup entity.
 */
public interface CnrgroupRepository extends JpaRepository<Cnrgroup,Long> {

    @Query("select distinct cnrgroup from Cnrgroup cnrgroup left join fetch cnrgroup.parents")
    public List<Cnrgroup> findAllWithEagerRelationships();

    @Query("select cnrgroup from Cnrgroup cnrgroup left join fetch cnrgroup.parents where cnrgroup.id =:id")
    public Cnrgroup findOneWithEagerRelationships(@Param("id") Long id);

    @Query("select displayName from Cnrgroup cnrgroup where cnrgroup.name =:name")
    public String findDisplayName(@Param("name") String name);

    @Query("select cnrgroup from Cnrgroup cnrgroup where cnrgroup.name =:name")
    public Cnrgroup findCnrgroupByName(@Param("name") String name);
}
