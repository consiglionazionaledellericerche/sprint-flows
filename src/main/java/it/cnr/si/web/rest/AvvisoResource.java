package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Avviso;
import it.cnr.si.repository.AvvisoRepository;
import it.cnr.si.service.AvvisoService;
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
 * REST controller for managing Avviso.
 */
@RestController
@RequestMapping("/api")
public class AvvisoResource {

    private final Logger log = LoggerFactory.getLogger(AvvisoResource.class);
        
    @Inject
    private AvvisoService avvisoService;


    /**
     * POST  /avvisos : Create a new avviso.
     *
     * @param avviso the avviso to create
     * @return the ResponseEntity with status 201 (Created) and with body the new avviso, or with status 400 (Bad Request) if the avviso has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/avvisos",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Avviso> createAvviso(@Valid @RequestBody Avviso avviso) throws URISyntaxException {
        log.debug("REST request to save Avviso : {}", avviso);
        if (avviso.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("avviso", "idexists", "A new avviso cannot already have an ID")).body(null);
        }
        Avviso result = avvisoService.save(avviso);
        return ResponseEntity.created(new URI("/api/avvisos/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("avviso", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /avvisos : Updates an existing avviso.
     *
     * @param avviso the avviso to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated avviso,
     * or with status 400 (Bad Request) if the avviso is not valid,
     * or with status 500 (Internal Server Error) if the avviso couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/avvisos",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Avviso> updateAvviso(@Valid @RequestBody Avviso avviso) throws URISyntaxException {
        log.debug("REST request to update Avviso : {}", avviso);
        if (avviso.getId() == null) {
            return createAvviso(avviso);
        }
        Avviso result = avvisoService.save(avviso);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("avviso", avviso.getId().toString()))
            .body(result);
    }

    /**
     * GET  /avvisos : get all the avvisos.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of avvisos in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/avvisos",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Avviso>> getAllAvvisos(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Avvisos");
        Page<Avviso> page = avvisoService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/avvisos");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /avvisos/:id : get the "id" avviso.
     *
     * @param id the id of the avviso to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the avviso, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/avvisos/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Avviso> getAvviso(@PathVariable Long id) {
        log.debug("REST request to get Avviso : {}", id);
        Avviso avviso = avvisoService.findOne(id);
        return Optional.ofNullable(avviso)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /avvisos/:id : delete the "id" avviso.
     *
     * @param id the id of the avviso to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/avvisos/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteAvviso(@PathVariable Long id) {
        log.debug("REST request to delete Avviso : {}", id);
        avvisoService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("avviso", id.toString())).build();
    }

}
