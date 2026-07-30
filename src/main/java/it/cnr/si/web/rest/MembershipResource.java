package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Membership;
import it.cnr.si.domain.Relationship;

import it.cnr.si.service.CnrgroupService;
import it.cnr.si.service.FlowsUserService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.SecurityService;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;



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
    @Inject
    private CnrgroupService cnrgroupService;
    @Inject
    private FlowsUserService flowsUserService;
    @Inject
    private SecurityService securityService;



    /**
     * Metodo di creazione delle membership customizzato
     * (prende come parametri 3 stringhe e non l'oggetto "membership", in questo modo è più facile da richiamare da js)
     *
     * @param groupName the group name
     * @param userName  the user name
     * @param groupRole the group role
     * @return the response entity
     * @throws URISyntaxException the uri syntax exception
     */
    @RequestMapping(value = "/createMemberships",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Membership> myCreateMembership(@RequestParam("groupName") String groupName,
                                                         @RequestParam("userName") String userName,
                                                         @RequestParam("groupRole") String groupRole) throws URISyntaxException {

        log.debug("REST request to save Membership : groupName->{} , userName->{}, groupRole->{}", groupName, userName, groupRole);

        //se cerco di creare una relationship con username e groupname uguale ad una che già esiste restituisco errore
        if (membershipService.findOneByUsernameAndGroupname(userName, groupName) != null)
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("membership", "A membership with this Username AND groupname already exist", "A membership with this Username AND groupname already exist")).body(null);

        if(cnrgroupService.findCnrgroupByName(groupName) == null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("groupName", "A groupName with the name:"+ groupName + " doesn't exist", "A groupName with the name:"+ groupName + " doesn't exist")).body(null);
        }

        if(!flowsUserService.getUserWithAuthoritiesByLogin(userName).isPresent()) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("userName", "A userName with the name:"+ userName + " doesn't exist", "A userName with the name:"+ userName + " doesn't exist")).body(null);
        }

        Membership membership = new Membership();
        membership.setCnrgroup(cnrgroupService.findCnrgroupByName(groupName));
        membership.setGrouprole(groupRole);
        membership.setUser(flowsUserService.getUserWithAuthoritiesByLogin(userName).orElse(null));

        Membership result = membershipService.save(membership);
        return ResponseEntity.created(new URI("/api/memberships/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert("membership", result.getId().toString()))
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
     * Gets groups for user.
     *
     * @param pageable the pageable
     * @return the groups for user
     * @throws URISyntaxException the uri syntax exception
     */
    @RequestMapping(value = "/memberships/groupsForUser",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Membership>> getGroupsForUser(Pageable pageable) throws URISyntaxException {

        String user = securityService.getCurrentUserLogin();
        log.debug("REST request dei gruppi di cui è coordinator l'utente {}", user);

        List<Membership> userGroup = flowsUserService.getGroupsForUser(user, pageable);

        PageImpl<Membership> resultPage = new PageImpl<>(userGroup, pageable, userGroup.size());

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(resultPage, "/api/memberships");
        return Optional.ofNullable(resultPage.getContent())
                .map(result -> new ResponseEntity<>(
                        result,
                        headers,
                        HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }



    /**
     * Gets members by group name.
     *
     * @param pageable  the pageable
     * @param groupName the group name
     * @return the members by group name
     * @throws URISyntaxException the uri syntax exception
     */
    @RequestMapping(value = "/memberships/membersByGroupName",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Membership>> getMembersByGroupName(Pageable pageable, @RequestParam("groupName") String groupName) throws URISyntaxException {

        List<Membership> members = membershipService.getMembershipByGroupName(groupName);

        Set<Relationship> relationships = relationshipService.getRelationshipsForGroupRelationship(groupName);

        for (Relationship relationship : relationships) {
            List<Membership> membershipForRelationship = membershipService.getMembershipByGroupName(relationship.getGroupName());
//            setto il grouprole e il groupName come indicato dalla relationship
            for (Membership membership : membershipForRelationship) {
                membership.setGrouprole(relationship.getGroupRole());
                membership.setCnrgroup(cnrgroupService.findCnrgroupByName(relationship.getGroupName()));
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