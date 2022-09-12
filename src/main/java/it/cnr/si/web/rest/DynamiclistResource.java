package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Dynamiclist;
import it.cnr.si.repository.DynamiclistRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.DynamicListService;
import it.cnr.si.web.rest.util.HeaderUtil;
import it.cnr.si.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Dynamiclist.
 */
@RestController
@RequestMapping("/api")
public class DynamiclistResource {

    private final Logger log = LoggerFactory.getLogger(DynamiclistResource.class);

    @Inject
    private DynamiclistRepository dynamiclistRepository;
    @Autowired(required = false)
    DynamicListService dynamicListService;




    /**
     * POST  /dynamiclists : Create a new dynamiclist.
     *
     * @param dynamiclist the dynamiclist to create
     * @return the ResponseEntity with status 201 (Created) and with body the new dynamiclist, or with status 400 (Bad Request) if the dynamiclist has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/dynamiclists",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Dynamiclist> createDynamiclist(@Valid @RequestBody Dynamiclist dynamiclist) throws URISyntaxException {
        log.debug("REST request to save Dynamiclist : {}", dynamiclist);
        if (dynamiclist.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("dynamiclist", "idexists", "A new dynamiclist cannot already have an ID")).body(null);
        }
        Dynamiclist result = dynamiclistRepository.save(dynamiclist);
        return ResponseEntity.created(new URI("/api/dynamiclists/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("dynamiclist", result.getId().toString()))
                .body(result);
    }

    /**
     * PUT  /dynamiclists : Updates an existing dynamiclist.
     *
     * @param dynamiclist the dynamiclist to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated dynamiclist,
     * or with status 400 (Bad Request) if the dynamiclist is not valid,
     * or with status 500 (Internal Server Error) if the dynamiclist couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/dynamiclists",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Dynamiclist> updateDynamiclist(@Valid @RequestBody Dynamiclist dynamiclist) throws URISyntaxException {
        log.debug("REST request to update Dynamiclist : {}", dynamiclist);
        if (dynamiclist.getId() == null) {
            return createDynamiclist(dynamiclist);
        }
        Dynamiclist result = dynamiclistRepository.save(dynamiclist);
        return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert("dynamiclist", dynamiclist.getId().toString()))
                .body(result);
    }

    /**
     * GET  /dynamiclists : get all the dynamiclists.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of dynamiclists in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/dynamiclists",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Dynamiclist>> getAllDynamiclists(Pageable pageable)
            throws URISyntaxException {
        log.debug("REST request to get a page of Dynamiclists");
        Page<Dynamiclist> page = dynamiclistRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/dynamiclists");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /dynamiclists/:id : get the "id" dynamiclist.
     *
     * @param id the id of the dynamiclist to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the dynamiclist, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/dynamiclists/{id}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Dynamiclist> getDynamiclist(@PathVariable Long id) {
        log.debug("REST request to get Dynamiclist : {}", id);
        return dynamiclistRepository.findById(id)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    /**
     * Gets sigla dynamic list.
     *
     * @param name the name
     * @return the sigla dynamic list
     */
    @RequestMapping(value = "/sigladynamiclist/byname/{name}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Dynamiclist> getSiglaDynamicList(@PathVariable String name) {
        Dynamiclist dynamiclist = dynamicListService.getSiglaDynamicList(name);

        return Optional.ofNullable(dynamiclist)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @RequestMapping(value = "/dynamiclists/byname/{name}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Dynamiclist> getDynamiclistByName(@PathVariable String name) {
        log.debug("REST request to get Dynamiclist byName : {}", name);
        Dynamiclist dynamiclist = dynamiclistRepository.findOneByName(name);
        return Optional.ofNullable(dynamiclist)
                .map(result -> new ResponseEntity<>(
                        result,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    /**
     * DELETE  /dynamiclists/:id : delete the "id" dynamiclist.
     *
     * @param id the id of the dynamiclist to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/dynamiclists/{id}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> deleteDynamiclist(@PathVariable Long id) {
        log.debug("REST request to delete Dynamiclist : {}", id);
        dynamiclistRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("dynamiclist", id.toString())).build();
    }


}
