package it.cnr.si.flows.ng.service;

import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.ReportException;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.service.RestPdfSiglaService;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.engine.impl.variable.LongStringType;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricIdentityLinkResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.pdfbox.pdmodel.PDPage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import rst.pdfbox.layout.elements.ControlElement;
import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.ImageElement;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.text.BaseFont;
import rst.pdfbox.layout.text.Position;

import javax.inject.Inject;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Aggiornamento;
import static it.cnr.si.flows.ng.utils.Enum.Azione.Caricamento;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.*;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;

@Service
public class FlowsPdfService {

	public static final String TITLE = "title";
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsPdfService.class);
	private static final float FONT_SIZE = 10;
	private static final float TITLE_SIZE = 18;
	private static final String VALUTAZIONE_ESPERIENZE_JSON = "valutazioneEsperienze_json";
	private static final String IMPEGNI_JSON = "impegni_json";
	private static final String DITTECANDIDATEJSON = "ditteCandidate_json";
	private static final String DITTEINVITATEJSON = "ditteInvitate_json";
	private static final String DITTERTIJSON = "ditteRTI_json";

	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private FlowsProcessDiagramService flowsProcessDiagramService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private ViewRepository viewRepository;
	@Inject
	private Utils utils;
	@Inject
	private Environment env;
	@Inject
	private TaskService taskService;
	@Inject
	private HistoryService historyService;
	@Inject
	private RuntimeService runtimeService;
	//@Inject
	//private FlowsPdfBySiglaRestService flowsPdfBySiglaRestService;
	@Inject
	private RestPdfSiglaService restPdfSiglaService;
	

	// ELENCO PARAMETRI STATISTICHE
	private int nrFlussiTotali = 0;
	private int nrFlussiAttivi = 0;
	private int nrFlussiTerminati = 0;
	private int allTerminatedProcessInstancesDurationInMillis = 0;
	private Calendar newDate = Calendar.getInstance();

	public String makeSummaryPdf(String processInstanceId, ByteArrayOutputStream outputStream) throws IOException, ParseException {

		Document pdf = new Document(40, 60, 40, 60);
		Paragraph paragraphField = new Paragraph();
		Paragraph paragraphDiagram = new Paragraph();
		Paragraph paragraphDocs = new Paragraph();
		Paragraph paragraphHistory = new Paragraph();

		Map<String, Object> map = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId, true);

		HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) map.get("entity");
		String fileName = "Summary_" + processInstance.getBusinessKey() + ".pdf";
		LOGGER.debug("creating pdf {} ", fileName);

		List<RestVariable> variables = processInstance.getVariables();
		ArrayList<Map> tasksSortedList = (ArrayList<Map>) map.get("history");
		Collections.reverse(tasksSortedList);  //ordino i task rispetto alla data di creazione (in senso crescente)

		//      genero il titolo del pdf (la bussineskey (es: "Acquisti Trasparenza-2017-1") + titolo (es: "acquisto pc")
		String titolo = processInstance.getBusinessKey() + "\n";
		Optional<RestVariable> variable = variables.stream()
				.filter(a -> (a.getName().equals(Enum.VariableEnum.titolo.name())))
				.findFirst();
		if (variable.isPresent())
			titolo += variable.get().getValue() + "\n\n";
		else {
			// Titolo nel file pdf in caso di Workflow Definition che non ha il titolo
			// nella variabile "titolo" ma nella vecchia variabile "title" Flussi CNR
			variable = variables.stream()
					.filter(a -> (a.getName()).equals(TITLE))
					.findFirst();

			titolo += variable.get().getValue() + "\n\n";
		}
		paragraphField.addText(titolo, TITLE_SIZE, HELVETICA_BOLD);

		//variabili da visualizzare per forza (se presenti)
		for (RestVariable var : variables) {
			String variableName = var.getName();
			if (variableName.equals(initiator.name())) {
				paragraphField.addText("Avviato da: " + var.getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);
			} else if (variableName.equals(startDate.name())) {
				if (var.getValue() != null)
					paragraphField.addText("Avviato il: " + formatDate(utils.parsaData((String) var.getValue())) + "\n", FONT_SIZE, HELVETICA_BOLD);
			} else if (variableName.equals(endDate.name())) {
				if (var.getValue() != null)
					paragraphField.addText("Terminato il: " + formatDate(utils.parsaData((String) var.getValue())) + "\n", FONT_SIZE, HELVETICA_BOLD);
			} else if (variableName.equals(gruppoRA.name())) {
				paragraphField.addText("Gruppo Responsabile Acquisti: " + var.getValue() + "\n", FONT_SIZE, HELVETICA_BOLD);
			}
		}

		//variabili "visibili" (cioè presenti nella view nel db)
		View viewToDb = viewRepository.getViewByProcessidType(processInstance.getProcessDefinitionId().split(":")[0], "detail");
		Elements metadatums = Jsoup.parse(viewToDb.getView()).getElementsByTag("metadatum");
		for (org.jsoup.nodes.Element metadatum : metadatums) {
			String label = metadatum.attr("label");
			String type = metadatum.attr("type");

			if (type.equals("table")) {
				variable = variables.stream()
						.filter(a -> (a.getName()).equals(getPropertyName(metadatum, "rows")))
						.findFirst();
				if (variable.isPresent() && variable.get().getValue() != null) {
					paragraphField.addText(label + ":\n", FONT_SIZE, HELVETICA_BOLD);
					JSONArray impegni = new JSONArray((String) variable.get().getValue());
					for (int i = 0; i < impegni.length(); i++) {
						JSONObject impegno = impegni.getJSONObject(i);

						addLine(paragraphField, "Impegno numero " + (i + 1), "", true, false);
						JSONArray keys = impegno.names();
						for (int j = 0; j < keys.length(); j++) {
							String key = keys.getString(j);
							Object value = impegno.get(key);
							String stringValue = String.valueOf(value);
							addLine(paragraphField, key, stringValue, true, true);
						}
					}
					//Fine del markup indentato
					paragraphField.addMarkup("-!\n", FONT_SIZE, BaseFont.Helvetica);
				}
			} else {
				variable = variables.stream()
						.filter(a -> (a.getName()).equals(getPropertyName(metadatum, "value")))
						.findFirst();
				if (variable.isPresent()) {
					variables.remove(variable.get());
					String value = String.valueOf(variable.get().getValue());
					if (type.equals("wysiwyg"))
						value = Jsoup.parse(value).text();
					paragraphField.addText(label + ": " + value + "\n", FONT_SIZE, HELVETICA_BOLD);
				}
			}
		}

		//caricamento diagramma workflow
		ImageElement image = makeDiagram(processInstanceId, paragraphDiagram, new PDPage().getMediaBox().createDimension());

		//caricamento documenti allegati al flusso e cronologia
		makeDocs(paragraphDocs, processInstanceId);

		//caricamento history del workflow
		makeHistory(paragraphHistory, tasksSortedList);

		pdf.add(paragraphField);
		pdf.add(ControlElement.NEWPAGE);
		pdf.add(paragraphDiagram);
		pdf.add(image);
		pdf.add(ControlElement.NEWPAGE);
		pdf.add(paragraphDocs);
		pdf.add(ControlElement.NEWPAGE);
		pdf.add(paragraphHistory);

		pdf.save(outputStream);

		return fileName;
	}

	//Sotituisco il mapping direttamente con il json delle variabili attuali
	private JSONObject mappingVariables(JSONObject variables, String processInstanceId) {

		Map<String, Object> map = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId, false);

		HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) map.get("entity");
		variables.put("businessKey", processInstance.getBusinessKey());


		//refactoring della stringona contenete le esperienze in un jsonArray
		if (variables.has(VALUTAZIONE_ESPERIENZE_JSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(VALUTAZIONE_ESPERIENZE_JSON));
			variables.put(VALUTAZIONE_ESPERIENZE_JSON, esperienze);
		}

		if (variables.has(IMPEGNI_JSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(IMPEGNI_JSON));
			variables.put(IMPEGNI_JSON, esperienze);
		}

		if (variables.has(DITTEINVITATEJSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(DITTEINVITATEJSON));
			variables.put(DITTEINVITATEJSON, esperienze);
		}

		if (variables.has(DITTECANDIDATEJSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(DITTECANDIDATEJSON));
			variables.put(DITTECANDIDATEJSON, esperienze);
		}

		if (variables.has(DITTERTIJSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(DITTERTIJSON));
			variables.put(DITTERTIJSON, esperienze);
		}
		return variables;
	}


	//Sotituisco il mapping direttamente con il json delle variabili attuali
	private JSONObject mappingVariableBeforeStartPi(JSONObject variables, String processInstanceId) {

		//refactoring della stringona contenete le esperienze in un jsonArray
		if (variables.has(VALUTAZIONE_ESPERIENZE_JSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(VALUTAZIONE_ESPERIENZE_JSON));
			variables.put(VALUTAZIONE_ESPERIENZE_JSON, esperienze);
		}

		if (variables.has(IMPEGNI_JSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(IMPEGNI_JSON));
			variables.put(IMPEGNI_JSON, esperienze);
		}

		if (variables.has(DITTEINVITATEJSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(DITTEINVITATEJSON));
			variables.put(DITTEINVITATEJSON, esperienze);
		}

		if (variables.has(DITTECANDIDATEJSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(DITTECANDIDATEJSON));
			variables.put(DITTECANDIDATEJSON, esperienze);
		}

		if (variables.has(DITTERTIJSON)) {
			JSONArray esperienze = new JSONArray(variables.getString(DITTERTIJSON));
			variables.put(DITTERTIJSON, esperienze);
		}
		return variables;
	}


	public Pair<String, byte[]>  makePdf(String tipologiaDoc, String processInstanceId) {

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject variableInstanceJson = new JSONObject();

		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
				.includeProcessVariables()
				.processInstanceId(processInstanceId)
				.singleResult();

		// Verifico se il workflow sia terminato
		if((historicProcessInstance != null) && (historicProcessInstance.getEndTime() != null)){
			//carico le processVariables e rimappo in formato json il campo stringa "valutazioneEsperienze_json"
			variableInstanceJson = new JSONObject(historicProcessInstance.getProcessVariables());
		} else {
			Map<String, VariableInstance> tutteVariabiliMap = runtimeService.getVariableInstances(processInstanceId);
			for (Map.Entry<String, VariableInstance> entry : tutteVariabiliMap.entrySet()) {
				String key = entry.getKey();
				VariableInstance value = entry.getValue();
				//le variabili di tipo serializable (file) non vanno inseriti nel json delle variabili che verranno inseriti nel pdf
				//(ho testato valutazioni esperienze_Json fino a 11000 caratteri ed a questo livello appare come longString)
				if((!(((VariableInstanceEntity) value).getType() instanceof SerializableType)) || (((VariableInstanceEntity) value).getType() instanceof LongStringType)){
					if(key.toString().equals("startDate")) {
						Date startDate = (Date)value.getValue();
						SimpleDateFormat sdf = new  SimpleDateFormat("dd/MM/yyyy HH:mm");
						sdf.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
						variableInstanceJson.put(key, sdf.format(startDate));
					} else {
						String valueEscaped = "campo erroneamente compilato";
						if (runtimeService.getVariable(processInstanceId,value.getName()) != null) {
							valueEscaped = Jsoup.parse(StringEscapeUtils.escapeHtml(runtimeService.getVariable(processInstanceId,value.getName()).toString().replaceAll("\t", "  "))).text();
							valueEscaped = valueEscaped.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
							variableInstanceJson.put(key, valueEscaped);
						}
					}
				}
			}
			LOGGER.info("variableInstanceJson: {}", variableInstanceJson);
		}

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject processVariables = mappingVariables(variableInstanceJson, processInstanceId);
		//creo il pdf corrispondente
		String utenteRichiedente = "sistema";
		String fileName = tipologiaDoc + ".pdf";

		if(processVariables.has("nomeRichiedente")) {
			utenteRichiedente = processVariables.getString("nomeRichiedente");
			fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
		}

		if(processVariables.has("userNameRichiedente")) {
			utenteRichiedente = processVariables.getString("userNameRichiedente");
			fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
		}


		return Pair.of(fileName, makePdf(Enum.PdfType.valueOf(tipologiaDoc), processVariables, fileName, utenteRichiedente, processInstanceId));
	}

	public byte[] makePdf(Enum.PdfType pdfType, JSONObject processvariables, String fileName, String utenteRichiedente, String processInstanceId) {
		Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

		String dir = env.getProperty("jasper-report.dir-cnr");
		if(activeProfiles.contains("oiv")) {
			dir = env.getProperty("jasper-report.dir-oiv");
		}
		else if(activeProfiles.contains("cnr")) {
			dir = env.getProperty("jasper-report.dir-cnr");
		}
		byte[] pdfByteArray = null;
		HashMap<String, Object> parameters = new HashMap();
		InputStream jasperFile = null;
		try {
			//carico le variabili della process instance
			LOGGER.debug("Json con i dati da inserire nel pdf: {}", processvariables.toString().replaceAll("\\\\\"","\""));
			JRDataSource datasource = new JsonDataSource(new ByteArrayInputStream(processvariables.toString().getBytes(Charset.forName("UTF-8"))));
			//JRDataSource datasource = new JsonDataSource(new ByteArrayInputStream(processvariables.toString().replaceAll("\\\\\"","\"").getBytes(Charset.forName("UTF-8"))));

			final ResourceBundle resourceBundle = ResourceBundle.getBundle(
					"net.sf.jasperreports.view.viewer", Locale.ITALIAN);

			//carico un'immagine nel pdf "dinamicamente" (sostituisco una variabile nel file jsper con lo stream dell'immagine)
			if(activeProfiles.contains("oiv")) {
				parameters.put("ANN_IMAGE", this.getClass().getResourceAsStream(dir.substring(dir.indexOf("/print")) + "logo_OIV.JPG"));
			}
			else if(activeProfiles.contains("cnr")) {
				parameters.put("ANN_IMAGE", this.getClass().getResourceAsStream(dir.substring(dir.indexOf("/print")) + "logo_CNR.JPG"));
			}
			parameters.put(JRParameter.REPORT_LOCALE, Locale.ITALIAN);
			parameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, resourceBundle);
			parameters.put(JRParameter.REPORT_DATA_SOURCE, datasource);

			SimpleJasperReportsContext ctx = new SimpleJasperReportsContext(DefaultJasperReportsContext.getInstance());
			JasperFillManager fillmgr = JasperFillManager.getInstance(ctx);

			//il nome del file jasper da caricare(dipende dal tipo di pdf da creare)
			jasperFile = this.getClass().getResourceAsStream(dir.substring(dir.indexOf("/print")) + pdfType.name() + ".jasper");
			JasperPrint jasperPrint = fillmgr.fill(jasperFile, parameters);
			LOGGER.info("-- jasperFile: {}", pdfType.name() + ".jasper");

			pdfByteArray = JasperExportManager.exportReportToPdf(jasperPrint);
		} catch (JRException e) {
			throw new ReportException("Errore JASPER nella creazione del pdf: {}", e);
		}

		//"Allego" il file nel flusso
		Map<String, FlowsAttachment> attachments = flowsAttachmentService.getAttachementsForProcessInstance(processInstanceId);

		FlowsAttachment attachment = attachments.get(pdfType.name());
		if (attachment != null) {
			//aggiorno il pdf
			attachment.setFilename(fileName);
			attachment.setName(pdfType.name());
			attachment.setAzione(Aggiornamento);
			attachment.setUsername(utenteRichiedente);
		} else {
			//salvo il pdf nel flusso
			attachment = new FlowsAttachment();
			attachment.setAzione(Caricamento);
			attachment.setPath(runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class));
			attachment.setTaskId(null);
			attachment.setTaskName(null);
			attachment.setTime(new Date());
			attachment.setName(pdfType.name());
			attachment.setFilename(fileName);
			attachment.setMimetype(com.google.common.net.MediaType.PDF.toString());
			attachment.setUsername(utenteRichiedente);
		}

		try {
			String taskId = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult().getId();
			flowsAttachmentService.saveAttachment(taskId, pdfType.name(), attachment, pdfByteArray);
		}catch (NullPointerException e){
			flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, pdfType.name(), attachment, pdfByteArray);
		}
		return pdfByteArray;
	}

	public byte[] makeSiglaPdf(Enum.PdfType pdfType, JSONObject processvariables, String fileName, String labelFile, String report, String utenteRichiedente, String processInstanceId) {

        JSONObject variabliStampa = new JSONObject();

        variabliStampa.put("nomeFile", fileName);
        variabliStampa.put("report", report);

        JSONArray array = new JSONArray();
        JSONObject arrayParams = new JSONObject();

        JSONObject arrayParamsKey = new JSONObject();
        JSONObject nomeParams = new JSONObject();
        arrayParamsKey.put("paramType", "java.lang.String");
        // Inserisco ogni parametro
        
        
        arrayParamsKey.put("valoreParam", processvariables.toString());
        nomeParams.put("nomeParam", "REPORT_DATA_SOURCE");
        arrayParamsKey.put("key", nomeParams);

        array.put(arrayParamsKey);
        variabliStampa.put("params", array);
		HashMap<String, Object> parameters = new HashMap();
		// RICHIESTA DEL PDF
        byte[] pdfByteArray = null;
        pdfByteArray = restPdfSiglaService.getSiglaPdf(variabliStampa.toString());
    
		//"Allego" il file nel flusso
		Map<String, FlowsAttachment> attachments = flowsAttachmentService.getAttachementsForProcessInstance(processInstanceId);

		FlowsAttachment attachment = attachments.get(pdfType.name());
		if (attachment != null) {
			//aggiorno il pdf
			attachment.setFilename(fileName);
			attachment.setName(pdfType.name());
			attachment.setAzione(Aggiornamento);
			attachment.setUsername(utenteRichiedente);
		} else {
			//salvo il pdf nel flusso
			attachment = new FlowsAttachment();
			attachment.setAzione(Caricamento);
			attachment.setPath(runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class));
			attachment.setTaskId(null);
			attachment.setTaskName(null);
			attachment.setTime(new Date());
			attachment.setName(pdfType.name());
			attachment.setFilename(fileName);
			attachment.setMimetype(com.google.common.net.MediaType.PDF.toString());
			attachment.setUsername(utenteRichiedente);
		}

		try {
			String taskId = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult().getId();
			flowsAttachmentService.saveAttachment(taskId, pdfType.name(), attachment, pdfByteArray);
		}catch (NullPointerException e){
			flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, pdfType.name(), attachment, pdfByteArray);
		}
		return pdfByteArray;
	}

	public byte[] makeStatisticPdf( JSONObject processvariables, String fileName) {
		byte[] pdfByteArray = null;
		HashMap<String, Object> parameters = new HashMap();
		InputStream jasperFile = null;
		Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
		String dir = env.getProperty("jasper-report.dir-cnr");
		if(activeProfiles.contains("oiv")) {
			dir = env.getProperty("jasper-report.dir-oiv");
		}
		else if(activeProfiles.contains("cnr")) {
			dir = env.getProperty("jasper-report.dir-cnr");
		}
		try {
			//carico le variabili della process instance
			LOGGER.debug("Json con i dati da inserire nel pdf: {}", processvariables.toString());
			JRDataSource datasource = new JsonDataSource(new ByteArrayInputStream(processvariables.toString().getBytes(Charset.forName("UTF-8"))));

			final ResourceBundle resourceBundle = ResourceBundle.getBundle(
					"net.sf.jasperreports.view.viewer", Locale.ITALIAN);

			//carico un'immagine nel pdf "dinamicamente" (sostituisco una variabile nel file jsper con lo stream dell'immagine)
			parameters.put("ANN_IMAGE", this.getClass().getResourceAsStream(dir.substring(dir.indexOf("/print")) + "logo_OIV.JPG"));
			parameters.put(JRParameter.REPORT_LOCALE, Locale.ITALIAN);
			parameters.put(JRParameter.REPORT_RESOURCE_BUNDLE, resourceBundle);
			parameters.put(JRParameter.REPORT_DATA_SOURCE, datasource);

			SimpleJasperReportsContext ctx = new SimpleJasperReportsContext(DefaultJasperReportsContext.getInstance());
			JasperFillManager fillmgr = JasperFillManager.getInstance(ctx);

			//il nome del file jasper da caricare(dipende dal tipo di pdf da creare)
			jasperFile = this.getClass().getResourceAsStream(dir.substring(dir.indexOf("/print")) + fileName + ".jasper");
			JasperPrint jasperPrint = fillmgr.fill(jasperFile, parameters);

			pdfByteArray = JasperExportManager.exportReportToPdf(jasperPrint);
		} catch (JRException e) {
			LOGGER.error("Errore JASPER nella creazione del pdf: {}", e);
			throw new ReportException("Errore JASPER nella creazione del pdf: {}", e);
		}
		return pdfByteArray;
	}


	public JSONObject getPdfStatistics (String processDefinitionKey, String idStruttura, String startDateGreat, String startDateLess) {
		Map<String, String> req = new HashMap<>();
		req.put("startDateGreat", startDateGreat);
		req.put("startDateLess", startDateLess);
		if (idStruttura != null && !idStruttura.equals("0000")) {
			req.put("idStruttura", "text="+idStruttura);
		}
		//req.put(processDefinitionKey, processDefinitionKey);
		String order = "ASC";
		Integer firstResult = -1;
		Integer maxResults = -1;

		resetStatisticvariables();

		DataResponse flussiAttivi = flowsProcessInstanceService.search(req, processDefinitionKey, true, order, firstResult, maxResults, false);
		DataResponse flussiTerminati = flowsProcessInstanceService.search(req, processDefinitionKey, false, order, firstResult, maxResults, false);
		Map<String, Integer> mapStatiFlussiAttivi = new HashMap();
		Map<String, Integer> mapStatiFlussiTerminati = new HashMap();

		//VALORIZZAZIONE PARAMETRI STATISTICHE
		nrFlussiAttivi = (int) flussiAttivi.getTotal();
		nrFlussiTerminati  = (int) flussiTerminati.getTotal();
		nrFlussiTotali = nrFlussiAttivi + nrFlussiTerminati ;

		LOGGER.debug("nr. nrFlussiAttivi: {} - nr. nrFlussiTerminati: {} - nr. nrFlussiTotali: {}", nrFlussiAttivi, nrFlussiTerminati, nrFlussiTotali);

		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI ATTIVI
		List<HistoricProcessInstanceResponse> activeProcessInstances = (List<HistoricProcessInstanceResponse>) flussiAttivi.getData();
		for (HistoricProcessInstanceResponse pi : activeProcessInstances) {
			LOGGER.debug(" getId = {}", pi.getId());
			LOGGER.debug(" getDurationInMillis = {}", pi.getDurationInMillis());
			LOGGER.debug(" elementi = {}", pi.getName());
			String processInstanceId = pi.getId();

			// Calcolo gli stati nei flussi attivi)
			String currentTaskName = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult().getName();
			LOGGER.debug("--##  currentTaskName : {} ", currentTaskName);
			//calcolo nr istanze per Stato
			if(mapStatiFlussiAttivi.containsKey(currentTaskName)) {
				mapStatiFlussiAttivi.put(currentTaskName, mapStatiFlussiAttivi.get(currentTaskName) + 1);
			} else {
				mapStatiFlussiAttivi.put(currentTaskName, 1);
			}
		}

		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI TERMINATI
		List<HistoricProcessInstanceResponse> terminatedProcessInstances = (List<HistoricProcessInstanceResponse>) flussiTerminati.getData();
		for (HistoricProcessInstanceResponse pi : terminatedProcessInstances) {
			LOGGER.debug(" getId = {}", pi.getId());
			LOGGER.debug(" getDurationInMillis = {}", pi.getDurationInMillis());
			LOGGER.debug(" elementi = {}", pi.getName());

			allTerminatedProcessInstancesDurationInMillis = (int) (allTerminatedProcessInstancesDurationInMillis + pi.getDurationInMillis());
			JSONObject json = new JSONObject(pi.getName());
			//Rimuovo il VECCHIO stato

			String taskEndName = json.getString(Enum.VariableEnum.stato.name());
			LOGGER.info("-- taskEndName: {}", taskEndName);
			//calcolo nr istanze per Stato
			if(mapStatiFlussiTerminati.containsKey(taskEndName)) {
				mapStatiFlussiTerminati.put(taskEndName, mapStatiFlussiTerminati.get(taskEndName) + 1);
			} else {
				mapStatiFlussiTerminati.put(taskEndName, 1);
			}
		}

		JSONObject variableStatisticsJson = new JSONObject();

		//LISTA VARIABILI COMUNI
		variableStatisticsJson.put("dataIn", startDateGreat);
		variableStatisticsJson.put("dataOut", startDateLess);
		variableStatisticsJson.put("processDefinitionKey", processDefinitionKey);
		variableStatisticsJson.put("nrFlussiAttivi", nrFlussiAttivi);
		variableStatisticsJson.put("nrFlussiTerminati", nrFlussiTerminati);
		variableStatisticsJson.put("nrFlussiTotali", nrFlussiTotali);

		//LISTA VARIABILI FLUSSI ATTIVI
		Map<String, Object> listaStatiFlussiAttivi = new HashMap<String, Object>();
		JSONArray arrayStatiFlussiAttivi = new JSONArray();
		for (Map.Entry<String, Integer> pair : mapStatiFlussiAttivi.entrySet()) {
			listaStatiFlussiAttivi.put("Stato", pair.getKey());
			listaStatiFlussiAttivi.put("NrIstanze", pair.getValue());
			arrayStatiFlussiAttivi.put(listaStatiFlussiAttivi);
		}
		variableStatisticsJson.put("StatiFlussiAttivi", arrayStatiFlussiAttivi);

		//LISTA VARIABILI FLUSSI TERMINATI
		Map<String, String> listaStatiFlussiTerminati = new HashMap<String, String>();
		JSONArray arrayStatiFlussiTerminati = new JSONArray();
		int mediaGiorniFlusso = 0;
		if (allTerminatedProcessInstancesDurationInMillis > 0 && nrFlussiTerminati > 0) {
			mediaGiorniFlusso = allTerminatedProcessInstancesDurationInMillis/ (1000 * 60 * 60 * 24 * nrFlussiTerminati);
		}
		variableStatisticsJson.put("mediaGiorniFlusso", mediaGiorniFlusso);

		for (Map.Entry<String, Integer> pair : mapStatiFlussiTerminati.entrySet()) {
			listaStatiFlussiTerminati.put("Stato", pair.getKey());
			listaStatiFlussiTerminati.put("NrIstanze", pair.getValue().toString());
			arrayStatiFlussiTerminati.put(listaStatiFlussiTerminati);
		}
		variableStatisticsJson.put("StatiFlussiTerminati", arrayStatiFlussiTerminati);

		return variableStatisticsJson;
	}


	public Pair<String, byte[]> makePdfBeforeStartPi(String tipologiaDoc, String processInstanceId) {

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject variableInstanceJson = new JSONObject();

		Map<String, VariableInstance> tutteVariabiliMap = runtimeService.getVariableInstances(processInstanceId);
		for (Map.Entry<String, VariableInstance> entry : tutteVariabiliMap.entrySet()) {
			String key = entry.getKey();
			VariableInstance value = entry.getValue();
			//le variabili di tipo serializable (file) non vanno inseriti nel json delle variabili che verranno inseriti nel pdf
			//(ho testato valutazioni esperienze_Json fino a 11000 caratteri ed a questo livello appare come longString)
			if((!(((VariableInstanceEntity) value).getType() instanceof SerializableType)) || (((VariableInstanceEntity) value).getType() instanceof LongStringType)){
				if(key.toString().equals("startDate")) {
					Date startDate = (Date)value.getValue();
					SimpleDateFormat sdf = new  SimpleDateFormat("dd/MM/yyyy HH:mm");
					sdf.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
					variableInstanceJson.put(key, sdf.format(startDate));
				} else {
					String valueEscaped = "campo erroneamente compilato";
					if (runtimeService.getVariable(processInstanceId,value.getName()) != null) {
						//valueEscaped = Jsoup.parse(StringEscapeUtils.escapeHtml(runtimeService.getVariable(processInstanceId,value.getName()).toString().replaceAll("\t", "  "))).text();
						//valueEscaped = valueEscaped.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
						variableInstanceJson.put(key, valueEscaped);
					}
				}
			}
		}
		LOGGER.info("variableInstanceJson: {}", variableInstanceJson);

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject processVariables = mappingVariableBeforeStartPi(variableInstanceJson, processInstanceId);
		//creo il pdf corrispondente
		String utenteRichiedente = "sistema";
		String fileName = tipologiaDoc + ".pdf";

		if(processVariables.has("nomeRichiedente")) {
			utenteRichiedente = processVariables.getString("nomeRichiedente");
			fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
		}

		if(processVariables.has("userNameRichiedente")) {
			utenteRichiedente = processVariables.getString("userNameRichiedente");
			fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
		}
		return Pair.of(fileName, makePdf(Enum.PdfType.valueOf(tipologiaDoc), processVariables, fileName, utenteRichiedente, processInstanceId));
	}

	public Pair<String, byte[]> makePdfBySigla(String tipologiaDoc, String processInstanceId, List<String> listaVariabiliHtml, String labelFile, String report) {
			//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject variableInstanceJson = new JSONObject();
		Map<String, VariableInstance> tutteVariabiliMap = runtimeService.getVariableInstances(processInstanceId);

		try {
    		for (Map.Entry<String, VariableInstance> entry : tutteVariabiliMap.entrySet()) {
    			String key = entry.getKey();
    			VariableInstance value = entry.getValue();
    			//le variabili di tipo serializable (file) non vanno inseriti nel json delle variabili che verranno inseriti nel pdf
    			//(ho testato valutazioni esperienze_Json fino a 11000 caratteri ed a questo livello appare come longString)
    			if((!(((VariableInstanceEntity) value).getType() instanceof SerializableType)) || (((VariableInstanceEntity) value).getType() instanceof LongStringType)){
    				if(key.toString().equals("startDate")) {
    					Date startDate = (Date)value.getValue();
    					SimpleDateFormat sdf = new  SimpleDateFormat("dd/MM/yyyy HH:mm");
    					sdf.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
    					variableInstanceJson.put(key, sdf.format(startDate));
    				} else {
    					String valueEscaped = "campo erroneamente compilato";
    					if (runtimeService.getVariable(processInstanceId,value.getName()) != null) {
    						String variabileCorrente = value.getName().toString();
    						if (listaVariabiliHtml.contains(variabileCorrente)) {
    							variableInstanceJson.put(variabileCorrente, Utils.sanitizeHtml(runtimeService.getVariable(processInstanceId, variabileCorrente)));
    
    						} else {
    							valueEscaped = Jsoup.parse(StringEscapeUtils.escapeHtml(runtimeService.getVariable(processInstanceId,value.getName()).toString().replaceAll("\t", "  "))).text();
    							valueEscaped = valueEscaped.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
    							variableInstanceJson.put(key, valueEscaped);
    						}
    					}
    				}
    			}
    		}
    		LOGGER.info("variableInstanceJson: {}", variableInstanceJson);
    
    		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
    		JSONObject processVariables = mappingVariableBeforeStartPi(variableInstanceJson, processInstanceId);
    		//creo il pdf corrispondente
    		String utenteRichiedente = "sistema";
    		String fileName = tipologiaDoc + ".pdf";
    
    		if(processVariables.has("nomeRichiedente")) {
    			utenteRichiedente = processVariables.getString("nomeRichiedente");
    			fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
    		}
    
    		if(processVariables.has("userNameRichiedente")) {
    			utenteRichiedente = processVariables.getString("userNameRichiedente");
    			fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
    		}
    		
    		
    		//SOTITUZIONE FIRM ACON JASPER CON FIRMA SIGLA
    		//return Pair.of(fileName, makePdf(Enum.PdfType.valueOf(tipologiaDoc), processVariables, fileName, utenteRichiedente, processInstanceId));
    		return Pair.of(fileName, makeSiglaPdf(Enum.PdfType.valueOf(tipologiaDoc), processVariables, fileName, labelFile, report, utenteRichiedente, processInstanceId));
		} catch (Exception e) {
            LOGGER.error("Il flusso non puo' avere stampe, le variabili sono sbagliate: {} {} {}", processInstanceId, listaVariabiliHtml, tutteVariabiliMap);
            throw e;
		}
		
	}

	//GESTIONE DEI PARAMETRI DA VISUALIZZARE
	private void resetStatisticvariables() {
		nrFlussiTotali = 0;
		nrFlussiAttivi = 0;
		nrFlussiTerminati = 0;
	}

	private String getPropertyName(Element metadatum, String attr) {
		String propertyName = "";
		propertyName = metadatum.attr(attr);
		propertyName = propertyName.substring(propertyName.lastIndexOf('.') + 1).replaceAll("}", "");
		return propertyName;
	}


	private void makeHistory(Paragraph paragraphHistory, ArrayList<Map> tasksSortedList) throws IOException {
		intestazione(paragraphHistory, "Cronologia task del flusso:");
		for (Map task : tasksSortedList) {
			HistoricTaskInstanceResponse historyTask = (HistoricTaskInstanceResponse) task.get("historyTask");
			ArrayList<HistoricIdentityLinkResponse> historyIdentityLinks = (ArrayList<HistoricIdentityLinkResponse>) task.get("historyIdentityLink");

			addLine(paragraphHistory, "Titolo task", historyTask.getName(), true, false);
			addLine(paragraphHistory, "Avviato il ", formatDate(historyTask.getStartTime()), true, true);
			if (historyTask.getEndTime() != null)
				addLine(paragraphHistory, "Terminato il ", formatDate(historyTask.getEndTime()), true, true);

			for (HistoricIdentityLinkResponse il : historyIdentityLinks) {
				addLine(paragraphHistory, il.getType(), (il.getUserId() == null ? il.getGroupId() : il.getUserId()), true, true);
			}
			paragraphHistory.addText("\n", FONT_SIZE, HELVETICA_BOLD);
		}
	}


	private String formatDate(Date date) {
		return date != null ? utils.formattaDataOra(date) : "";
	}


	private ImageElement makeDiagram(String processInstanceId, Paragraph paragraphDiagram, Dimension dimension) throws IOException {
		ImageElement image = null;
		intestazione(paragraphDiagram, "Diagramma del flusso:");
		int margineSx = 50;

		InputStream diagram = flowsProcessDiagramService.getDiagramForProcessInstance(processInstanceId, null);

		image = new ImageElement(diagram);
		Dimension scaledDim = getScaledDimension(new Dimension((int) image.getWidth(), (int) image.getHeight()),
				dimension, margineSx);
		image.setHeight((float) scaledDim.getHeight());
		image.setWidth((float) scaledDim.getWidth());
		image.setAbsolutePosition(new Position(20, 700));
		return image;
	}


	private void makeDocs(Paragraph paragraphDocs, String processInstancesId) throws IOException {

		intestazione(paragraphDocs, "Documenti del flusso:");
		Map<String, FlowsAttachment> docs = flowsAttachmentService.getAttachementsForProcessInstance(processInstancesId);

		for (Map.Entry<String, FlowsAttachment> entry : docs.entrySet()) {
			FlowsAttachment doc = entry.getValue();
			addLine(paragraphDocs, entry.getKey(), doc.getName(), true, false);

			addLine(paragraphDocs, "Nome del file", doc.getFilename(), true, true);
			addLine(paragraphDocs, "Caricato il", formatDate(doc.getTime()), true, true);
			addLine(paragraphDocs, "Dall'utente", doc.getUsername(), true, true);
			addLine(paragraphDocs, "Nel task", doc.getTaskName(), true, true);
			addLine(paragraphDocs, "Mime-Type", doc.getMimetype(), true, true);
			//Tolgo le parentesi quadre (ad es.: [Firmato, Protocollato, Pubblicato]
			String stati = doc.getStati().toString().replace("[", "").replace("]", "");
			if (!stati.isEmpty())
				addLine(paragraphDocs, "Stato Documento", stati, true, true);
			//doppio a capo dopo ogni documento
			paragraphDocs.addText("\n\n", FONT_SIZE, HELVETICA_BOLD);
		}
	}


	private void addLine(Paragraph paragraphField, String fieldName, String fieldValue, boolean elenco, boolean subField) throws IOException {
		String text = "*" + fieldName + ":* " + fieldValue;
		if (elenco) {
			if (subField)
				paragraphField.addMarkup(" -+" + text + "\n", FONT_SIZE, BaseFont.Helvetica);
			else
				paragraphField.addMarkup("-+" + text + "\n", FONT_SIZE, BaseFont.Helvetica);
		} else
			paragraphField.addText(text + "\n", FONT_SIZE, HELVETICA_BOLD);
	}


	private void intestazione(Paragraph contentStream, String titolo) throws IOException {
		contentStream.addText(titolo + "\n\n", TITLE_SIZE, HELVETICA_BOLD);
	}


	private Dimension getScaledDimension(Dimension imgSize, Dimension boundary, int marginRigth) {
		int originalWidth = imgSize.width;
		int originalHeight = imgSize.height;
		int boundWidth = boundary.width;
		int boundHeight = boundary.height;
		int newWidth = originalWidth;
		int newHeight = originalHeight;

		// controllo se abbiamo bisogno di "scalare" la larghezza
		if (originalWidth > boundWidth) {
			//adatto la larghezza alla pagina ed ai margini
			newWidth = boundWidth - marginRigth;
			//adatto l'altezza per mantenere le proporzioni con la nuova larghezza "scalata"
			newHeight = (newWidth * originalHeight) / originalWidth;
		}

		// verifico se c'è bisogno di scalare anche con la nuova altezza
		if (newHeight > boundHeight) {
			//"scalo" l'altezza
			newHeight = boundHeight;
			//adatto la larghezza per adattarla all'altezza "scalata"
			newWidth = ((newHeight * originalWidth) / originalHeight) - marginRigth;
		}
		return new Dimension(newWidth, newHeight);
	}
}