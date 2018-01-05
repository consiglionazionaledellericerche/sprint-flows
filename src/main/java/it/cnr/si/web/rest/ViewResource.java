package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.View;

import it.cnr.si.repository.ViewRepository;
import it.cnr.si.security.AuthoritiesConstants;
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
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing View.
 */
@RestController
@RequestMapping("/api")
public class ViewResource {

    private final Logger log = LoggerFactory.getLogger(ViewResource.class);

    @Inject
    private ViewRepository viewRepository;

    /**
     * POST  /views : Create a new view.
     *
     * @param view the view to create
     * @return the ResponseEntity with status 201 (Created) and with body the new view, or with status 400 (Bad Request) if the view has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/views",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<View> createView(@Valid @RequestBody View view) throws URISyntaxException {
        log.debug("REST request to save View : {}", view);
        if (view.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("view", "idexists", "A new view cannot already have an ID")).body(null);
        }
        View result = viewRepository.save(view);
        return ResponseEntity.created(new URI("/api/views/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("view", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /views : Updates an existing view.
     *
     * @param view the view to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated view,
     * or with status 400 (Bad Request) if the view is not valid,
     * or with status 500 (Internal Server Error) if the view couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/views",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<View> updateView(@Valid @RequestBody View view) throws URISyntaxException {
        log.debug("REST request to update View : {}", view);
        if (view.getId() == null) {
            return createView(view);
        }
        View result = viewRepository.save(view);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("view", view.getId().toString()))
            .body(result);
    }

    /**
     * GET  /views : get all the views.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of views in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/views",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<View>> getAllViews(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Views");
        Page<View> page = viewRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/views");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /views/:id : get the "id" view.
     *
     * @param id the id of the view to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the view, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/views/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<View> getView(@PathVariable Long id) {
        log.debug("REST request to get View : {}", id);
        View view = viewRepository.findOne(id);
        return Optional.ofNullable(view)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET  /views/:id : get the "id" view.
     *
     * @param id the id of the view to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the view, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/views/{processId}/{type}",
        method = RequestMethod.GET,
        produces = MediaType.TEXT_HTML_VALUE)
    @Timed
    public ResponseEntity<String> getViewByProcessidType(@PathVariable String processId, @PathVariable String type) {
        log.debug("REST request to get View for Name&Type : {} {}", processId, type);
        View view = viewRepository.getViewByProcessidType(processId, type);
        return Optional.ofNullable(view)
            .map(result -> new ResponseEntity<>(
                result.getView(),
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /views/:id : delete the "id" view.
     *
     * @param id the id of the view to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/views/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> deleteView(@PathVariable Long id) {
        log.debug("REST request to delete View : {}", id);
        viewRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("view", id.toString())).build();
    }

}
