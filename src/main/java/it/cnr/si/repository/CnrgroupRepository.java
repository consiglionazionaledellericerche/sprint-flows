package it.cnr.si.repository;

import it.cnr.si.domain.Cnrgroup;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Cnrgroup entity.
 */
@SuppressWarnings("unused")
public interface CnrgroupRepository extends JpaRepository<Cnrgroup,Long> {

    @Query("select distinct cnrgroup from Cnrgroup cnrgroup left join fetch cnrgroup.parents left join fetch cnrgroup.memberUsers")
    List<Cnrgroup> findAllWithEagerRelationships();

    @Query("select cnrgroup from Cnrgroup cnrgroup left join fetch cnrgroup.parents left join fetch cnrgroup.memberUsers where cnrgroup.id =:id")
    Cnrgroup findOneWithEagerRelationships(@Param("id") Long id);

}
