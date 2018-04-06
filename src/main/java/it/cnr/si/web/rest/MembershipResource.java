package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Membership;
import it.cnr.si.domain.Relationship;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.web.rest.util.HeaderUtil;
import it.cnr.si.web.rest.util.PaginationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import java.util.Set;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.RoleOiv.coordinator;
import static it.cnr.si.flows.ng.utils.Enum.RoleOiv.member;

/**
 * REST controller for managing Membership.
 */
@RestController
@RequestMapping("/api")
public class MembershipResource {

    private final Logger log = LoggerFactory.getLogger(MembershipResource.class);

    @Inject
    private MembershipService membershipService;
    @Inject
    private RelationshipService relationshipService;

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
        //se cerco di creare una relationshi con username e groupname uguale ad una che già esiste restituisco errore
        if (membershipService.findOneByUsernameAndGroupname(membership.getUsername(), membership.getGroupname()) != null)
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("membership", "A membership with this Username AND groupname already exist", "A membership with this Username AND groupname already exist")).body(null);

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


    @RequestMapping(value = "/memberships/groupsWithRoleCoordinator",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Membership>> getGroupsWithRoleCoordinator(Pageable pageable) throws URISyntaxException {

        String user = SecurityUtils.getCurrentUserLogin();
        log.debug("REST request dei grutti di cui è coordinator l'utente {}", user);

        Page<Membership> pageCoordinator = membershipService.getGroupsWithRole(pageable, user, coordinator.name());
        Page<Membership> pageMember = membershipService.getGroupsWithRole(pageable, user, member.name());

        List<Membership> userGroup = membershipService.findAll(pageable).getContent().stream()
                .filter(membership -> membership.getUsername().equals(user))
                .collect(Collectors.toList());

        for (Membership membership : pageMember.getContent()) {
            String groupname = membership.getGroupname();

            Set<Relationship> relationships = relationshipService.getAllRelationshipForGroup(groupname);
            for (Relationship relationship : relationships) {
                if (relationship.getGroupRole().equals(coordinator.name())) {

                    Membership membershipFromRelationship = new Membership();

                    membershipFromRelationship.setUsername(membership.getUsername());
                    membershipFromRelationship.setGrouprole(coordinator.name());
                    membershipFromRelationship.setGroupname(relationship.getGroupRelationship());

                    userGroup.add(membershipFromRelationship);
                }
            }
        }
        userGroup.addAll(pageCoordinator.getContent());
        userGroup = userGroup.stream().distinct().collect(Collectors.toList());

        PageImpl<Membership> resultPage = new PageImpl<>(userGroup, pageable, userGroup.size());

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(pageCoordinator, "/api/memberships");
        return Optional.ofNullable(resultPage.getContent())
                .map(result -> new ResponseEntity<>(
                        result,
                        headers,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @RequestMapping(value = "/memberships/groupMembersByGroupName",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Membership>> getGroupMembersByGroupName(Pageable pageable, @RequestParam("groupName") String groupName) throws URISyntaxException {

        List<Membership> members = membershipService.getMembershipByGroupName(groupName);

        List<Relationship> relationships = relationshipService.getRelationshipsForGroupRelationship(groupName);

        for (Relationship relationship : relationships) {
            List<Membership> membershipForRelationship = membershipService.getMembershipByGroupName(relationship.getGroupName());
//            setto il grouprole e il groupName come indicato dalla relationship
            for (Membership membership : membershipForRelationship) {
                membership.setGrouprole(relationship.getGroupRole());
                membership.setGroupname(relationship.getGroupName());
                membership.setId(null);
            }
            members.addAll(membershipForRelationship);
        }
        members = members.stream().distinct().collect(Collectors.toList());

        PageImpl results = new PageImpl(members);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(results, "/api/memberships");

        return Optional.ofNullable(members)
                .map(result -> new ResponseEntity<>(
                        result,
                        headers,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}
