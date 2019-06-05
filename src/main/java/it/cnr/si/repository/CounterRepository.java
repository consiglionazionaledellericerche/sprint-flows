package it.cnr.si.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.cnr.si.domain.Counter;

/**
 * Spring Data JPA repository for the Counter entity.
 */
public interface CounterRepository extends JpaRepository<Counter,Long> {

    @Query("SELECT c from Counter c where c.name = :name")
    public Counter findByName(@Param("name") String name);

}
