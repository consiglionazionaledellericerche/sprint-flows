package it.cnr.si.flows.ng.service;

import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.ReportException;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.PdfType;
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
import org.springframework.boot.bind.RelaxedPropertyResolver;
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
public class FlowsPdfBySiglaRestService {

	public static final String TITLE = "title";
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsPdfBySiglaRestService.class);
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
	@Inject
	private RestPdfSiglaService restPdfSiglaService;
	
	




	public Pair<String, byte[]>  makePdf(DelegateExecution execution, String nomeFile, String labelFile, String report, String valoreParam, String tipologiaDoc, String processInstanceId, String utenteFile) {
		JSONObject variabliStampa = new JSONObject();
		
		variabliStampa.put("nomeFile", nomeFile);
		variabliStampa.put("report", report);
		
		JSONArray array = new JSONArray();
		JSONObject arrayParams = new JSONObject();

		JSONObject arrayParamsKey = new JSONObject();
		JSONObject nomeParams = new JSONObject();
		arrayParamsKey.put("paramType", "java.lang.String");
		arrayParamsKey.put("valoreParam", valoreParam);
		nomeParams.put("nomeParam", "REPORT_DATA_SOURCE");
		arrayParamsKey.put("key", nomeParams);

		array.put(arrayParamsKey);
		variabliStampa.put("params", array);
		
		//ESEMPIO
//		{
//			"nomeFile": "Smart Working",
//			"report": "scrivaniadigitale/smart_working.jrxml",
//			"params": [{
//				"key": {
//					"nomeParam": "REPORT_DATA_SOURCE"
//				},
//				"paramType": "java.lang.String",
//				"valoreParam": "{'matricola' : '15221','cds': 'ASR','direttore': 'MAURIZIO LANCIA','mese': 'Marzo','attivita_svolta': 'Ho partecipato a svariate riunioni<br>e ho svilupòpato<BR> ed ho lavorato per molto tempo'}"
//			}]
//		}

		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)

		// RICHIESTA DEL PDF
		byte[] pdfByteArray = null;
		pdfByteArray = restPdfSiglaService.getSiglaPdf(variabliStampa.toString());
		
		
		
		//"Allego" il file nel flusso
		Map<String, FlowsAttachment> attachments = flowsAttachmentService.getCurrentAttachments(execution);
		PdfType pdfType = Enum.PdfType.valueOf(tipologiaDoc);

		FlowsAttachment attachment = attachments.get(pdfType.name());
		if (attachment != null) {
			//aggiorno il pdf
			attachment.setFilename(nomeFile);
			attachment.setLabel(labelFile);;
			attachment.setName(pdfType.name());
			attachment.setAzione(Aggiornamento);
			attachment.setUsername(utenteFile);
		} else {
			//salvo il pdf nel flusso
			attachment = new FlowsAttachment();
			attachment.setAzione(Caricamento);
			attachment.setPath(runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class));;
			attachment.setTaskId(null);
			attachment.setTaskName(null);
			attachment.setTime(new Date());
			attachment.setName(pdfType.name());
			attachment.setLabel(labelFile);;
			attachment.setFilename(nomeFile);
			attachment.setMimetype(com.google.common.net.MediaType.PDF.toString());
			attachment.setUsername(utenteFile);
		}
		
		flowsAttachmentService.saveAttachment(execution, pdfType.name(), attachment, pdfByteArray);

		return Pair.of(nomeFile, pdfByteArray);
	}
	


	private String formatDate(Date date) {
		return date != null ? utils.formattaDataOra(date) : "";
	}


}