package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.FlowsUserDetailsService;
import it.cnr.si.security.PermissionEvaluatorImpl;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Aggiornamento;
import static it.cnr.si.flows.ng.utils.Enum.Azione.Caricamento;

@Controller
@RequestMapping("api/attachments")
public class FlowsAttachmentResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsAttachmentResource.class);
    @Inject
    private HistoryService historyService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private TaskService taskService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private FlowsUserDetailsService flowsUserDetailsService;
    @Inject
    private PermissionEvaluatorImpl permissionEvaluator;
    @Inject
    private FlowsAttachmentService attachmentService;


    @RequestMapping(value = "{processInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, FlowsAttachment>> getAttachementsForProcessInstance(
            @PathVariable("processInstanceId") String processInstanceId) {

        Map<String, FlowsAttachment> result = flowsAttachmentService.getAttachementsForProcessInstance(processInstanceId);

        return ResponseEntity.ok(result);
    }


    @RequestMapping(value = "{processInstanceId}/getPublicDocuments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, FlowsAttachment>> getPublicDocumentsForProcessInstance(
            @PathVariable("processInstanceId") String processInstanceId) {

        Map<String, FlowsAttachment> result = null;//flowsAttachmentService.getPublicDocumentsForProcessInstance(processInstanceId);

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "task/{taskId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public ResponseEntity<Map<String, FlowsAttachment>> getAttachementsForTask(
            @PathVariable("taskId") String taskId) {

        String processInstanceId = taskService.createTaskQuery().taskId(taskId).singleResult().getProcessInstanceId();
        return getAttachementsForProcessInstance(processInstanceId);
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
                    .sorted( (l, r) -> l.getTime().compareTo(r.getTime()) )
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
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public void getAttachment(
            HttpServletResponse response,
            @PathVariable("processInstanceId") String processInstanceId,
            @PathVariable("attachmentName") String attachmentName) throws IOException {

        List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(attachmentName)
                .list();
        FlowsAttachment attachment = (FlowsAttachment) list.get(0).getValue();

        response.setContentLength(attachment.getBytes().length);
        ServletOutputStream output = response.getOutputStream();
        response.setContentType(attachment.getMimetype());
        ByteArrayInputStream baos = new ByteArrayInputStream(attachment.getBytes());
        IOUtils.copy(baos, output);
    }

    @RequestMapping(value = "{processInstanceId}/{attachmentName}/data", method = RequestMethod.POST)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @PreAuthorize("@permissionEvaluator.canUpdateAttachment(#processInstanceId, @flowsUserDetailsService)")
    @Timed
    public void updateAttachment(@PathVariable("processInstanceId") String processInstanceId,
                                 @PathVariable("attachmentName") String attachmentName,
                                 MultipartHttpServletRequest request) throws IOException {

        Map<String, Object> data = FlowsTaskService.extractParameters(request);
        String username = SecurityUtils.getCurrentUserLogin();

        FlowsAttachment att = runtimeService.getVariable(processInstanceId, attachmentName, FlowsAttachment.class);
        MultipartFile file = request.getFile(attachmentName + "_data");

        attachmentService.setAttachmentProperties(file, null, "Fuori Task", attachmentName, data, false, username, att);

        flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, attachmentName, att);
        if(att.isProtocollo()) {
            String vecchiProtocolli = runtimeService.getVariable(processInstanceId, flowsAttachmentService.NUMERI_PROTOCOLLO, String.class);
            flowsAttachmentService.addProtocollo(vecchiProtocolli, att.getNumeroProtocollo());
        }
    }

    @RequestMapping(value = "{processInstanceId}/data/new", method = RequestMethod.POST)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @PreAuthorize("@permissionEvaluator.canUpdateAttachment(#processInstanceId, @flowsUserDetailsService)")
    @Timed
    public void uploadNewAttachment(@PathVariable("processInstanceId") String processInstanceId,
                                 MultipartHttpServletRequest request) throws IOException {

        Map<String, Object> data = FlowsTaskService.extractParameters(request);
        String username = SecurityUtils.getCurrentUserLogin();

        FlowsAttachment att = new FlowsAttachment();
        MultipartFile file = request.getFile("newfile_data");
        String attachmentName = "allegati"+ attachmentService.getNextIndexByProcessInstanceId(processInstanceId, "allegati");

        attachmentService.setAttachmentProperties(file, null, "Fuori Task", "newfile", data, true, username, att);
        att.setName(attachmentName);

        flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, attachmentName, att);

        if(att.isProtocollo()) {
            String vecchiProtocolli = runtimeService.getVariable(processInstanceId, flowsAttachmentService.NUMERI_PROTOCOLLO, String.class);
            flowsAttachmentService.addProtocollo(vecchiProtocolli, att.getNumeroProtocollo());
        }
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

    @RequestMapping(value = "task/{taskId}/{attachmentName}/data", method = RequestMethod.GET)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @Timed
    public void getAttachmentForTask(
            HttpServletResponse response,
            @PathVariable("taskId") String taskId,
            @PathVariable("attachmentName") String attachmentName) throws IOException {

        String processInstanceId = taskService.createTaskQuery().taskId(taskId).singleResult().getProcessInstanceId();
        getAttachment(response, processInstanceId, attachmentName);
    }

    @RequestMapping(value = "{processInstanceId}/{attachmentName}/pubblicaTrasparenza", method = RequestMethod.POST)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @PreAuthorize("@permissionEvaluator.canPublishAttachment(#processInstanceId)")
    @Timed
    public void setPubblicabileTrasparenza(
            HttpServletResponse response,
            @PathVariable("processInstanceId") String processInstanceId,
            @PathVariable("attachmentName") String attachmentName,
            @RequestParam("pubblica") boolean pubblica ) {

        flowsAttachmentService.setPubblicabileTrasparenza(processInstanceId, attachmentName, pubblica);

    }

    @RequestMapping(value = "{processInstanceId}/{attachmentName}/pubblicaUrp", method = RequestMethod.POST)
    @ResponseBody
    @Secured(AuthoritiesConstants.USER)
    @PreAuthorize("@permissionEvaluator.canPublishAttachment(#processInstanceId)")
    @Timed
    public void setPubblicabileUrp(
            HttpServletResponse response,
            @PathVariable("processInstanceId") String processInstanceId,
            @PathVariable("attachmentName") String attachmentName,
            @RequestParam("pubblica") boolean pubblica ) {

        flowsAttachmentService.setPubblicabileUrp(processInstanceId, attachmentName, pubblica);

    }
}
