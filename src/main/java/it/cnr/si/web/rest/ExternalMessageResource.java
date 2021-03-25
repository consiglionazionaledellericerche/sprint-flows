package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.web.rest.util.HeaderUtil;
import it.cnr.si.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing ExternalMessage.
 */
@RestController
@RequestMapping("/api")
public class ExternalMessageResource {

    public String EXTERNAL_MESSAGE = "externalMessage";
    private final Logger log = LoggerFactory.getLogger(ExternalMessageResource.class);
        
    @Inject
    private ExternalMessageService externalMessageService;

    /**
     * POST  /external-messages : Create a new externalMessage.
     *
     * @param externalMessage the externalMessage to create
     * @return the ResponseEntity with status 201 (Created) and with body the new externalMessage, or with status 400 (Bad Request) if the externalMessage has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(value = "/external-messages",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<ExternalMessage> createExternalMessage(@Valid @RequestBody ExternalMessage externalMessage) throws URISyntaxException {
        log.debug("REST request to save ExternalMessage : {}", externalMessage);
        if (externalMessage.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(EXTERNAL_MESSAGE, "idexists", "A new externalMessage cannot already have an ID")).body(null);
        }
        ExternalMessage result = externalMessageService.save(externalMessage);
        return ResponseEntity.created(new URI("/api/external-messages/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(EXTERNAL_MESSAGE, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /external-messages : Updates an existing externalMessage.
     *
     * @param externalMessage the externalMessage to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated externalMessage,
     * or with status 400 (Bad Request) if the externalMessage is not valid,
     * or with status 500 (Internal Server Error) if the externalMessage couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping(value = "/external-messages",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<ExternalMessage> updateExternalMessage(@Valid @RequestBody ExternalMessage externalMessage) throws URISyntaxException {
        log.debug("REST request to update ExternalMessage : {}", externalMessage);
        if (externalMessage.getId() == null) {
            return createExternalMessage(externalMessage);
        }
        ExternalMessage result = externalMessageService.save(externalMessage);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(EXTERNAL_MESSAGE, externalMessage.getId().toString()))
            .body(result);
    }

    /**
     * GET  /external-messages : get all the externalMessages.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of externalMessages in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping(value = "/external-messages",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<ExternalMessage>> getAllExternalMessages(@RequestParam(name = "searchTerms", required = false) String searchTerms,
                                                                        Pageable pageable) throws URISyntaxException {
        log.debug("REST request to get a page of ExternalMessages");
        Page<ExternalMessage> page = externalMessageService.findAll(searchTerms, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/external-messages");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /external-messages/:id : get the "id" externalMessage.
     *
     * @param id the id of the externalMessage to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the externalMessage, or with status 404 (Not Found)
     */
    @GetMapping(value = "/external-messages/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<ExternalMessage> getExternalMessage(@PathVariable Long id) {
        log.debug("REST request to get ExternalMessage : {}", id);
        ExternalMessage externalMessage = externalMessageService.findOne(id);
        return Optional.ofNullable(externalMessage)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /external-messages/:id : delete the "id" externalMessage.
     *
     * @param id the id of the externalMessage to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping(value = "/external-messages/{id}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteExternalMessage(@PathVariable Long id) {
        log.debug("REST request to delete ExternalMessage : {}", id);
        externalMessageService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(EXTERNAL_MESSAGE, id.toString())).build();
    }

}
