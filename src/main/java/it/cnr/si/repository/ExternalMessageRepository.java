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

    @Query("select externalmessage from ExternalMessage externalmessage where externalmessage.url LIKE CONCAT('%',:url,'%')")
    Page<ExternalMessage> findAllByUrl(@Param("url") String url, Pageable pageable);

    @Query("select externalmessage from ExternalMessage externalmessage where externalmessage.payload LIKE CONCAT('%',:payload,'%')")
    Page<ExternalMessage> findAllByPayload(@Param("payload") String payload, Pageable pageable);
}
