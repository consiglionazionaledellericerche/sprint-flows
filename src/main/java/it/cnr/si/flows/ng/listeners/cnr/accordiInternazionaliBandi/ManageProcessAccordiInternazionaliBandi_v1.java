package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliBandi;



import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.TaskServiceImpl;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.listeners.cnr.acquisti.service.AcquistiService;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

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
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
		};break;  

		case "firma-verbale-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "verbale");
			}
		};break; 
		case "protocollo-verbale-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "verbale");
			}
		};break;  	
		case "endevent-bando-start": {
			execution.setVariable(STATO_FINALE_VERBALE, "VERBALE APPROVATO");
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
				LOGGER.debug(" key: "+ taskIstance.getProcessVariables().get("key") + " titolo: "+ taskIstance.getProcessVariables().get("titolo") + " ProcessInstanceId(): "+ taskIstance.getProcessInstanceId() + " taskid: "+ taskId + " getTaskDefinitionKey: " + taskIstance.getTaskDefinitionKey()  + " [" + taskIstance.getName() + "]");
				LOGGER.debug("Sblocco la Domanda per il task: "+ taskId + " della Domanda: " + taskService.getVariable(taskId, "idDomanda") + " del Bando nr: " + taskService.getVariable(taskId, "idBando"));

				Map<String, Object> variabili = new HashMap<>();
				variabili.put("sceltaUtente", "graduatoria da verbale");
				taskService.complete(taskId, variabili);
			});
			//throw new RuntimeException("Errore per provare la transazione atomica dello sblocco delle domande");

			// LOGGER.info("Domande sbloccate correttamente");

		} catch ( RuntimeException  e) {
			LOGGER.error("Errore nel completamento dei task relativi al flusso " + executionId, e);
			throw e;
		}

	}
}
