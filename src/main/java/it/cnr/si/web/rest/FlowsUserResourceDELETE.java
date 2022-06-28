package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.config.Constants;
import it.cnr.si.domain.Authority;
import it.cnr.si.domain.FlowsUser;
import it.cnr.si.flows.ng.dto.FlowsUserDto;
import it.cnr.si.flows.ng.service.FlowsMailService;
import it.cnr.si.flows.ng.utils.Utils.SearchResult;
import it.cnr.si.repository.FlowsUserRepository;
import it.cnr.si.security.AuthoritiesConstants;

import it.cnr.si.service.FlowsUserService;
import it.cnr.si.service.SecurityService;
import it.cnr.si.web.rest.util.HeaderUtil;
import it.cnr.si.web.rest.util.PaginationUtil;
import org.activiti.rest.service.api.RestResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * REST controller per la gestione FlowsUser: usiamo un "nostro" REST controller
 * (e non quello di sprint-core) perchè abbiamo dei campi customizzati (phone).
 */
@RestController
@RequestMapping("/api/flows")
public class FlowsUserResourceDELETE {

	public static final String USER_MANAGEMENT = "userManagement";
	public static final String USER_EXISTS = "userexists";
	public static final String LOGIN_ALREADY_IN_USE = "Login already in use";
	public static final String EMAIL_EXISTS = "emailexists";
	private final Logger log = LoggerFactory.getLogger(UserResource.class);

    @Inject
    private FlowsUserRepository flowsUserRepository;
    @Inject
    private FlowsMailService flowsMailService;
    @Inject
    private FlowsUserService flowsUserService;
    @Inject
    private RestResponseFactory restResponseFactory;
    @Inject
    private SecurityService securityService;


