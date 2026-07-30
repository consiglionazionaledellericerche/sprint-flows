package it.cnr.si.service;

import it.cnr.si.domain.Draft;
import it.cnr.si.repository.DraftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

/**
 * Service Implementation for managing Avviso.
 */
@Service
@Transactional
public class DraftService {

    @Inject
    private DraftRepository draftRepository;

    /**
     * Find draft draft.
     * nel caso in cui non si specifichi l`username si recupera un Draft che può essere letto da tutti
     *
     * @param taskId   the task id
     * @param username the username
     * @return the draft
     */
    public Draft findDraftByTaskId(Long taskId, String username) {
        if(username != null)
            return draftRepository.getDraftByTaskIdAndUsername(taskId, username);
        else
            return draftRepository.getDraftByTaskId(taskId);
    }



    /**
     * Find draft draft.
     * nel caso in cui non si specifichi l`username si recupera un Draft che può essere letto da tutti
     *
     * @param processDefinitionId   the process Definition Id
     * @param username the username
     * @return the draft
     */
    public Draft findDraftByProcessDefinitionId(String processDefinitionId, String username) {
        return draftRepository.getDraftByProcessInstanceIdAndUsername(processDefinitionId, username);
    }


    public Draft findDraftByTaskId(Long taskId) {
        return draftRepository.getDraftByTaskId(taskId);

    }


    /**
     * Save draft.
     *
     * @param draft the draft
     * @return the draft
     */
    public Draft save(Draft draft) {
        return draftRepository.save(draft);
    }

    /**
     * Find all list.
     *
     * @return the list
     */
    public List<Draft> findAll() {
        return draftRepository.findAll();
    }

    /**
     * Find one draft.
     *
     * @param id the id
     * @return the draft
     */
    public Draft findOne(Long id) {
        return  draftRepository.findById(id).get();
    }

    /**
     * Delete.
     *
     * @param id the id
     */
    public void delete(Long id) {
        draftRepository.deleteById(id);
    }


    /**
     * Delete draft by task id
     *
     * @param taskId   the task id
     */
    public void deleteDraftByTaskId(Long taskId) {
        Draft draft = draftRepository.getDraftByTaskId(taskId);
        if(draft != null)
            draftRepository.deleteById(draft.getId());
    }


    /**
     * Delete draft by process Instance id and Username.
     *
     * @param processDefinitionId   the process Definition Id
     * @param username the username
     */
    public void deleteDraftByProcessInstanceIdAndUsername(String processDefinitionId, String username) {
        Draft draft = draftRepository.getDraftByProcessInstanceIdAndUsername(processDefinitionId, username);
        if (draft != null)
            draftRepository.delete(draft);
    }
}
