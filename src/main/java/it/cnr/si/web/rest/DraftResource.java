package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Draft;
import it.cnr.si.service.DraftService;
import it.cnr.si.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.net.URISyntaxException;
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
     *     nel caso in cui non si specifichi l`username si recupera un Draft che può essere letto da tutti,
     *     viceversa, se si specifica uno username, il Draft potrà essere letto solo da qurello username
     * @param taskId   the draft to update
     * @param json     the json
     * @param username the username
     * @return the ResponseEntity with status 200 (OK) and with body the updated draft, or with status 400 (Bad Request) if the draft is not valid, or with status 500 (Internal Server Error) if the draft couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/drafts/updateDraft",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Draft> updateDraft(@RequestParam("taskId") Long taskId, @RequestParam("json") String json, @RequestParam("username") String username) throws URISyntaxException {
        Draft dbDraft = draftService.findDraft(taskId, username);

        if (dbDraft == null) {
            dbDraft = new Draft();
        }
        dbDraft.setJson(json);
        dbDraft.setTaskId(taskId);
        dbDraft.setUsername(username.isEmpty() ? null : username);

        Draft result = draftService.save(dbDraft);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("draft", result.getId().toString()))
                .body(result);
    }

    /**
     * GET  /drafts : get all the drafts.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of drafts in body
     */
    @RequestMapping(value = "/drafts",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Draft> getAllDrafts() {
        log.debug("REST request to get all Drafts");
        List<Draft> drafts = draftService.findAll();
        return drafts;
    }


    /**
     * GET  /drafts/:id : get the "id" draft.
     *
     * @param id the id of the draft to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the draft, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/draft/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Draft> getDraft(@PathVariable Long id) {
        log.debug("REST request to get Draft : {}", id);
        Draft draft = draftService.findOne(id);
        return Optional.ofNullable(draft)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET  /drafts/:id : get the "id" draft.
     *
     * @param taskId   the id of the draft to retrieve
     * @param username the username
     * @return the ResponseEntity with status 200 (OK) and with body the draft, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/draft/getDraftByTaskId",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Draft> getDraftByTaskId(@RequestParam("taskId") Long taskId, @RequestParam("username") String username) {
        log.debug("REST request to get Draft by taskId : {}", taskId);
        Draft draft = draftService.findDraft(taskId, username);

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
    @RequestMapping(value = "/draft/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        log.debug("REST request to delete Draft : {}", id);
        draftService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("draft", id.toString())).build();
    }
}