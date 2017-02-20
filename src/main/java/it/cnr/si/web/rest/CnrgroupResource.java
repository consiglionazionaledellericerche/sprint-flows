package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Cnrgroup;
import it.cnr.si.service.CnrgroupService;
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
 * REST controller for managing Cnrgroup.
 */
@RestController
@RequestMapping("/api")
public class CnrgroupResource {

    private final Logger log = LoggerFactory.getLogger(CnrgroupResource.class);
        
    @Inject
    private CnrgroupService cnrgroupService;

    /**
     * POST  /cnrgroups : Create a new cnrgroup.
     *
     * @param cnrgroup the cnrgroup to create
     * @return the ResponseEntity with status 201 (Created) and with body the new cnrgroup, or with status 400 (Bad Request) if the cnrgroup has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/cnrgroups",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Cnrgroup> createCnrgroup(@Valid @RequestBody Cnrgroup cnrgroup) throws URISyntaxException {
        log.debug("REST request to save Cnrgroup : {}", cnrgroup);
        if (cnrgroup.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("cnrgroup", "idexists", "A new cnrgroup cannot already have an ID")).body(null);
        }
        Cnrgroup result = cnrgroupService.save(cnrgroup);
        return ResponseEntity.created(new URI("/api/cnrgroups/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("cnrgroup", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /cnrgroups : Updates an existing cnrgroup.
     *
     * @param cnrgroup the cnrgroup to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated cnrgroup,
     * or with status 400 (Bad Request) if the cnrgroup is not valid,
     * or with status 500 (Internal Server Error) if the cnrgroup couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/cnrgroups",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Cnrgroup> updateCnrgroup(@Valid @RequestBody Cnrgroup cnrgroup) throws URISyntaxException {
        log.debug("REST request to update Cnrgroup : {}", cnrgroup);
        if (cnrgroup.getId() == null) {
            return createCnrgroup(cnrgroup);
        }
        Cnrgroup result = cnrgroupService.save(cnrgroup);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("cnrgroup", cnrgroup.getId().toString()))
            .body(result);
    }

    /**
     * GET  /cnrgroups : get all the cnrgroups.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of cnrgroups in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/cnrgroups",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Cnrgroup>> getAllCnrgroups(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Cnrgroups");
        Page<Cnrgroup> page = cnrgroupService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/cnrgroups");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /cnrgroups/:id : get the "id" cnrgroup.
     *
     * @param id the id of the cnrgroup to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the cnrgroup, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/cnrgroups/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Cnrgroup> getCnrgroup(@PathVariable Long id) {
        log.debug("REST request to get Cnrgroup : {}", id);
        Cnrgroup cnrgroup = cnrgroupService.findOne(id);
        return Optional.ofNullable(cnrgroup)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /cnrgroups/:id : delete the "id" cnrgroup.
     *
     * @param id the id of the cnrgroup to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/cnrgroups/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteCnrgroup(@PathVariable Long id) {
        log.debug("REST request to delete Cnrgroup : {}", id);
        cnrgroupService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("cnrgroup", id.toString())).build();
    }

}
