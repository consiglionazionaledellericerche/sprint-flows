package it.cnr.si.repository;

import it.cnr.si.domain.Faq;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Faq entity.
 */
@SuppressWarnings("unused")
public interface FaqRepository extends JpaRepository<Faq,Long> {

    @Query("select faq from Faq faq where faq.isReadable = TRUE")
    public List<Faq> getReadableFaq();

    @Query("select faq from Faq faq where faq.isReadable = TRUE AND faq.domanda LIKE CONCAT('%',:procesDefinition,'%')")
    public List<Faq> getReadableForProcessDefinition(@Param("procesDefinition") String procesDefinition);

}
