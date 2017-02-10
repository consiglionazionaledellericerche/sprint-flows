package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Cnrauthority;
import it.cnr.si.service.CnrauthorityService;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Cnrauthority.
 */
@RestController
@RequestMapping("/api")
public class CnrauthorityResource {

    private final Logger log = LoggerFactory.getLogger(CnrauthorityResource.class);
        
    @Inject
    private CnrauthorityService cnrauthorityService;

    /**
     * POST  /cnrauthorities : Create a new cnrauthority.
     *
     * @param cnrauthority the cnrauthority to create
     * @return the ResponseEntity with status 201 (Created) and with body the new cnrauthority, or with status 400 (Bad Request) if the cnrauthority has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/cnrauthorities",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Cnrauthority> createCnrauthority(@RequestBody Cnrauthority cnrauthority) throws URISyntaxException {
        log.debug("REST request to save Cnrauthority : {}", cnrauthority);
        if (cnrauthority.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("cnrauthority", "idexists", "A new cnrauthority cannot already have an ID")).body(null);
        }
        Cnrauthority result = cnrauthorityService.save(cnrauthority);
        return ResponseEntity.created(new URI("/api/cnrauthorities/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("cnrauthority", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /cnrauthorities : Updates an existing cnrauthority.
     *
     * @param cnrauthority the cnrauthority to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated cnrauthority,
     * or with status 400 (Bad Request) if the cnrauthority is not valid,
     * or with status 500 (Internal Server Error) if the cnrauthority couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/cnrauthorities",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Cnrauthority> updateCnrauthority(@RequestBody Cnrauthority cnrauthority) throws URISyntaxException {
        log.debug("REST request to update Cnrauthority : {}", cnrauthority);
        if (cnrauthority.getId() == null) {
            return createCnrauthority(cnrauthority);
        }
        Cnrauthority result = cnrauthorityService.save(cnrauthority);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("cnrauthority", cnrauthority.getId().toString()))
            .body(result);
    }

    /**
     * GET  /cnrauthorities : get all the cnrauthorities.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of cnrauthorities in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/cnrauthorities",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Cnrauthority>> getAllCnrauthorities(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Cnrauthorities");
        Page<Cnrauthority> page = cnrauthorityService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/cnrauthorities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /cnrauthorities/:id : get the "id" cnrauthority.
     *
     * @param id the id of the cnrauthority to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the cnrauthority, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/cnrauthorities/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Cnrauthority> getCnrauthority(@PathVariable Long id) {
        log.debug("REST request to get Cnrauthority : {}", id);
        Cnrauthority cnrauthority = cnrauthorityService.findOne(id);
        return Optional.ofNullable(cnrauthority)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /cnrauthorities/:id : delete the "id" cnrauthority.
     *
     * @param id the id of the cnrauthority to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/cnrauthorities/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteCnrauthority(@PathVariable Long id) {
        log.debug("REST request to delete Cnrauthority : {}", id);
        cnrauthorityService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("cnrauthority", id.toString())).build();
    }

}
