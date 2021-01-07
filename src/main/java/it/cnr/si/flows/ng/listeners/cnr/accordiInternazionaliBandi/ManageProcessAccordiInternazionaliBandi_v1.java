package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliBandi;


import com.google.common.net.MediaType;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.task.Task;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Enum.Azione.GenerazioneDaSistema;

@Component
@Profile("cnr")
public class ManageProcessAccordiInternazionaliBandi_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAccordiInternazionaliBandi_v1.class);
	public static final String STATO_FINALE_VERBALE = "statoFinaleDomanda";

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartAccordiInternazionaliBandiSetGroupsAndVisibility startAccordiInternazionaliSetGroupsAndVisibility;
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
	private Utils utils;


	private Expression faseEsecuzione;


	@Override
	public void notify(DelegateExecution execution) throws Exception {
		//(OivPdfService oivPdfService = new OivPdfService();

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
			startAccordiInternazionaliSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    
		// START
		case "caricamento-verbale-start": {
			creaExportCsvDomandePerBando(execution, (execution.getVariable("idBando").toString()));
			utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
		};break;  

		case "firma-verbale-end": {

			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "verbale", null);
			}
		};break; 
		case "protocollo-verbale-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "verbale");
			}
		};break;  	
		case "endevent-bando-start": {
			execution.setVariable(STATO_FINALE_VERBALE, "APPROVATO");
			execution.setVariable("statoFinale", "APPROVATO");
			utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
		};break;    	

		case "process-end": {
			sbloccaDomandeBando(execution);
		};break; 
		// DEFAULT  
		default:  {
		};break;    

		} 
	}


	private void sbloccaDomandeBando(DelegateExecution execution) {

		String executionId = execution.getId();
		String idBando = runtimeService.getVariable(executionId, "idBando", String.class);

		try {
			List<Task> taskIstances = taskService
					.createTaskQuery()
					//.taskDefinitionKey("valutazione-domande-bando")
					.processDefinitionKey("accordi-internazionali-domande")
					.taskDefinitionKey("valutazione-domande-bando")
					.processVariableValueEquals("idBando", idBando)
					.active()
					.list();

			LOGGER.info("nr istanze trovate:" + taskIstances.size());

			taskIstances.forEach(taskIstance -> {
				String taskId = taskIstance.getId(); 
				LOGGER.debug(" key: " + runtimeService.getVariable(taskIstance.getProcessInstanceId(), "key", String.class) + " titolo: "+ runtimeService.getVariable(taskIstance.getProcessInstanceId(), "titolo", String.class) + " ProcessInstanceId: "+ taskIstance.getProcessInstanceId() + " taskid: "+ taskId + " getTaskDefinitionKey: " + taskIstance.getTaskDefinitionKey()  + " [" + taskIstance.getName() + "]");
				LOGGER.debug("Sblocco la Domanda per il task: "+ taskId + " della Domanda: " + taskService.getVariable(taskId, "idDomanda") + " del Bando nr: " + taskService.getVariable(taskId, "idBando"));

				Map<String, Object> variabili = new HashMap<>();
				variabili.put("sceltaUtente", "graduatoria da verbale");
				variabili.put("linkToOtherWorkflows", execution.getProcessInstanceId());	
				if (execution.getVariable("linkToOtherWorkflows") != null) {
					execution.setVariable("linkToOtherWorkflows", execution.getVariable("linkToOtherWorkflows").toString() + "," + taskIstance.getProcessInstanceId());
				} else {
					execution.setVariable("linkToOtherWorkflows", taskIstance.getProcessInstanceId());
				}
				taskService.complete(taskId, variabili);
			});
			//throw new RuntimeException("Errore per provare la transazione atomica dello sblocco delle domande");

			// LOGGER.info("Domande sbloccate correttamente");

		} catch ( RuntimeException  e) {
			LOGGER.error("Errore nel completamento dei task relativi al flusso " + executionId, e);
			throw e;
		}

	}


	private void creaExportCsvDomandePerBando(DelegateExecution execution, String idBando) throws IOException {
		String processInstanceId = execution.getProcessInstanceId();
		Map<String, String> req = new HashMap<>();
		if (idBando != null) {
			req.put("idBando", "text="+idBando);
		}
		//req.put(processDefinitionKey, processDefinitionKey);
		String order = "ASC";
		Integer firstResult = -1;
		Integer maxResults = -1;
		String processDefinitionKey = "accordi-internazionali-domande";
		Boolean activeFlag = true;

		DataResponse flussiAttivaPerBando = flowsProcessInstanceService.search(req, processDefinitionKey, activeFlag, order, firstResult, maxResults, true);
		File tempFile = File.createTempFile("prefix-", "-suffix");
		//File tempFile = File.createTempFile("MyAppName-", ".tmp");
		PrintWriter writer = new PrintWriter(tempFile);
		//creo il csv corrispondente
		String fileName = "ExportCsvDomandeBando" + idBando + ".xls";
		//String downloadName = "ExportCsvDomandeBando" + idBando;
		String labelFile = "Export Csv Domande Bando";
		flowsTaskService.buildCsv((List<HistoricProcessInstanceResponse>) flussiAttivaPerBando.getData(), writer, processDefinitionKey);
		byte[] contents = FileUtils.readFileToByteArray(tempFile);
		FlowsAttachment documentoGenerato = new FlowsAttachment();
		documentoGenerato.setFilename(fileName);
		documentoGenerato.setName(fileName);
		documentoGenerato.setLabel(labelFile);
		documentoGenerato.setAzione(GenerazioneDaSistema);
		documentoGenerato.setMimetype(MediaType.MICROSOFT_EXCEL.toString());
		documentoGenerato.setPath(runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class));
		flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, fileName, documentoGenerato, contents);
	}
}
