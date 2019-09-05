package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("api/search")
public class FlowsSearchResource {

    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private FlowsTaskService flowsTaskService;
    @Inject
    private Utils util;

    /**
     * Funzionalità di Ricerca delle Process Instances.
     *
     * @param params the params
     * @return le response entity frutto della ricerca
     */
    @PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> search(@RequestBody Map<String, String> params) {

        String processDefinitionKey = util.getString(params, "processDefinitionKey", "all");
        String order = util.getString(params, "order", "ASC");
        boolean active = util.getBoolean(params, "active", true);
        boolean isTaskQuery = util.getBoolean(params, "isTaskQuery", false);
        int page = util.getInteger(params, "page", 1);

        Integer maxResults = util.getInteger(params, "maxResult", 20);
        Integer firstResult = maxResults * (page-1) ;

        DataResponse result;

        if (isTaskQuery) {
            result = flowsTaskService.search(params, processDefinitionKey, active, order, firstResult, maxResults);
        } else {
            result = flowsProcessInstanceService.search(params, processDefinitionKey, active, order, firstResult, maxResults, false);
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
    @PostMapping(value = "/exportCsv/{processDefinitionKey}", headers = "Accept=application/vnd.ms-excel", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = "application/vnd.ms-excel")
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public void exportCsv(
            HttpServletResponse res,
            @PathVariable("processDefinitionKey") String processDefinitionKey,
            @RequestBody Map<String, String> params) throws IOException {

        String order = util.getString(params, "order", "ASC");
        boolean active = Boolean.parseBoolean(util.getString(params, "active", "true"));
        boolean isTaskQuery = util.getBoolean(params, "isTaskQuery", false);
        Integer firstResult = Integer.parseInt(util.getString(params, "firstResult", "0"));
        Integer maxResults = Integer.parseInt(util.getString(params, "maxResults", "99999"));

        DataResponse result;
        if (isTaskQuery)
            result = flowsTaskService.search(params, processDefinitionKey, active, order, firstResult, maxResults);
        else
            result = flowsProcessInstanceService.search(params, processDefinitionKey, active, order, firstResult, maxResults, true);

        flowsTaskService.buildCsv(
                (List<HistoricProcessInstanceResponse>) result.getData(),
                res.getWriter(), processDefinitionKey);
    }
}
