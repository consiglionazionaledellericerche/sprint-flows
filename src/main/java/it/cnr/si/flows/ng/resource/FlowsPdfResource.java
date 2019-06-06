package it.cnr.si.flows.ng.resource;


import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.engine.impl.variable.SerializableType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.util.Map;
import java.util.Map.Entry;


/**
 * The type Flows pdf resource.
 */
@Controller
@RequestMapping("api")
public class FlowsPdfResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsPdfResource.class);

	@Inject
	private FlowsPdfService pdfService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private HistoryService historyService;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private Utils utils;



	/**
	 * Crea e restituisce il summary pdf del flusso.
	 *
	 * @param processInstanceId : processInstanceId del workflow di cui si vuole generare il summary
	 * @param req               the req
	 * @return ResponseEntity restituisce il pdf generato
	 */
	@RequestMapping(value = "/summaryPdf", headers = "Accept=application/pdf", method = RequestMethod.GET, produces = "application/pdf")
	@ResponseBody
	@Timed
	@Secured(AuthoritiesConstants.USER)
	public ResponseEntity<byte[]> makeSummaryPdf(
			@RequestParam("processInstanceId") String processInstanceId,
			HttpServletRequest req) {

		try {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			String fileName = pdfService.makeSummaryPdf(processInstanceId, outputStream);

			HttpHeaders headers = new HttpHeaders();
			ResponseEntity<byte[]> resp;
			headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
			headers.setContentType(MediaType.parseMediaType("application/pdf"));
			headers.setContentLength(outputStream.toByteArray().length);
			resp = new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);

			return resp;
		} catch (Exception e) {
			LOGGER.error("Errore nella creazione del Summary.pdf per il flusso {}: ", processInstanceId, e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}



	/**
	 * Crea e restituisce il un pdf del tipo specificato.
	 *
	 * @param processInstanceId processInstanceId del flusso
	 * @param tipologiaDoc      la "tipologia" di pdf da create (ad es.: "rigetto")
	 * @return ResponseEntity restituisce il pdf generato
	 */
	@RequestMapping(value = "/makePdf", headers = "Accept=application/pdf", method = RequestMethod.GET, produces = "application/pdf")
	@ResponseBody
	@Timed
	@Secured(AuthoritiesConstants.USER)
	public ResponseEntity<byte[]> makePdf(
			@RequestParam("processInstanceId") String processInstanceId,
			@RequestParam("tipologiaDoc") String tipologiaDoc) {

        final Pair<String, byte[]> filePair = pdfService.makePdf(tipologiaDoc, processInstanceId);

        //popolo gli headers della response
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Disposition", "attachment; filename=\"" + filePair.getFirst() + "\"");
		headers.setContentType(MediaType.parseMediaType("application/pdf"));
		headers.setContentLength(filePair.getSecond().length);

		return new ResponseEntity<>(filePair.getSecond(), headers, HttpStatus.OK);
	}



	/**
	 * Make statistic pdf response entity.
	 *
	 * @param processDefinitionKey the process definition key
	 * @param startDateGreat       the start date great
	 * @param startDateLess        the start date less
	 * @param idStruttura          idStruttura
	 * @return the response entity
	 */
//    todo:  TEST
	@RequestMapping(value = "/makeStatisticPdf", method = RequestMethod.GET, produces = "application/pdf")
	@ResponseBody
	@Timed
	@Secured(AuthoritiesConstants.USER)
	public ResponseEntity<byte[]> makeStatisticPdf(
			@RequestParam("processDefinitionKey") String processDefinitionKey,
			@RequestParam("idStruttura") String idStruttura,
			@RequestParam("startDateGreat") String startDateGreat,
			@RequestParam("startDateLess") String startDateLess) {

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject processvariables = pdfService.getPdfStatistics(processDefinitionKey, idStruttura, startDateGreat, startDateLess);

		//creo il pdf corrispondente
		byte[] pdfByteArray = pdfService.makeStatisticPdf(processvariables, "statisticheGeneraliFlows");

		//popolo gli headers della response
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Disposition", "attachment; filename=\"statistiche_" + processDefinitionKey + ".pdf\"");
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentLength(pdfByteArray.length);

		return new ResponseEntity<>(pdfByteArray, headers, HttpStatus.OK);
	}

}