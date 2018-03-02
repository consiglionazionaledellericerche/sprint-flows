package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("api/search")
public class FlowsSearchResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsSearchResource.class);

    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private FlowsTaskService flowsTaskService;

    /**
     * Funzionalità di Ricerca delle Process Instances.
     *
     * @param processInstanceId Il processInstanceId della ricerca
     * @param active            Boolean che indica se ricercare le Process Instances attive o terminate
     * @param order             L'ordine in cui vogliamo i risltati ('ASC' o 'DESC')
     * @return le response entity frutto della ricerca
     */
    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Object> search(@RequestBody Map<String, String> params) {

        String processDefinitionKey = getString(params, "processDefinitionKey", "all");
        String order = getString(params, "order", "ASC");
        boolean active = getBoolean(params, "active", true);
        boolean isTaskQuery = getBoolean(params, "isTaskQuery", false);
        int page = getInteger(params, "page", 1);

        Integer maxResults = 20;
        Integer firstResult = maxResults * (page-1) ;

        Map<String, Object> result;

        if (isTaskQuery) {
            result = flowsTaskService.search(params, processDefinitionKey, active, order, firstResult, maxResults);
        } else {
            result = flowsProcessInstanceService.search(params, processDefinitionKey, active, order, firstResult, maxResults);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Export csv: esporta il result-set di una search sulle Process Instances in un file Csv
     *
     * @param res                  the res
     * @param processDefinitionKey La process definition key della ricerca (oppurer "all")
     * @param params               i "parametri della ricerca
     * @throws IOException the io exception
     */
    @RequestMapping(value = "/exportCsv/{processDefinitionKey}", headers = "Accept=application/vnd.ms-excel", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = "application/vnd.ms-excel")
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public void exportCsv(
            HttpServletResponse res,
            @PathVariable("processDefinitionKey") String processDefinitionKey,
            @RequestBody Map<String, String> params) throws IOException {

        String order = getString(params, "order", "ASC");
        boolean active = Boolean.parseBoolean(getString(params, "active", "true"));
        boolean isTaskQuery = getBoolean(params, "isTaskQuery", false);
        Integer firstResult = Integer.parseInt(getString(params, "firstResult", "0"));
        Integer maxResults = Integer.parseInt(getString(params, "maxResults", "99999"));

        if (isTaskQuery) {
            Map<String, Object> result = flowsTaskService.search(
                    params, processDefinitionKey, active, order, firstResult, maxResults);

            flowsTaskService.buildCsv(
                    (List<HistoricTaskInstanceResponse>) result.get("tasks"),
                    res.getWriter(), processDefinitionKey);
        } else {
            Map<String, Object> result = flowsProcessInstanceService.search(
                    params, processDefinitionKey, active, order, firstResult, maxResults);

            flowsProcessInstanceService.buildCsv(
                    (List<HistoricProcessInstanceResponse>) result.get("processInstances"),
                    res.getWriter(), processDefinitionKey);
        }
    }

    /* */

    private static String getString(Map<String, String> params, String paramName, String defaultValue) {
        String value = params.get(paramName);
        return value != null ? value : defaultValue;
    }

    private static int getInteger(Map<String, String> params, String paramName, int defaultValue) {
        try {
            return Integer.parseInt( getString(params, paramName, String.valueOf(defaultValue)) ) ;
        } catch (NumberFormatException e) {
            LOGGER.info("Number Format Exception per il parametro "+ paramName +" con valore "+ params.get(paramName));
            return defaultValue;
        }
    }

    private static boolean getBoolean(Map<String, String> params, String paramName, boolean defaultValue) {
        return Boolean.parseBoolean( getString(params, paramName, String.valueOf(defaultValue)) ) ;
    }

}