    /**
     * POST  /users  : Crea un nuovo FlowsUser.
     * <p>
     * Crea un nuovo FlowsUser se il login e la email non sono usati da altri FlowsUser
     * </p>
     *
     * @param flowsUser il FlowsUser da creare
     * @param request   HTTP request
     * @return la ResponseEntity con status 201 (Creato) e con il nuovo FlowsUser nel body, o con status 400 (Bad Request) se il login o la email sono già in uso
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/users",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<FlowsUser> createUser(
            @RequestBody FlowsUserDto flowsUser,
            HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to save User : {}", flowsUser);

        //Lowercase the user login before comparing with database
        if (flowsUserRepository.findOneByLogin(flowsUser.getLogin().toLowerCase()).isPresent()) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert(USER_MANAGEMENT, USER_EXISTS, LOGIN_ALREADY_IN_USE))
                    .body(null);
        } else if (flowsUserRepository.findOneByEmail(flowsUser.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .headers(HeaderUtil.createFailureAlert(USER_MANAGEMENT, EMAIL_EXISTS, "Email already in use"))
                    .body(null);
        } else {
            FlowsUserDto flowsUserDto = new FlowsUserDto(flowsUser.getId(), flowsUser.getLogin(), flowsUser.getPassword(), flowsUser.getFirstName(),
                                                         flowsUser.getLastName(), flowsUser.getEmail(), flowsUser.isActivated(), flowsUser.getLangKey(),
                                                         flowsUser.getAuthorities(), flowsUser.getCreatedDate(), flowsUser.getLastModifiedBy(),
                                                         flowsUser.getLastModifiedDate(), flowsUser.getPhone(), flowsUser.getGender());

            FlowsUser newFlowsUser = flowsUserService.createUser(flowsUserDto);

            return ResponseEntity.created(new URI("/api/users/" + newFlowsUser.getLogin()))
                    .headers(HeaderUtil.createAlert("userManagement.created", newFlowsUser.getLogin()))
                    .body(newFlowsUser);
        }
    }


    /**
     * PUT  /users : Aggiorna un FlowsUser che esiste.
     *
     * @param flowsUserDto il FlowsUser da aggiornare
     * @return la ResponseEntity con status 200 (OK) ed il FlowsUser aggiornato nel body,
     * o con status 400 (Bad Request) se il login o la email è già in uso,
     * o con status 500 (Internal Server Error) se lo FlowsUser non può essere aggiornato (ad es.: si è avviata l'app con il profilo "cnr"),
     */
    @RequestMapping(value = "/users",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Profile(value = {"!cnr", "oiv"})
    public ResponseEntity<FlowsUserDto> updateUser(@RequestBody FlowsUserDto flowsUserDto) {
        //se l'utente ha ROLE_ADMIN può fare tutto
        if (securityService.isCurrentUserInRole(AuthoritiesConstants.ADMIN)) {
            log.debug("L'utente {} sta modificando i campi dell'utente {}", securityService.getCurrentUserLogin(), flowsUserDto.getLogin());

            Optional<FlowsUser> existingUser = flowsUserRepository.findOneByEmail(flowsUserDto.getEmail());
            if (existingUser.isPresent() && (!existingUser.get().getId().equals(flowsUserDto.getId()))) {
                return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(USER_MANAGEMENT, EMAIL_EXISTS, "E-mail already in use")).body(null);
            }

            existingUser = flowsUserRepository.findOneByLogin(flowsUserDto.getLogin().toLowerCase());
            if (existingUser.isPresent() && (!existingUser.get().getId().equals(flowsUserDto.getId()))) {
                return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(USER_MANAGEMENT, USER_EXISTS, LOGIN_ALREADY_IN_USE)).body(null);
            }

            flowsUserService.updateUser(flowsUserDto.getId(), flowsUserDto.getLogin(), flowsUserDto.getFirstName(),
                                        flowsUserDto.getLastName(), flowsUserDto.getEmail(), flowsUserDto.isActivated(),
                                        flowsUserDto.getLangKey(), flowsUserDto.getAuthorities(), flowsUserDto.getPhone(), flowsUserDto.getGender());

            FlowsUserDto newFlowsUserDto = new FlowsUserDto(flowsUserDto.getId(), flowsUserDto.getLogin(), flowsUserDto.getPassword(),
                                                            flowsUserDto.getFirstName(), flowsUserDto.getLastName(), flowsUserDto.getEmail(),
                                                            flowsUserDto.isActivated(), flowsUserDto.getLangKey(), flowsUserDto.getAuthorities(),
                                                            flowsUserDto.getCreatedDate(), flowsUserDto.getLastModifiedBy(),
                                                            flowsUserDto.getLastModifiedDate(), flowsUserDto.getPhone(), flowsUserDto.getGender());
            return ResponseEntity.ok()
                    .headers(HeaderUtil.createAlert("userManagement.updated", flowsUserDto.getLogin()))
                    .body(newFlowsUserDto);
        } else {
            //ALTRIMENTI può modificare solo se stesso e solo alcuni campi (CAMPI IMMUTABILI: "id", "activate" e "authorities"
            // (sarà sempre "ROLE_USER" perchè solo gli utenti con questo ruolo eseguono questo ramo del codice))
            String username = securityService.getCurrentUserLogin();
            if (username.equals(flowsUserDto.getLogin())) {
                log.debug("L'utente {} sta modificando i propri campi", flowsUserDto.getLogin());
                Optional<FlowsUser> existingUser = flowsUserRepository.findOneByEmail(flowsUserDto.getEmail());
                if (existingUser.isPresent() && (!existingUser.get().getId().equals(flowsUserDto.getId()))) {
                    return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(USER_MANAGEMENT, EMAIL_EXISTS, "E-mail already in use")).body(null);
                }

                existingUser = flowsUserRepository.findOneByLogin(flowsUserDto.getLogin().toLowerCase());
                if (existingUser.isPresent() && (!existingUser.get().getId().equals(flowsUserDto.getId()))) {
                    return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(USER_MANAGEMENT, USER_EXISTS, LOGIN_ALREADY_IN_USE)).body(null);
                }

                Long id = existingUser.orElse(new FlowsUser()).getId();
                Set<String> authorities = new HashSet<>();
                authorities.add(AuthoritiesConstants.USER);

