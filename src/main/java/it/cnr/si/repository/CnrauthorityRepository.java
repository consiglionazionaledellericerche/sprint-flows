package it.cnr.si.repository;

import it.cnr.si.domain.Cnrauthority;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the Cnrauthority entity.
 */
@SuppressWarnings("unused")
public interface CnrauthorityRepository extends JpaRepository<Cnrauthority,Long> {

}
