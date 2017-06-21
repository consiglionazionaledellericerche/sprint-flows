package it.cnr.si.repository;

import it.cnr.si.domain.NotificationRule;

import org.springframework.data.jpa.repository.*;

import java.util.List;

/**
 * Spring Data JPA repository for the NotificationRule entity.
 */
@SuppressWarnings("unused")
public interface NotificationRuleRepository extends JpaRepository<NotificationRule,Long> {

}
