package it.cnr.si.flows.ng.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.security.AuthoritiesConstants;

@Controller
@RequestMapping("api/attachments")
public class FlowsAttachmentResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsAttachmentResource.class);

    @Autowired
    private HistoryService historyService;
    @Autowired
    private RuntimeService runtimeService;

    @RequestMapping(value = "{processInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<List<FlowsAttachment>> getAttachementsForProcessInstance(
            @PathVariable("processInstanceId") String processInstanceId) {

        Map<String, Object> processVariables = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .includeProcessVariables()
                .singleResult()
                .getProcessVariables();

        List<FlowsAttachment> result = processVariables.entrySet().stream()
                .filter(e -> e.getValue() instanceof FlowsAttachment)
                .map(e -> (FlowsAttachment) e.getValue())
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/history/{processInstanceId}/{attachmentName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<List<FlowsAttachment>> getAttachementHistory(
            @PathVariable("processInstanceId") String processInstanceId,
            @PathVariable("attachmentName") String attachmentName) {
        List<FlowsAttachment> result = new ArrayList<>();

        try {
            LOGGER.debug("Recupero la storia per il file: processInstanceId {}, name {}", processInstanceId, attachmentName);

            result = historyService.createHistoricDetailQuery()
                    .processInstanceId(processInstanceId)
                    .variableUpdates()
                    .orderByVariableRevision()
                    .excludeTaskDetails()
                    .asc()
                    .list()
                    .stream()
                    .map(h -> (HistoricDetailVariableInstanceUpdateEntity) h)
                    .filter(h -> h.getName().equals(attachmentName))
                    .map(h -> {
                        FlowsAttachment a = (FlowsAttachment) h.getValue();
                        a.setUrl("api/attachments/"+ h.getId() +"/data");
                        return a;
                    })
                    .map(h -> {h.setBytes(null); return h;})
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LOGGER.error("Errore nella creazione della storia del file: processInstanceId {}, name {}", processInstanceId, attachmentName);
            throw e;
        }
    }

    @RequestMapping(value = "{processInstanceId}/{attachmentName}/data", method = RequestMethod.GET)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public void getAttachment(
            HttpServletResponse response,
            @PathVariable("processInstanceId") String processInstanceId,
            @PathVariable("attachmentName") String attachmentName) throws IOException {

        FlowsAttachment attachment = runtimeService.getVariable(processInstanceId, attachmentName, FlowsAttachment.class);

        response.setContentLength(attachment.getBytes().length);

        ServletOutputStream output = response.getOutputStream();
        response.setContentType(attachment.getMimetype());
        ByteArrayInputStream baos = new ByteArrayInputStream(attachment.getBytes());
        IOUtils.copy(baos, output);
    }

    @RequestMapping(value = "{variableId}/data", method = RequestMethod.GET)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public void getHistoricAttachment(
            HttpServletResponse response,
            @PathVariable("variableId") String variableId) throws IOException {

        HistoricDetailVariableInstanceUpdateEntity variable = (HistoricDetailVariableInstanceUpdateEntity)
                historyService.createHistoricDetailQuery()
                .id(variableId)
                .singleResult();
        FlowsAttachment attachment = (FlowsAttachment) variable.getValue();

        response.setContentLength(attachment.getBytes().length);
        ServletOutputStream output = response.getOutputStream();
        response.setContentType(attachment.getMimetype());
        ByteArrayInputStream baos = new ByteArrayInputStream(attachment.getBytes());
        IOUtils.copy(baos, output);
    }

}
