package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.HistoryService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Enum.Azione.GenerazioneDaSistema;

@Controller
@RequestMapping("api/search")
public class FlowsSearchResource {

	public static final String ORDER = "order";
	public static final String ACTIVE = "active";
	public static final String IS_TASK_QUERY = "isTaskQuery";
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private FlowsTaskService flowsTaskService;
	@Inject
	private Utils util;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private HistoryService historyService;
	/**
	 * Funzionalità di Ricerca delle Process Instances.
	 *
	 * @param params the params
	 * @return le response entity frutto della ricerca
	 */
	@PostMapping(value = "/", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Secured(AuthoritiesConstants.USER)
	@Timed
	public ResponseEntity<DataResponse> search(@RequestBody Map<String, String> params) {

		String processDefinitionKey = util.getString(params, "processDefinitionKey", "all");
		String order = util.getString(params, ORDER, "ASC");
		boolean active = util.getBoolean(params, ACTIVE, true);
		boolean isTaskQuery = util.getBoolean(params, IS_TASK_QUERY, false);
		int page = util.getInteger(params, "page", 1);

		Integer maxResults = util.getInteger(params, "maxResult", 50);
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
	@PostMapping(value = "/exportCsv/{processDefinitionKey}", headers = "Accept=application/vnd.ms-excel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/vnd.ms-excel")
	@Secured(AuthoritiesConstants.USER)
	@Timed
	public void exportCsv(
			HttpServletResponse res,
			@PathVariable("processDefinitionKey") String processDefinitionKey,
			@RequestBody Map<String, String> params) throws IOException {

		String order = util.getString(params, ORDER, "ASC");
		boolean active = Boolean.parseBoolean(util.getString(params, ACTIVE, "true"));
		boolean isTaskQuery = util.getBoolean(params, IS_TASK_QUERY, false);
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

	@PostMapping(value = "/exportCsvAndSaveInProcess/{processDefinitionKey}/{processInstanceId}", headers = "Accept=application/vnd.ms-excel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/vnd.ms-excel")
	@Secured(AuthoritiesConstants.USER)
	@Timed
	public void exportCsvAndSaveInProcess(
			HttpServletResponse res,
			@PathVariable("processDefinitionKey") String processDefinitionKey,
			@PathVariable("processInstanceId") String processInstanceId,
			@RequestBody Map<String, String> params) throws IOException {

		String order = util.getString(params, ORDER, "ASC");
		boolean active = Boolean.parseBoolean(util.getString(params, ACTIVE, "true"));
		boolean isTaskQuery = util.getBoolean(params, IS_TASK_QUERY, false);
		Integer firstResult = Integer.parseInt(util.getString(params, "firstResult", "0"));
		Integer maxResults = Integer.parseInt(util.getString(params, "maxResults", "99999"));
//		String pathFascicoloDocumenti = "";
		DataResponse result;
		if (isTaskQuery)
			result = flowsTaskService.search(params, processDefinitionKey, active, order, firstResult, maxResults);
		else
			result = flowsProcessInstanceService.search(params, processDefinitionKey, active, order, firstResult, maxResults, true);

		File tempFile = File.createTempFile("prefix-", "-suffix");
		//File tempFile = File.createTempFile("MyAppName-", ".tmp");
		PrintWriter writer = new PrintWriter(tempFile);

		//creo il csv corrispondente

		flowsTaskService.buildCsv(
				(List<HistoricProcessInstanceResponse>) result.getData(),
				writer, processDefinitionKey);
		byte[] contents = FileUtils.readFileToByteArray(tempFile);
		FlowsAttachment documentoGenerato = new FlowsAttachment();

		documentoGenerato.setAzione(GenerazioneDaSistema);
		documentoGenerato.setMimetype(com.google.common.net.MediaType.MICROSOFT_EXCEL.toString());
		//documentoGenerato.setPath(runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class));

		Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId)
				.includeProcessVariables()
				.singleResult()
				.getProcessVariables();

		if(processVariables.get("pathFascicoloDocumenti") != null) {
			documentoGenerato.setPath(processVariables.get("pathFascicoloDocumenti").toString());
		}
		String fileName = "ExportCsvGraduatoriaBando" + processVariables.get("idBando") + "Dipartimento" + processVariables.get("dipartimentoId") + ".xls";
		String labelFile = "Export Csv Graduatoria Bando" + processVariables.get("idBando") + " Dipartimento" + processVariables.get("dipartimentoId");
		documentoGenerato.setFilename(fileName);
		documentoGenerato.setName(fileName);
		documentoGenerato.setLabel(labelFile);
		
		flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, fileName, documentoGenerato, contents);
	}
}
