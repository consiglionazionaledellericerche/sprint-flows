package it.cnr.si.web.rest;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.domain.Counter;
import it.cnr.si.flows.ng.service.CounterService;

/**
 * REST controller for managing Counter.
 */
@RestController
@RequestMapping("/api")
public class CounterResource {

    private final Logger log = LoggerFactory.getLogger(CounterResource.class);

    @Inject
    private CounterService counterService;

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
        List<Counter> counters = counterService.findAll();
        return counters;
    }

}
