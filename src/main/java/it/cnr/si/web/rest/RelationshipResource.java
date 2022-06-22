package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Relationship;
import it.cnr.si.repository.RelationshipRepository;
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
 * REST controller for managing Relationship.
 */
@RestController
@RequestMapping("/api")
public class RelationshipResource {

    private final Logger log = LoggerFactory.getLogger(RelationshipResource.class);

    @Inject
    private RelationshipRepository relationshipRepository;

    /**
     * POST  /relationships : Create a new relationship.
     *
     * @param relationship the relationship to create
     * @return the ResponseEntity with status 201 (Created) and with body the new relationship, or with status 400 (Bad Request) if the relationship has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/relationships",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Relationship> createRelationship(@Valid @RequestBody Relationship relationship) throws URISyntaxException {
        log.debug("REST request to save Relationship : {}", relationship);
        if (relationship.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("relationship", "idexists", "A new relationship cannot already have an ID")).body(null);
        }
        Relationship result = relationshipRepository.save(relationship);
        return ResponseEntity.created(new URI("/api/relationships/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("relationship", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /relationships : Updates an existing relationship.
     *
     * @param relationship the relationship to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated relationship,
     * or with status 400 (Bad Request) if the relationship is not valid,
     * or with status 500 (Internal Server Error) if the relationship couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/relationships",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Relationship> updateRelationship(@Valid @RequestBody Relationship relationship) throws URISyntaxException {
        log.debug("REST request to update Relationship : {}", relationship);
        if (relationship.getId() == null) {
            return createRelationship(relationship);
        }
        Relationship result = relationshipRepository.save(relationship);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("relationship", relationship.getId().toString()))
                .body(result);
    }

    /**
     * GET  /relationships : get all the relationships.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of relationships in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/relationships",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Relationship>> getAllRelationships(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Relationships");
        Page<Relationship> page = relationshipRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/relationships");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /relationships/:id : get the "id" relationship.
     *
     * @param id the id of the relationship to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the relationship, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/relationships/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Relationship> getRelationship(@PathVariable Long id) {
        log.debug("REST request to get Relationship : {}", id);
        Relationship relationship = relationshipRepository.findById(id).get();
        return Optional.ofNullable(relationship)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /relationships/:id : delete the "id" relationship.
     *
     * @param id the id of the relationship to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/relationships/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteRelationship(@PathVariable Long id) {
        log.debug("REST request to delete Relationship : {}", id);
        relationshipRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("relationship", id.toString())).build();
    }

}
