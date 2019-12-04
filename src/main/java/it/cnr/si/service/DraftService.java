package it.cnr.si.service;

import it.cnr.si.domain.Draft;
import it.cnr.si.repository.DraftRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger log = LoggerFactory.getLogger(DraftService.class);

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
    public Draft findDraft(Long taskId, String username) {
        Draft dbDraft;

        if(username.isEmpty())
            dbDraft = draftRepository.getDraftByTaskId(taskId);
        else
            dbDraft = draftRepository.getDraftByTaskIdAndUsername(taskId, username);

        return dbDraft;
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
        return  draftRepository.findOne(id);
    }

    /**
     * Delete.
     *
     * @param id the id
     */
    public void delete(Long id) {
        draftRepository.delete(id);
    }

    /**
     * Delete draft by task id.
     *
     * @param taskId the task id
     */
    public void deleteDraftByTaskId(Long taskId) {
        Draft draft = draftRepository.getDraftByTaskId(taskId);

        draftRepository.delete(draft.getId());
    }
}
