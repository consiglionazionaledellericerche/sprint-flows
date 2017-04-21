package it.cnr.si.repository;

import it.cnr.si.domain.Cnrgroup;
import it.cnr.si.domain.Membership;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * Spring Data JPA repository for the Membership entity.
 */
@SuppressWarnings("unused")
public interface MembershipRepository extends JpaRepository<Membership,Long> {

    @Query("select groupname from Membership membership where membership.username =:username")
    Set<String> findGroupsForUsername(@Param("username") String username);
}
