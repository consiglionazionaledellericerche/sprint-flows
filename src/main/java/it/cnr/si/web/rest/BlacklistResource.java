package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Blacklist;
import it.cnr.si.service.BlacklistService;
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
 * REST controller for managing Blacklist.
 */
@RestController
@RequestMapping("/api")
public class BlacklistResource {

    private final Logger log = LoggerFactory.getLogger(BlacklistResource.class);
        
    @Inject
    private BlacklistService blacklistService;

    /**
     * POST  /blacklists : Create a new blacklist.
     *
     * @param blacklist the blacklist to create
     * @return the ResponseEntity with status 201 (Created) and with body the new blacklist, or with status 400 (Bad Request) if the blacklist has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/blacklists",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Blacklist> createBlacklist(@Valid @RequestBody Blacklist blacklist) throws URISyntaxException {
        log.debug("REST request to save Blacklist : {}", blacklist);
        if (blacklist.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("blacklist", "idexists", "A new blacklist cannot already have an ID")).body(null);
        }
        Blacklist result = blacklistService.save(blacklist);
        return ResponseEntity.created(new URI("/api/blacklists/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("blacklist", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /blacklists : Updates an existing blacklist.
     *
     * @param blacklist the blacklist to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated blacklist,
     * or with status 400 (Bad Request) if the blacklist is not valid,
     * or with status 500 (Internal Server Error) if the blacklist couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/blacklists",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Blacklist> updateBlacklist(@Valid @RequestBody Blacklist blacklist) throws URISyntaxException {
        log.debug("REST request to update Blacklist : {}", blacklist);
        if (blacklist.getId() == null) {
            return createBlacklist(blacklist);
        }
        Blacklist result = blacklistService.save(blacklist);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("blacklist", blacklist.getId().toString()))
            .body(result);
    }

    /**
     * GET  /blacklists : get all the blacklists.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of blacklists in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/blacklists",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Blacklist>> getAllBlacklists(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Blacklists");
        Page<Blacklist> page = blacklistService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/blacklists");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /blacklists/:id : get the "id" blacklist.
     *
     * @param id the id of the blacklist to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the blacklist, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/blacklists/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Blacklist> getBlacklist(@PathVariable Long id) {
        log.debug("REST request to get Blacklist : {}", id);
        Blacklist blacklist = blacklistService.findById(id).get();
        return Optional.ofNullable(blacklist)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /blacklists/:id : delete the "id" blacklist.
     *
     * @param id the id of the blacklist to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/blacklists/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteBlacklist(@PathVariable Long id) {
        log.debug("REST request to delete Blacklist : {}", id);
        blacklistService.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("blacklist", id.toString())).build();
    }

}
