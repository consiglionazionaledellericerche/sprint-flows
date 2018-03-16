package it.cnr.si.repository;

import it.cnr.si.domain.FlowsUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface FlowsUserRepository extends JpaRepository<FlowsUser,Long> {

    @Query("SELECT u from FlowsUser u where u.login = :login")
    Optional<FlowsUser> findOneByLogin(@Param("login") String login);

    Optional<FlowsUser> findOneByActivationKey(String var1);

    List<FlowsUser> findAllByActivatedIsFalseAndCreatedDateBefore(ZonedDateTime var1);

    Optional<FlowsUser> findOneByResetKey(String var1);

    Optional<FlowsUser> findOneByEmail(String var1);

    Optional<FlowsUser> findOneById(Long var1);

    @Query(
            value = "select distinct user from FlowsUser user join fetch user.authorities",
            countQuery = "select count(user) from FlowsUser user"
    )
    Page<FlowsUser> findAllWithAuthorities(Pageable var1);

    void delete(FlowsUser var1);
}
