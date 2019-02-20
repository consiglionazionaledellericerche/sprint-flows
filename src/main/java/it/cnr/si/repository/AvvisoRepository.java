package it.cnr.si.repository;

import it.cnr.si.domain.Avviso;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the Avviso entity.
 */
@SuppressWarnings("unused")
public interface AvvisoRepository extends JpaRepository<Avviso,Long> {

    public List<Avviso> findByAttivoTrueOrderByIdDesc();
}
