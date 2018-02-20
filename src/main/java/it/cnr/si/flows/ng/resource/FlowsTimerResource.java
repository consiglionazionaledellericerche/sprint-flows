package it.cnr.si.flows.ng.resource;


import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.listeners.oiv.service.GestioneTimerService;
import it.cnr.si.flows.ng.resource.FlowsUserResource.SearchResult;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.Job;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("api")
public class FlowsTimerResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTimerResource.class);

    public static final String BYTES = "bytes";
    @Inject
    private GestioneTimerService gestioneTimerService;
    @Inject
    private RestResponseFactory restResponseFactory;
    
    @RequestMapping(value = "/{processId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId, @flowsUserDetailsService)")
    @Timed
    public ResponseEntity<DataResponse> getTimer(@PathVariable("processId") String processInstanceId) throws IOException, ParseException {
    	String timerId = "x";

        List<Job> timerList = gestioneTimerService.getTimer(processInstanceId, timerId);
        LOGGER.info("timerList.size(): " + timerList.size());
        DataResponse response = new DataResponse();
        response.setSize(timerList.size());// numero di timer restituiti
        response.setData(restResponseFactory.createJobResponseList(timerList));
        return ResponseEntity.ok(response);
    }
    
}