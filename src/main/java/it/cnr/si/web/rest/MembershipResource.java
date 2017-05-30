package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Membership;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.MembershipService;
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
 * REST controller for managing Membership.
 */
@RestController
@RequestMapping("/api")
@Secured(AuthoritiesConstants.ADMIN)
public class MembershipResource {

    private final Logger log = LoggerFactory.getLogger(MembershipResource.class);

    @Inject
    private MembershipService membershipService;

    /**
     * POST  /memberships : Create a new membership.
     *
     * @param membership the membership to create
     * @return the ResponseEntity with status 201 (Created) and with body the new membership, or with status 400 (Bad Request) if the membership has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/memberships",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Membership> createMembership(@Valid @RequestBody Membership membership) throws URISyntaxException {
        log.debug("REST request to save Membership : {}", membership);
        if (membership.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("membership", "idexists", "A new membership cannot already have an ID")).body(null);
        }
        Membership result = membershipService.save(membership);
        return ResponseEntity.created(new URI("/api/memberships/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("membership", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /memberships : Updates an existing membership.
     *
     * @param membership the membership to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated membership,
     * or with status 400 (Bad Request) if the membership is not valid,
     * or with status 500 (Internal Server Error) if the membership couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/memberships",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Membership> updateMembership(@Valid @RequestBody Membership membership) throws URISyntaxException {
        log.debug("REST request to update Membership : {}", membership);
        if (membership.getId() == null) {
            return createMembership(membership);
        }
        Membership result = membershipService.save(membership);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("membership", membership.getId().toString()))
            .body(result);
    }

    /**
     * GET  /memberships : get all the memberships.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of memberships in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/memberships",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Membership>> getAllMemberships(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Memberships");
        Page<Membership> page = membershipService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/memberships");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /memberships/:id : get the "id" membership.
     *
     * @param id the id of the membership to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the membership, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/memberships/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Membership> getMembership(@PathVariable Long id) {
        log.debug("REST request to get Membership : {}", id);
        Membership membership = membershipService.findOne(id);
        return Optional.ofNullable(membership)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /memberships/:id : delete the "id" membership.
     *
     * @param id the id of the membership to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/memberships/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Void> deleteMembership(@PathVariable Long id) {
        log.debug("REST request to delete Membership : {}", id);
        membershipService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("membership", id.toString())).build();
    }


    /**
     * GET  /memberships : get all the memberships.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of memberships in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/members/{groupName}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<String>> getMembersInGroup(@PathVariable String groupName) {
        log.debug("REST request to get Members in Group");
        List<String> result = membershipService.findMembersInGroup(groupName);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
