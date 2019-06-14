package it.cnr.si.flows.ng.resource;

import it.cnr.si.flows.ng.ldap.LdapPersonToSearchResultMapper;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.FlowsLdapAccountService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import org.activiti.engine.ManagementService;
import org.activiti.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.ContainerCriteria;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/lookup")
@Profile("cnr")
public class FlowsLookupResource {

    @Inject
    private LdapTemplate ldapTemplate;
    @Inject
    private AceBridgeService aceService;
    @Inject
    private ManagementService managementService;
    @Inject
    private RestResponseFactory restResponseFactory;
    @Inject
    private FlowsLdapAccountService flowsLdapAccountService;
    @Inject
    private SiperService siperService;

    @RequestMapping(value = "/ace/user/{username:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getAce(@PathVariable String username) {
        return aceService.getAceGroupsForUser(username);
    }

    @RequestMapping(value = "/ace/usersingroup/{groupname:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public List<String> getAceGroup(@PathVariable String groupname) {
        return aceService.getUsersInAceGroup(groupname);
    }

    @RequestMapping(value = "/ace/groupdetail/{id:.+}", method = RequestMethod.GET)
    @Secured(AuthoritiesConstants.ADMIN)
    public String getAceGroupDetail(@PathVariable Integer id) {
        return aceService.getNomeStruturaById(id);
    }

    @RequestMapping(value = "/ace/uo/{id:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Utils.SearchResult> getUoById(@PathVariable Integer id) {

        EntitaOrganizzativaWebDto s = aceService.getUoById(id);
        Utils.SearchResult r = new Utils.SearchResult(s.getId().toString(), s.getCdsuo() +" - "+ s.getDenominazione());

        return ResponseEntity.ok(r);
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

        return ResponseEntity.ok(siperService.getResponsabileCDSUO(cdsuo));
    }
}
