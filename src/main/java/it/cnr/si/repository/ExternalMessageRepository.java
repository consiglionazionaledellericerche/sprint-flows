package it.cnr.si.repository;

import it.cnr.si.domain.ExternalMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the ExternalMessage entity.
 */
@SuppressWarnings("unused")
public interface ExternalMessageRepository extends JpaRepository<ExternalMessage,Long> {

    @Query("select externalmessage from ExternalMessage externalmessage " +
            " where " +
            " (externalmessage.status = 'NEW' or externalmessage.status = 'ERROR') " +
            " and externalmessage.retries < 6")
    List<ExternalMessage> getNewExternalMessages();

    @Query("select externalmessage from ExternalMessage externalmessage" +
            " where externalmessage.status = 'ERROR' " +
            " and externalmessage.retries >= 6 and externalmessage.retries < 15")
    List<ExternalMessage> getFailedExternalMessages();

    @Query("select externalmessage from ExternalMessage externalmessage where externalmessage.url LIKE CONCAT('%',:searchTerms,'%') OR externalmessage.payload LIKE CONCAT('%',:searchTerms,'%')")
    Page<ExternalMessage> findAllByUrlOrPayload(@Param("searchTerms") String searchTerms, Pageable pageable);
}
