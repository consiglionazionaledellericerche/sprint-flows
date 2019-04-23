package it.cnr.si.repository;

import it.cnr.si.domain.ExternalMessage;

import org.springframework.data.jpa.repository.*;

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
    public List<ExternalMessage> getNewExternalMessages();

    @Query("select externalmessage from ExternalMessage externalmessage" +
            " where externalmessage.status = 'NEW' or externalmessage.status = 'ERROR' " +
            " and externalmessage.retries >= 6 and externalmessage.retries < 15")
    public List<ExternalMessage> getFailedExternalMessages();

}
