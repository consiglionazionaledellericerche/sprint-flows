package it.cnr.si.repository;

import it.cnr.si.domain.NotificationRule;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the NotificationRule entity.
 */
@SuppressWarnings("unused")
public interface NotificationRuleRepository extends JpaRepository<NotificationRule,Long> {

    @Query("select notificationrule from NotificationRule notificationrule "
            + "where notificationrule.processId =:processId "
            + "and notificationrule.eventType =:eventType "
            + "and notificationrule.taskName =:taskName")
    public List<NotificationRule> findRulesByProcessIdEventTypeTaskName(@Param("processId") String processId, @Param("eventType") String eventType, @Param("taskName") String taskName);

    @Query("select notificationrule from NotificationRule notificationrule "
            + "where notificationrule.processId =:processId "
            + "and notificationrule.eventType =:eventType")
    public List<NotificationRule> findRulesByProcessIdEventType(@Param("processId") String processId, @Param("eventType") String eventType);


}