                flowsUserService.updateUser(id, flowsUserDto.getLogin(), flowsUserDto.getFirstName(), flowsUserDto.getLastName(), flowsUserDto.getEmail(),
                                            true, flowsUserDto.getLangKey(), authorities, flowsUserDto.getPhone(), flowsUserDto.getGender());

                FlowsUserDto newFlowsUserDto = new FlowsUserDto(id, username, flowsUserDto.getPassword(), flowsUserDto.getFirstName(),
                                                                flowsUserDto.getLastName(), flowsUserDto.getEmail(), true,
                                                                flowsUserDto.getLangKey(), authorities, flowsUserDto.getCreatedDate(),
                                                                flowsUserDto.getLastModifiedBy(), flowsUserDto.getLastModifiedDate(),
                                                                flowsUserDto.getPhone(), flowsUserDto.getGender());

                return ResponseEntity.ok()
                        .headers(HeaderUtil.createAlert("userManagement.updated", flowsUserDto.getLogin()))
                        .body(newFlowsUserDto);
            } else
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new FlowsUserDto());
        }
    }


    /**
     * GET  /users : get all FlowsUser.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and with body all FlowsUser
     * @throws URISyntaxException if the pagination headers couldn't be generated
     */
    @RequestMapping(value = "/users",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<FlowsUserDto>> getAllUsers(Pageable pageable)
            throws URISyntaxException {
        Page<FlowsUser> page = flowsUserRepository.findAllWithAuthorities(pageable);
        List<FlowsUserDto> flowsUserDtos = page.getContent().stream().map(
                this::getFlowsUserDto)
                .collect(Collectors.toList());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/users");
        return new ResponseEntity<>(flowsUserDtos, headers, HttpStatus.OK);
    }

    /**
     * GET  /users/:login : get the "login" FlowsUser.
     *
     * @param login the login of the FlowsUser to find
     * @return the ResponseEntity with status 200 (OK) and with body the "login" FlowsUser, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/users/{login:" + Constants.LOGIN_REGEX + "}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<FlowsUserDto> getUser(@PathVariable String login) {
        log.debug("REST request to get User : {}", login);
        return flowsUserService.getUserWithAuthoritiesByLogin(login).map(
                this::getFlowsUserDto)
                .map(flowsUserDto -> new ResponseEntity<>(flowsUserDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private FlowsUserDto getFlowsUserDto(FlowsUser flowsUserDto) {
        Set<String> authorities = flowsUserDto.getAuthorities().stream()
                .map(Authority::getName)
                .collect(Collectors.toSet());
        return new FlowsUserDto(flowsUserDto.getId(), flowsUserDto.getLogin(), flowsUserDto.getPassword(), flowsUserDto.getFirstName(),
                                flowsUserDto.getLastName(), flowsUserDto.getEmail(), flowsUserDto.getActivated(), flowsUserDto.getLangKey(),
                                authorities, flowsUserDto.getCreatedDate(), flowsUserDto.getLastModifiedBy(),
                                flowsUserDto.getLastModifiedDate(), flowsUserDto.getPhone(), flowsUserDto.getGender());
    }

    /**
     * DELETE /users/:login : delete the "login" FlowsUser.
     *
     * @param login the login of the FlowsUser to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/users/{login:" + Constants.LOGIN_REGEX + "}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Void> deleteUser(@PathVariable String login) {
        log.debug("REST request to delete User: {}", login);
        flowsUserService.deleteUser(login);
        return ResponseEntity.ok().headers(HeaderUtil.createAlert("userManagement.deleted", login)).build();
    }



    @RequestMapping(value = "/users/{username:.+}/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, Object>> searchUsers(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        //con il profilo "OIV" faccio la ricerca per l'autocompletamento degli utenti sul DB
        List<FlowsUser> result = flowsUserService.searchByLogin(username);
		List<SearchResult> search = result.stream()
                .map(user -> new SearchResult(user.getLogin(), user.getLogin()))
                .collect(Collectors.toList());

        response.put("more", search.size() > 10);
        response.put("results", search.stream().limit(10).collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }
}
