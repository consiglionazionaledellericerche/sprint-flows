package it.cnr.si.repository;

import it.cnr.si.domain.Faq;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the Faq entity.
 */
@SuppressWarnings("unused")
public interface FaqRepository extends JpaRepository<Faq,Long> {

    @Query("select faq from Faq faq where faq.isReadable = TRUE")
    public List<Faq> getReadableFaq();

}
