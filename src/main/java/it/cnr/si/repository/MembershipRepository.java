package it.cnr.si.repository;

import it.cnr.si.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * Spring Data JPA repository for the Membership entity.
 */
@SuppressWarnings("unused")
public interface MembershipRepository extends JpaRepository<Membership,Long> {

    @Query("select groupname from Membership membership where membership.username =:username")
    public Set<String> findGroupsForUsername(@Param("username") String username);

    @Query("select username from Membership membership where membership.groupname =:groupname")
    public List<String> findMembersInGroup(@Param("groupname") String groupname);
}
