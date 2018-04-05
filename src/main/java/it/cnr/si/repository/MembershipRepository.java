package it.cnr.si.repository;

import it.cnr.si.domain.Membership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("select membership from Membership membership where membership.groupname =:groupname")
    public List<Membership> getMembershipByGroupName(@Param("groupname") String groupname);

    @Query(
            value = "SELECT membership FROM Membership membership WHERE membership.grouprole = :grouprole AND membership.username = :username"
    )
    public Page<Membership> getGroupsWithRole(@Param("grouprole") String grouprole, @Param("username") String username, Pageable pageable);

    @Query("select membership from Membership membership where membership.username =:username AND membership.groupname =:groupname")
    public Membership findOneByUsernameAndGroupname(@Param("username") String username, @Param("groupname") String groupname);
}
