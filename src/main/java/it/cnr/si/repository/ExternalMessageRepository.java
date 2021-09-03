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


    @Query("SELECT em FROM ExternalMessage em " +
            "WHERE (:payload IS NULL OR em.payload LIKE CONCAT('%',:payload,'%')) " +
            "AND (:status IS NULL OR em.status LIKE CONCAT('%',:status,'%')) " +
            "AND (:application IS NULL OR em.application LIKE CONCAT('%',:application,'%')) " +
            "AND (:lastErrorMessage IS NULL OR em.lastErrorMessage LIKE CONCAT('%',:lastErrorMessage,'%')) ")
    Page<ExternalMessage> findAllBySearchTerms(@Param("status") String status,
                                               @Param("application") String application,
                                               @Param("payload") String payload,
                                               @Param("lastErrorMessage") String lastErrorMessage, Pageable pageable);
}
