package it.cnr.si.flows.ng.listeners.cnr.covid19;




import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.TaskServiceImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.common.net.MediaType;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsCsvService;
import it.cnr.si.flows.ng.service.FlowsPdfBySiglaRestService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTaskService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.PdfType;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.scritture.UtenteDto;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.listeners.cnr.acquisti.service.AcquistiService;

import static it.cnr.si.flows.ng.utils.Utils.DESCRIZIONE;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;
import static it.cnr.si.flows.ng.utils.Utils.TITOLO;
import static it.cnr.si.flows.ng.utils.Enum.Azione.GenerazioneDaSistema;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

@Component
@Profile("cnr")
public class ManageCovid19_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageCovid19_v1.class);
	public static final String STATO_FINALE_GRADUATORIA = "statoFinaleDomanda";

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartCovid19SetGroupsAndVisibility_v1 startCovid19SetGroupsAndVisibility_v1;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private TaskService taskService;
	@Inject
	private FlowsCsvService flowsCsvService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private FlowsTaskService flowsTaskService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private FlowsPdfBySiglaRestService flowsPdfBySiglaRestService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private AceService aceService;
	
	private Expression faseEsecuzione;


	@Override
	public void notify(DelegateExecution execution) throws Exception {

		Map<String, FlowsAttachment> attachmentList;
		String processInstanceId =  execution.getProcessInstanceId();
		String executionId =  execution.getId();
		String stato =  execution.getCurrentActivityName();
		String sceltaUtente = "start";
		
		if(execution.getVariable("sceltaUtente") != null) {
			sceltaUtente =  (String) execution.getVariable("sceltaUtente");	
		}

		LOGGER.info("ProcessInstanceId: " + processInstanceId);
		String faseEsecuzioneValue = "noValue";
		faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		LOGGER.info("-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);

		switch(faseEsecuzioneValue){  
		// START
		case "process-start": {
			startCovid19SetGroupsAndVisibility_v1.configuraVariabiliStart(execution);
		};break;    
		case "firma-start": {
			// INSERIMENTO VARIABILI FLUSSO
		    execution.setVariable("titolo", "Scheda " + execution.getVariable("tipoAttivita").toString() + " - " + execution.getVariable("initiator").toString());
		    execution.setVariable("descrizione", "Scheda Attività" );
			String tipoAttivita = "rendicontazione";
			if (execution.getVariable("tipoAttivita") != null) {
				tipoAttivita = execution.getVariable("tipoAttivita").toString();
			}
			String nomeFile=tipoAttivita + ".pdf";
			String labelFile = "Monitoraggio Attività Personale";
			String report = "scrivaniadigitale/smart_working.jrxml";
			//tipologiaDoc è la tipologia del file
			String tipologiaDoc = Enum.PdfType.valueOf("monitoraggioAttivitaCovid19").name();
			String utenteFile = execution.getVariable("initiator").toString();
			//valoreParam p il json che racchiude i dati della stampa
			
			JSONObject valoreParamJson = new JSONObject();
			
			valoreParamJson.put("matricola", execution.getVariable("matricola"));
			valoreParamJson.put("nomeCognomeUtente", execution.getVariable("nomeCognomeUtente"));
			valoreParamJson.put("tipoContratto", execution.getVariable("tipoContratto"));
			valoreParamJson.put("cds", execution.getVariable("cds"));
			valoreParamJson.put("direttore", execution.getVariable("direttore"));
			valoreParamJson.put("mese", execution.getVariable("mese").toString());
			valoreParamJson.put("anno", execution.getVariable("anno").toString());
			valoreParamJson.put("attivita_svolta", execution.getVariable("attivita").toString());
			valoreParamJson.put("tipoAttivita", execution.getVariable("tipoAttivita").toString());
			
			//esempio:
			// "{'matricola' : '15221','cds': 'ASR','direttore': 'MAURIZIO LANCIA','mese': 'Marzo','attivita_svolta': 'Ho partecipato a svariate riunioni<br>e ho svilupòpato<BR> ed ho lavorato per molto tempo'}"
				
			String valoreParam = valoreParamJson.toString();

			if (tipoAttivita.equals("rendicontazione")) {
				labelFile="Rendicontazione Attività Personale";
			} else {
				labelFile="Programmazione Attività Personale";
			}
			// UPDATE VARIABILI FLUSSO
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
			// GENERAZIONE PDF
			flowsPdfBySiglaRestService.makePdf(execution, nomeFile, labelFile, report, valoreParam, tipologiaDoc, processInstanceId, utenteFile);
		};break;      
		case "firma-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution,  Enum.PdfType.valueOf("monitoraggioAttivitaCovid19").name());
			}
		};break;     
		case "modifica-end": {

		};break; 
		case "protocollo-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, execution.getVariable("tipoAttivita").toString());
			}
		};break;  	
		case "endevent-covid19-start": {
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "APPROVATO");
		};break;    	

		case "process-end": {
			//

		};break; 
		// DEFAULT  
		default:  {
		};break;    

		} 
	}


}
