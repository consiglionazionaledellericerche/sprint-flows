package it.cnr.si.repository;

import it.cnr.si.domain.Blacklist;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Blacklist entity.
 */
@SuppressWarnings("unused")
public interface BlacklistRepository extends JpaRepository<Blacklist,Long> {
    
    @Query("select blacklist from Blacklist blacklist where blacklist.email =:email and blacklist.processDefinitionKey =:key")
    public Blacklist findBlacklist(@Param("email") String email, @Param("key") String key);

}
