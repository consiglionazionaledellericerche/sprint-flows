package it.cnr.si.repository;

import it.cnr.si.domain.Form;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Form entity.
 */
@SuppressWarnings("unused")
public interface FormRepository extends JpaRepository<Form,Long> {

    @Query("select form from Form form where form.processDefinitionKey =:processDefinitionKey and form.version = :version and form.taskId =:taskId")
    public Form findOneByProcessDefinitionKeyAndVersionAndTaskId(
            @Param("processDefinitionKey") String processDefinitionKey,
            @Param("version") String version,
            @Param("taskId") String taskId);

}
