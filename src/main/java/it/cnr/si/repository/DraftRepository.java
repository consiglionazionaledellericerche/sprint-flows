package it.cnr.si.repository;

import it.cnr.si.domain.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for the Draft entity.
 */
@SuppressWarnings("unused")
public interface DraftRepository extends JpaRepository<Draft,Long> {

    @Query("select draft from Draft draft where draft.taskId =:taskId AND draft.username=:username")
    public Draft getDraftByTaskIdAndUsername(@Param("taskId") Long taskId, @Param("username") String username);

    @Query("select draft from Draft draft where draft.processDefinitionId =:processDefinitionId AND draft.username=:username")
    public Draft getDraftByProcessInstanceIdAndUsername(@Param("processDefinitionId") String processDefinitionId, @Param("username") String username);

    @Query("select draft from Draft draft where draft.taskId =:taskId AND draft.username is null)")
    public Draft getDraftByTaskId(@Param("taskId") Long taskId);

    @Query("select draft from Draft draft where draft.taskId =:taskId")
    List<Draft> getAllDraftByTaskId(@Param("taskId") Long taskId);
}
