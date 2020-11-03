package it.cnr.si.flows.ng.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import it.cnr.si.flows.ng.service.FlowsSiperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.ContainerCriteria;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import it.cnr.si.flows.ng.ldap.LdapPersonToSearchResultMapper;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.FlowsLdapAccountService;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;

@RestController
@RequestMapping("api/lookup")
@Profile("cnr")
public class FlowsLookupResource {

    private final Logger log = LoggerFactory.getLogger(FlowsLookupResource.class);

    @Inject
    private LdapTemplate ldapTemplate;
    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private FlowsLdapAccountService flowsLdapAccountService;
    @Inject
    private FlowsSiperService flowsSiperService;

    
    @RequestMapping(value = "/ace/boss", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Utils.SearchResult> getBossForCurrentUser() {
    	String username = SecurityUtils.getCurrentUserLogin();
    	BossDto boss = aceBridgeService.bossFirmatarioByUsername(username);
    	String fullname = boss.getUtente().getPersona().getNome() +" "+ boss.getUtente().getPersona().getCognome();
        return ResponseEntity.ok(new Utils.SearchResult(fullname, fullname));
    }
    
    @RequestMapping(value = "/ace/user/{username:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public Set<String> getAce(@PathVariable String username) {
        return aceBridgeService.getAceRolesForUser(username);
    }

    @RequestMapping(value = "/ace/usersingroup/{groupname:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public Set<String> getAceGroup(@PathVariable String groupname) {
        return aceBridgeService.getUsersInAceGroup(groupname);
    }

    @RequestMapping(value = "/ace/groupdetail/{id:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public String getAceGroupDetail(@PathVariable Integer id) {
        return aceBridgeService.getNomeStruturaById(id);
    }

    @RequestMapping(value = "/ace/uo/{id:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Utils.SearchResult> getUoById(@PathVariable Integer id) {

        SimpleEntitaOrganizzativaWebDto s = aceBridgeService.getUoById(id);
        Utils.SearchResult r = new Utils.SearchResult(s.getId().toString(), s.getCdsuo() +" - "+ s.getDenominazione());

        return ResponseEntity.ok(r);
    }

    @RequestMapping(value = "/ace/user/cdsuoabilitate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<List<Utils.SearchResult>> getCdsUoAbilitate() {

        List<Utils.SearchResult> CDSUOs = SecurityUtils.getCurrentUserAuthorities().stream()
                .map(Utils::removeLeadingRole)
                .filter(role -> role.startsWith("staffAmministrativo"))
                .map(role -> role.split("@")[1])
                .map(idEo -> {
                    Integer id = Integer.parseInt(idEo);
                    return aceBridgeService.getStrutturaById(id);
                })
                .map(eo -> {
                    return new Utils.SearchResult(String.valueOf(eo.getId()), eo.getCdsuo() +" - "+ eo.getDenominazione());
                }).collect(Collectors.toList());

        return ResponseEntity.ok(CDSUOs);
    }

    @RequestMapping(value = "/ldap/user/{username:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Utils.SearchResult> getUserByUsername(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        ContainerCriteria criteria = LdapQueryBuilder.query().where("uid").is(username);
        List<Utils.SearchResult> result = ldapTemplate.search( criteria, new LdapPersonToSearchResultMapper());

        return ResponseEntity.ok(result.get(0));
    }

    @RequestMapping(value = "/ldap/userfull/{username:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<Map<String, String>> getFullUserByUsername(@PathVariable String username) {

        Map<String, Object> response = new HashMap<>();

        List<Map<String, String>> result = flowsLdapAccountService.getFulluser(username);

        return ResponseEntity.ok(result.get(0));
    }

    @RequestMapping(value = "/siper/responsabilesede/{cdsuo:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.ADMIN)
    public ResponseEntity<List<Map<String, Object>>> getResponsabileSede(@PathVariable String cdsuo) {

        return ResponseEntity.ok(flowsSiperService.getResponsabileCDSUO(cdsuo));
    }

}
