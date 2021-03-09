package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.security.PermissionEvaluatorImpl;
import it.cnr.si.spring.storage.StoreService;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.MimetypeUtils.getMimetype;

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
	private UserDetailsService flowsUserDetailsService;
	@Inject
	private PermissionEvaluatorImpl permissionEvaluator;
	@Inject
	private FlowsAttachmentService attachmentService;
	@Inject
	private StoreService storeService;


	@RequestMapping(value = "{processInstanceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
	@Timed
	public ResponseEntity<Map<String, FlowsAttachment>> getAttachementsForProcessInstance(
			@PathVariable("processInstanceId") String processInstanceId) {

		Map<String, FlowsAttachment> result = flowsAttachmentService.getAttachementsForProcessInstance(processInstanceId);

		return ResponseEntity.ok(result);
	}

	// TODO matrin - eliminare entro il 15/04/2021

	//    @RequestMapping(value = "{processInstanceId}/getPublicDocuments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	//    @ResponseBody
	//    @Secured(AuthoritiesConstants.USER)
	//    @Timed
	//    public ResponseEntity<Map<String, FlowsAttachment>> getPublicDocumentsForProcessInstance(
	//            @PathVariable("processInstanceId") String processInstanceId) {
	//
	//        Map<String, FlowsAttachment> result = null;//flowsAttachmentService.getPublicDocumentsForProcessInstance(processInstanceId);
	//
	//        return ResponseEntity.ok(result);
	//    }

	@RequestMapping(value = "task/{taskId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId, @flowsUserDetailsService)")
	@Timed
	public ResponseEntity<Map<String, FlowsAttachment>> getAttachementsForTask(
			@PathVariable("taskId") String taskId) {

		String processInstanceId = taskService.createTaskQuery().taskId(taskId).singleResult().getProcessInstanceId();
		return getAttachementsForProcessInstance(processInstanceId);
	}

	@RequestMapping(value = "/history/{processInstanceId}/{attachmentName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
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
					// TODO rimuovere questo obbrobbrio
					.map(h -> {
						FlowsAttachment a = (FlowsAttachment) h.getValue();
						a.setUrl("api/attachments/byAttachmentId/"+ processInstanceId +"/"+ h.getId() +"/data");
						return a;
					})
					.sorted( (l, r) -> l.getTime().compareTo(r.getTime()) )
					//                    .map(h -> {h.setBytes(null); return h;})
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

		InputStream is = storeService.getResource(attachment.getUrl());

		response.setContentType(attachment.getMimetype());
		IOUtils.copy(is, response.getOutputStream());
	}

	@RequestMapping(value = "{processInstanceId}/{attachmentName}/data", method = RequestMethod.POST)
	@ResponseBody
	@Secured(AuthoritiesConstants.USER)
	@PreAuthorize("@permissionEvaluator.canUpdateAttachment(#processInstanceId, @flowsUserDetailsService)")
	@Timed
	public void updateAttachment(@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("attachmentName") String attachmentName,
			MultipartHttpServletRequest request) {

		Map<String, Object> data = FlowsTaskResource.extractParameters(request);

		FlowsAttachment att = runtimeService.getVariable(processInstanceId, attachmentName, FlowsAttachment.class);
		String key = runtimeService.getVariable(processInstanceId, "key", String.class);

		att = attachmentService.extractSingleAttachment(att, data, null, "Fuori Task", key, attachmentName, att.getPath());
		flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, attachmentName, att, null);

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

		Map<String, Object> data = FlowsTaskResource.extractParameters(request);

		String processKey = runtimeService.getVariable(processInstanceId, "processKey", String.class);
		String path = runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class);
		String attachmentName = "allegati"+ attachmentService.getNextIndexByProcessInstanceId(processInstanceId, "allegati");

		FlowsAttachment att = attachmentService.extractSingleAttachment(null, data, null, "Fuori Task", processKey, "newfile", path);

		att.setName(attachmentName);
		flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, attachmentName, att, null);

		if(att.isProtocollo()) {
			String vecchiProtocolli = runtimeService.getVariable(processInstanceId, flowsAttachmentService.NUMERI_PROTOCOLLO, String.class);
			flowsAttachmentService.addProtocollo(vecchiProtocolli, att.getNumeroProtocollo());
		}
	}


	@RequestMapping(value = "{processInstanceId}/{attachmentName}/data/sostituzione", method = RequestMethod.POST)
	@ResponseBody
	@PreAuthorize("@permissionEvaluator.canUpdateAttachment(#processInstanceId, @flowsUserDetailsService)")
	@Timed
	public void updateAttachmentSostituzioneProtocollo(@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("attachmentName") String attachmentName,
			MultipartHttpServletRequest request) throws IOException {

		Map<String, Object> data = FlowsTaskResource.extractParameters(request);

		FlowsAttachment att = runtimeService.getVariable(processInstanceId, attachmentName, FlowsAttachment.class);
		MultipartFile file = request.getFile(attachmentName + "_sostituzione_data");
		att.setFilename(file.getOriginalFilename());
		att.setMimetype(getMimetype(file));
		att.setNumeroProtocollo(request.getParameter(attachmentName + "_sostituzione_numeroProtocollo"));
		att.setDataProtocollo(request.getParameter(attachmentName + "_dataProtocollo"));
		att.setMetadato("motivoSostituzione", request.getParameter("motivoSostituzione"));
		att.setAzione(Enum.Azione.SostituzioneProtocollo);

		flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, attachmentName, att, file.getBytes());
		if(att.isProtocollo()) {
			String vecchiProtocolli = runtimeService.getVariable(processInstanceId, flowsAttachmentService.NUMERI_PROTOCOLLO, String.class);
			flowsAttachmentService.addProtocollo(vecchiProtocolli, att.getNumeroProtocollo());
		}
	}

	@RequestMapping(value = "{processInstanceId}/{attachmentName}/data/rettifica", method = RequestMethod.POST)
	@ResponseBody
	@PreAuthorize("@permissionEvaluator.canUpdateAttachment(#processInstanceId, @flowsUserDetailsService)")
	@Timed
	public void updateAttachmentRettificaProtocollo(@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("attachmentName") String attachmentName,
			MultipartHttpServletRequest request) throws IOException {

		Map<String, Object> data = FlowsTaskResource.extractParameters(request);

		FlowsAttachment old = runtimeService.getVariable(processInstanceId, attachmentName, FlowsAttachment.class);
		FlowsAttachment att = new FlowsAttachment();

		String baseName = "rettifica"+ old.getName();
		int index = flowsAttachmentService.getNextIndexByProcessInstanceId(processInstanceId, baseName);
		String name = baseName + index;
		att.setName(name);

		MultipartFile file = request.getFile(attachmentName + "_rettifica_data");
		att.setFilename(file.getOriginalFilename());
		att.setMimetype(getMimetype(file));
		att.setLabel("Rettifica "+ old.getLabel());

		att.setPubblicazioneUrp(old.isPubblicazioneUrp());
		att.setPubblicazioneTrasparenza(old.isPubblicazioneTrasparenza());
		att.setDataProtocollo( String.valueOf(data.get(attachmentName + "_rettifica_dataProtocollo")) );
		att.setNumeroProtocollo( String.valueOf(data.get(attachmentName + "_rettifica_numeroProtocollo")) );
		att.setStati(old.getStati());

		att.setAzione(Enum.Azione.RettificaProtocollo);

		flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, att.getName(), att, file.getBytes());
		if(att.isProtocollo()) {
			String vecchiProtocolli = runtimeService.getVariable(processInstanceId, flowsAttachmentService.NUMERI_PROTOCOLLO, String.class);
			flowsAttachmentService.addProtocollo(vecchiProtocolli, att.getNumeroProtocollo());
		}
	}

	@RequestMapping(value = "byAttachmentId/{processInstanceId}/{variableId}/data", method = RequestMethod.GET)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualize(#processInstanceId, @flowsUserDetailsService)")
	@Timed
	public void getHistoricAttachment(
			HttpServletResponse response,
			@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("variableId") String variableId) throws IOException {

		HistoricDetailVariableInstanceUpdateEntity variable = (HistoricDetailVariableInstanceUpdateEntity)
				historyService.createHistoricDetailQuery()
				.id(variableId)
				.singleResult();
		FlowsAttachment attachment = (FlowsAttachment) variable.getValue();

		if(!variable.getProcessInstanceId().equals(processInstanceId))
			response.sendError(403);
		else {
			ServletOutputStream output = response.getOutputStream();
			response.setContentType(attachment.getMimetype());
			InputStream baos = flowsAttachmentService.getAttachmentContent(attachment.getUrl());
			IOUtils.copy(baos, output);
		}
	}

	@RequestMapping(value = "task/{taskId}/{attachmentName}/data", method = RequestMethod.GET)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId, @flowsUserDetailsService)")
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
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canPublishAttachment(#processInstanceId)")
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
	@PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canPublishAttachment(#processInstanceId)")
	@Timed
	public void setPubblicabileUrp(
			HttpServletResponse response,
			@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("attachmentName") String attachmentName,
			@RequestParam("pubblica") boolean pubblica ) {

		flowsAttachmentService.setPubblicabileUrp(processInstanceId, attachmentName, pubblica);

	}

	@RequestMapping(value = "{processInstanceId}/{attachmentName}/updateAttachmentPath", method = RequestMethod.POST)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@Timed
	public void updateAttachmentPath(
			HttpServletResponse response,
			@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("attachmentName") String attachmentName,
			@RequestBody String valorePath)  {

		flowsAttachmentService.updateAttachmentPath(processInstanceId, attachmentName, valorePath);

	}    

	@RequestMapping(value = "{processInstanceId}/{attachmentName}/updateAttachmentUrl", method = RequestMethod.POST)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@Timed
	public void updateAttachmentUrl(
			HttpServletResponse response,
			@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("attachmentName") String attachmentName,
			@RequestBody String valoreUrl)  {

		flowsAttachmentService.updateAttachmentUrl(processInstanceId, attachmentName, valoreUrl);

	}

	@RequestMapping(value = "{processInstanceId}/{attachmentName}/updateAttachmentMimetypeToPDF", method = RequestMethod.POST)
	@ResponseBody
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@Timed
	public void updateAttachmentMimetype(
			HttpServletResponse response,
			@PathVariable("processInstanceId") String processInstanceId,
			@PathVariable("attachmentName") String attachmentName)
	{

		flowsAttachmentService.updateAttachmentMimetypeToPDF(processInstanceId, attachmentName);

	}
}
