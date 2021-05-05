package it.cnr.si.repository;

import it.cnr.si.domain.Blacklist;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the Blacklist entity.
 */
@SuppressWarnings("unused")
public interface BlacklistRepository extends JpaRepository<Blacklist,Long> {

}
