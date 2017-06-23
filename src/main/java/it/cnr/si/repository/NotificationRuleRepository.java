package it.cnr.si.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.cnr.si.domain.NotificationRule;

/**
 * Spring Data JPA repository for the NotificationRule entity.
 */
@SuppressWarnings("unused")
public interface NotificationRuleRepository extends JpaRepository<NotificationRule,Long> {


    @Query("select groups from NotificationRule notificationrule "
            + "where notificationrule.processId =:processId "
            + "and notificationrule.eventType =:eventType "
            + "and notificationrule.taskName =:taskName")
    String findGroupsByProcessIdEventTypeTaskName(@Param("processId") String processId, @Param("eventType") String eventType, @Param("taskName") String taskName);

    @Query("select groups from NotificationRule notificationrule "
            + "where notificationrule.processId =:processId "
            + "and notificationrule.eventType =:eventType")
    String findGroupsByProcessIdEventType(@Param("processId") String processId, @Param("eventType") String eventType);
}
