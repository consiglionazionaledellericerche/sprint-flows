package it.cnr.si.flows.ng.resource;


import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.listeners.oiv.service.StatisticOivService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private static final String VALUTAZIONE_ESPERIENZE_JSON = "valutazioneEsperienze_json";

	@Inject
	private FlowsPdfService pdfService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private HistoryService historyService;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private StatisticOivService statisticOivService;
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

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject variableInstanceJson = new JSONObject();

		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
				.includeProcessVariables()
				.processInstanceId(processInstanceId)
				.singleResult();

		// Verifico se il workflow sia terminato
		if(historicProcessInstance.getEndTime() != null){
			//carico le processVariables e rimappo in formato json il campo stringa "valutazioneEsperienze_json"
			variableInstanceJson = new JSONObject(historicProcessInstance.getProcessVariables());
		} else {
			Map<String, VariableInstance> tutteVariabiliMap = runtimeService.getVariableInstances(processInstanceId);
			for (Entry<String, VariableInstance> entry : tutteVariabiliMap.entrySet()) {
				String key = entry.getKey();
				VariableInstance value = entry.getValue();
				Object variableValuealue = value.getValue();
				variableInstanceJson.put(key, variableValuealue);
			}
			LOGGER.info("variableInstanceJson: {}", variableInstanceJson);
		}

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject processVariables = mappingVariables(variableInstanceJson);

		//creo il pdf corrispondente
		String utenteRichiedente = processVariables.getString("nomeRichiedente");
		String fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
		byte[] pdfByteArray = pdfService.makePdf(Enum.PdfType.valueOf(tipologiaDoc), processVariables, fileName, utenteRichiedente, processInstanceId);

		//popolo gli headers della response
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
		headers.setContentType(MediaType.parseMediaType("application/pdf"));
		headers.setContentLength(pdfByteArray.length);

		return new ResponseEntity<>(pdfByteArray, headers, HttpStatus.OK);
	}



	/**
	 * Make statistic pdf response entity.
	 *
	 * @param processDefinitionKey the process definition key
	 * @param startDateGreat       the start date great
	 * @param startDateLess        the start date less
	 * @return the response entity
	 * @throws ParseException the parse exception
	 */
//    todo:  TEST
	@RequestMapping(value = "/makeStatisticPdf", headers = "Accept=application/pdf", method = RequestMethod.GET, produces = "application/pdf")
	@ResponseBody
	@Timed
	@Secured(AuthoritiesConstants.USER)
	public ResponseEntity<byte[]> makeStatisticPdf(
			@RequestParam("processDefinitionKey") String processDefinitionKey,
			@RequestParam("startDateGreat") String startDateGreat,
			@RequestParam("startDateLess") String startDateLess) {

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject processvariables = pdfService.getPdfStatistics(processDefinitionKey, startDateGreat, startDateLess);

		//creo il pdf corrispondente
		byte[] pdfByteArray = pdfService.makeStatisticPdf(processvariables, "statisticheGeneraliFlows");

		//popolo gli headers della response
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Disposition", "attachment; filename=\"statistiche_" + processDefinitionKey + ".pdf\"");
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentLength(pdfByteArray.length);

		return new ResponseEntity<>(pdfByteArray, headers, HttpStatus.OK);
	}



	//Sotituisco il mapping direttamente con il json delle variabili sttuali
	private JSONObject mappingVariables(JSONObject variables) {

		//refactoring della stringona contenete le esperienze in un jsonArray
		if (variables.has(VALUTAZIONE_ESPERIENZE_JSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(VALUTAZIONE_ESPERIENZE_JSON));
			variables.put(VALUTAZIONE_ESPERIENZE_JSON, esperienze);
		}

		return variables;
	}
}