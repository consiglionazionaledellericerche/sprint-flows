package it.cnr.si.repository;

import it.cnr.si.domain.Dynamiclist;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Dynamiclist entity.
 */
@SuppressWarnings("unused")
public interface DynamiclistRepository extends JpaRepository<Dynamiclist,Long> {

    @Query("SELECT l from Dynamiclist l where l.name = :name")
    public Dynamiclist findOneByName(@Param("name") String name);

}
