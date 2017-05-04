package it.cnr.si.repository;

import it.cnr.si.domain.Dynamiclist;
import it.cnr.si.domain.View;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the View entity.
 */
@SuppressWarnings("unused")
public interface ViewRepository extends JpaRepository<View,Long> {

    @Query("SELECT v from View v where v.processId = :processId and v.type = :type")
    public View getViewByProcessidType(@Param("processId") String processId, @Param("type") String type);

}
