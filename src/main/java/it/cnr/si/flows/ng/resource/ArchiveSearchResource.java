package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.ArchiveProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;

import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.apache.commons.io.FileUtils;
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

import static it.cnr.si.flows.ng.utils.Enum.Azione.GenerazioneDaSistema;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("api/archive")
public class ArchiveSearchResource {

    @Inject
    private Utils util;
    @Inject
    private ArchiveProcessInstanceService archiveProcessInstanceService;
    
    /**
     * Funzionalità di Ricerca delle Process Instances.
     *
     * @param params the params
     * @return le response entity frutto della ricerca
     */
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<DataResponse> search(@RequestBody Map<String, String> params) {

        String processDefinitionKey = util.getString(params, "processDefinitionKey", "all");
        String order = util.getString(params, "order", "ASC");
        boolean active = util.getBoolean(params, "active", true);
        int page = util.getInteger(params, "page", 1);

        Integer maxResults = util.getInteger(params, "maxResult", 20);
        Integer firstResult = maxResults * (page-1) ;

        DataResponse result;

        result = archiveProcessInstanceService.search(params, processDefinitionKey, active, order, firstResult, maxResults, true);
        return ResponseEntity.ok(result);
    }
}
