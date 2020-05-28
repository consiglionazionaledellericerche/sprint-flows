package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Draft;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.service.DraftService;
import it.cnr.si.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Draft.
 */
@RestController
@RequestMapping("/api")
public class DraftResource {

    private final Logger log = LoggerFactory.getLogger(DraftResource.class);

    @Inject
    private DraftService draftService;


    /**
     * PUT  /drafts : Save or Updates an existing draft.
     * Draft associato a username e taskid (in caso di avvio del flusso il taskId è sostituito da -deploymentID della processDefinition che si sta avviando
     *
     * @param taskId   taskId del draft: SE È <0 ALLORA IL TASKID NON C`È, allora IL DRAFT È ASSOCIATO AD UNA PI CHE SI STA CREANDO (-deploimentid tipo di flusso che si sta avviando)
     * @param json la draft vera a propria, contenuta nel body della richiesta
     * @return the ResponseEntity with status 200 (OK) and with body the updated draft, or with status 400 (Bad Request) if the draft is not valid, or with status 500 (Internal Server Error) if the draft couldnt be updated
     */
    @PutMapping(value = "/drafts/updateDraft",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Draft> updateDraft(@RequestParam("taskId") Long taskId,
                                             @RequestBody String json){
        Draft dbDraft = draftService.findDraft(taskId, SecurityUtils.getCurrentUserLogin());

        if (dbDraft == null)
            dbDraft = new Draft();

        dbDraft.setJson(json);
        dbDraft.setTaskId(taskId);
        dbDraft.setUsername(SecurityUtils.getCurrentUserLogin());

        Draft result = draftService.save(dbDraft);
        return ResponseEntity.ok()
//                .headers(HeaderUtil.createEntityUpdateAlert("draft", result.getId().toString()))
                .headers(HeaderUtil.createAlert("Appunti salvati correttamente!", result.getId().toString()))
                .body(result);
    }

    /**
     * GET  /drafts : get all the drafts.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of drafts in body
     */
    @GetMapping(value = "/drafts",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Draft>> getAllDrafts() {
        log.debug("REST request to get all Drafts");
        List<Draft> drafts = draftService.findAll();

        return Optional.ofNullable(drafts)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    /**
     * GET  /drafts/:id : get the "id" draft.
     *
     * @param id the id of the draft to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the draft, or with status 404 (Not Found)
     */
    @GetMapping(value = "/draft/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Draft> getDraftById(@PathVariable Long id) {
        log.debug("REST request to get Draft : {}", id);
        Draft draft = draftService.findOne(id);
        return Optional.ofNullable(draft)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET  /drafts/getDraftByTaskId : restituisce i draft associati ad un utente
     *
     * @param taskId   the id of the draft to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the draft, or with status 404 (Not Found)
     */
    @GetMapping(value = "/draft/getDraftByTaskId",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Draft> getDraftByTaskId(@RequestParam("taskId") Long taskId) {
        log.debug("REST request to get Draft by taskId : {}", taskId);

        Draft draft = draftService.findDraft(taskId, SecurityUtils.getCurrentUserLogin());

        return Optional.ofNullable(draft)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /draft/:id : delete the "id" Draft.
     *
     * @param id the id of the Draft to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/draft/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        log.debug("REST request to delete Draft : {}", id);
        draftService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("draft", id.toString())).build();
    }
}