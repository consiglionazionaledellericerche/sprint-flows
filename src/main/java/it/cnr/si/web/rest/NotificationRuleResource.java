package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.NotificationRule;

import it.cnr.si.repository.NotificationRuleRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.web.rest.util.HeaderUtil;
import it.cnr.si.web.rest.util.PaginationUtil;

import org.apache.commons.lang3.BooleanUtils;
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
 * REST controller for managing NotificationRule.
 */
@RestController
@RequestMapping("/api")
public class NotificationRuleResource {

    private final Logger log = LoggerFactory.getLogger(NotificationRuleResource.class);

    @Inject
    private NotificationRuleRepository notificationRuleRepository;

    /**
     * POST  /notification-rules : Create a new notificationRule.
     *
     * @param notificationRule the notificationRule to create
     * @return the ResponseEntity with status 201 (Created) and with body the new notificationRule, or with status 400 (Bad Request) if the notificationRule has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/notification-rules",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<NotificationRule> createNotificationRule(@Valid @RequestBody NotificationRule notificationRule) throws URISyntaxException {
        log.debug("REST request to save NotificationRule : {}", notificationRule);
        if (notificationRule.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("notificationRule", "idexists", "A new notificationRule cannot already have an ID")).body(null);
        }

        // Boolean ha il terzo valore null, reminiscente di https://thedailywtf.com/articles/What_Is_Truth_0x3f_ - mtrycz
        notificationRule.setPersona( BooleanUtils.isTrue(notificationRule.isPersona()) );

        NotificationRule result = notificationRuleRepository.save(notificationRule);
        return ResponseEntity.created(new URI("/api/notification-rules/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("notificationRule", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /notification-rules : Updates an existing notificationRule.
     *
     * @param notificationRule the notificationRule to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated notificationRule,
     * or with status 400 (Bad Request) if the notificationRule is not valid,
     * or with status 500 (Internal Server Error) if the notificationRule couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/notification-rules",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<NotificationRule> updateNotificationRule(@Valid @RequestBody NotificationRule notificationRule) throws URISyntaxException {
        log.debug("REST request to update NotificationRule : {}", notificationRule);
        if (notificationRule.getId() == null) {
            return createNotificationRule(notificationRule);
        }
        NotificationRule result = notificationRuleRepository.save(notificationRule);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("notificationRule", notificationRule.getId().toString()))
            .body(result);
    }

    /**
     * GET  /notification-rules : get all the notificationRules.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of notificationRules in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/notification-rules",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<List<NotificationRule>> getAllNotificationRules(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of NotificationRules");
        Page<NotificationRule> page = notificationRuleRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/notification-rules");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /notification-rules/:id : get the "id" notificationRule.
     *
     * @param id the id of the notificationRule to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the notificationRule, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/notification-rules/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<NotificationRule> getNotificationRule(@PathVariable Long id) {
        log.debug("REST request to get NotificationRule : {}", id);
        NotificationRule notificationRule = notificationRuleRepository.findById(id).get();
        return Optional.ofNullable(notificationRule)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /notification-rules/:id : delete the "id" notificationRule.
     *
     * @param id the id of the notificationRule to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/notification-rules/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> deleteNotificationRule(@PathVariable Long id) {
        log.debug("REST request to delete NotificationRule : {}", id);
        notificationRuleRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("notificationRule", id.toString())).build();
    }

}
