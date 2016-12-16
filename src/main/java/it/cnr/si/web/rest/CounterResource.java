package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Counter;

import it.cnr.si.repository.CounterRepository;
import it.cnr.si.web.rest.util.HeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * REST controller for managing Counter.
 */
@RestController
@RequestMapping("/api")
public class CounterResource {

    private final Logger log = LoggerFactory.getLogger(CounterResource.class);
        
    @Inject
    private CounterRepository counterRepository;

    /**
     * POST  /counters : Create a new counter.
     *
     * @param counter the counter to create
     * @return the ResponseEntity with status 201 (Created) and with body the new counter, or with status 400 (Bad Request) if the counter has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/counters",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Counter> createCounter(@RequestBody Counter counter) throws URISyntaxException {
        log.debug("REST request to save Counter : {}", counter);
        if (counter.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("counter", "idexists", "A new counter cannot already have an ID")).body(null);
        }
        Counter result = counterRepository.save(counter);
        return ResponseEntity.created(new URI("/api/counters/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("counter", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /counters : Updates an existing counter.
     *
     * @param counter the counter to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated counter,
     * or with status 400 (Bad Request) if the counter is not valid,
     * or with status 500 (Internal Server Error) if the counter couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/counters",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Counter> updateCounter(@RequestBody Counter counter) throws URISyntaxException {
        log.debug("REST request to update Counter : {}", counter);
        if (counter.getId() == null) {
            return createCounter(counter);
        }
        Counter result = counterRepository.save(counter);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("counter", counter.getId().toString()))
            .body(result);
    }

    /**
     * GET  /counters : get all the counters.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of counters in body
     */
    @RequestMapping(value = "/counters",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public List<Counter> getAllCounters() {
        log.debug("REST request to get all Counters");
        List<Counter> counters = counterRepository.findAll();
        return counters;
    }

    /**
     * GET  /counters/:id : get the "id" counter.
     *
     * @param id the id of the counter to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the counter, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/counters/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Counter> getCounter(@PathVariable Long id) {
        log.debug("REST request to get Counter : {}", id);
        Counter counter = counterRepository.findOne(id);
        return Optional.ofNullable(counter)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /counters/:id : delete the "id" counter.
     *
     * @param id the id of the counter to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/counters/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteCounter(@PathVariable Long id) {
        log.debug("REST request to delete Counter : {}", id);
        counterRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("counter", id.toString())).build();
    }

}
