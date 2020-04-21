package it.cnr.si.flows.ng.resource;


import it.cnr.si.domain.ExternalProblem;
import it.cnr.si.flows.ng.utils.JSONResponseEntity;
import it.cnr.si.flows.ng.utils.proxy.CommonJsonRest;
import it.cnr.si.flows.ng.utils.proxy.RestServiceBean;
import it.cnr.si.flows.ng.utils.proxy.ResultProxy;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * REST controller for proxy to different application.
 */
@RestController
@RequestMapping("api/proxy/{app}")
public class FlowsProxyResource {

    private final Logger log = LoggerFactory.getLogger(FlowsProxyResource.class);

    public final static String PROXY_URL = "proxyURL";
    @Autowired
    private ProxyService proxyService;
    @Value("${spring.proxy.OIL.categoryScrivania}")
    private String categoryId;


    @RequestMapping(method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed(AuthoritiesConstants.USER)
    public ResponseEntity<String> get(@PathVariable String app, @RequestParam(value=PROXY_URL) String url, HttpServletRequest request, HttpServletResponse response) {
        if (log.isDebugEnabled())
        	// martin 21/04/2020 Sto rompendo il Proxy per far funzionare il Helpdesk
        	// se la cosa comporta problemi, l'unic omodo giusto di sistemarli e' di eliminare Proxy
        	// (e anche se non pone problemi)
            log.debug("GET from app: " + app + " with proxyURL: " + url + categoryId); 
        return process(HttpMethod.GET, null, app, url + categoryId, request, response);
    }

    @RequestMapping(method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed(AuthoritiesConstants.USER)
    public ResponseEntity<String> post(@RequestBody ExternalProblem body, @PathVariable String app, @RequestParam(value=PROXY_URL) String url, HttpServletRequest request, HttpServletResponse response) {
        if (log.isDebugEnabled())
            log.debug("POST from app: " + app + " with proxyURL: " + url);
        return process(HttpMethod.POST, body, app, url, request, response);
    }


    private ResponseEntity<String> process(HttpMethod httpMethod, ExternalProblem body, String app, String url, HttpServletRequest request, HttpServletResponse response) {
//todo: cache
        ResultProxy result = null;
        result = proxyService.process(httpMethod, body, app, url, request.getQueryString(), request.getHeader("x-proxy-authorization"));

        if (result.getStatus().compareTo(HttpStatus.OK) != 0){
            return JSONResponseEntity.getResponse(result.getStatus(), "");
        }
        response.setContentType(result.getType());
        response.setStatus(result.getStatus().value());
        String risposta = result.getBody();
        CommonJsonRest<RestServiceBean> commonJsonRest = result.getCommonJsonResponse();

        return JSONResponseEntity.ok(risposta);
    }
}
