package it.cnr.si.flows.ng.listeners.cnr.smartWorkingDomanda;


import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeSmartWorkingEnum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.initiator;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")
public class ManageProcessSmartWorkingDomanda_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessSmartWorkingDomanda_v1.class);


	@Value("${cnr.siper.url}")
	private String urlSiper;
	@Value("${cnr.siper.domandePath}")
	private String pathDomandeSmartWorking;

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartSmartWorkingDomandaSetGroupsAndVisibility startSmartWorkingDomandaSetGroupsAndVisibility;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private ExternalMessageService externalMessageService;	
	@Inject
	private TaskService taskService;	
	@Inject
	private FlowsTaskService flowsTaskService;
	@Inject
	private ManagementService managementService;
	@Inject
	private RepositoryService repositoryService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private AceService aceService;
	@Inject
	private Utils utils;

	private Expression faseEsecuzione;

	public void restToApplicazioneSiper(DelegateExecution execution, StatoDomandeSmartWorkingEnum statoDomanda) {

		String idDomanda = execution.getVariable("idDomanda").toString();
		Map<String, Object> stmPayload = new HashMap<String, Object>()
		{
			{
				put("idDomanda", idDomanda);
				put("stato", statoDomanda.name().toString());
			}	
		};

		String url = urlSiper + pathDomandeSmartWorking;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, stmPayload, ExternalApplication.LABCON);
	}



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


		//STATO INIZIALE statoFinaleSwitch
		String statoFinaleSwitch = "noValue";
		if (execution.getVariable("profiloFlusso") != null && execution.getVariable("profiloFlusso").toString().equals("PresaVisione")) {
			statoFinaleSwitch = "PresaVisione";
		} 
		if (execution.getVariable("profiloFlusso") != null && execution.getVariable("profiloFlusso").toString().equals("Validazione")) {
			statoFinaleSwitch = "Annulla";
		} 



		String faseEsecuzioneValue = "noValue";
		// la variabile tipologiaRespinta tiene conto delle varie possibilità di come la domanda è staata respinta
		faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		LOGGER.info("ProcessInstanceId: " + processInstanceId + "-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
		//CHECK PER ANNULLO FLUSSO 
		if (execution.getVariableInstance("motivazioneEliminazione") == null) {
			switch(faseEsecuzioneValue){  
			// START
			case "process-start": {
				startSmartWorkingDomandaSetGroupsAndVisibility.configuraVariabiliStart(execution);
				execution.setVariable("title", "Domanda Smart-Working " + execution.getVariable("nomeCognomeUtente"));

			};break;    	
			// START
			case "validazione-start": {
				execution.setVariable("statoValidazione", "si");
				utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
			};break;  
			case "validazione-end": {
				LOGGER.info("**** validazione-end");
				execution.setVariable("statoValidazione", "no");
			};break;  	 
			case "modifica-start": {
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.APERTA);
			};break;  	 
			case "modifica-end": {
				LOGGER.info("**** modifica-end");
			};break;

			// FINE SUBPROCESS
			case "endsubprocess-annullata-start": {
				execution.setVariable("statoFinaleDomanda", Enum.StatoDomandeSmartWorkingEnum.ANNULLATA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.ANNULLATA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.ANNULLATA);
			};break; 
			case "endsubprocess-validata-start": {
				execution.setVariable("statoFinaleDomanda", Enum.StatoDomandeSmartWorkingEnum.VALIDATA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.VALIDATA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.VALIDATA);
			};break; 
			case "endsubprocess-rifiutata-start": {
				execution.setVariable("statoFinaleDomanda", Enum.StatoDomandeSmartWorkingEnum.RIFIUTATA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.RIFIUTATA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.RIFIUTATA);
			};break; 
			case "endsubprocess-presavisione-start": {
				execution.setVariable("statoFinaleDomanda", Enum.StatoDomandeSmartWorkingEnum.PRESA_VISIONE.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.PRESA_VISIONE.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.PRESA_VISIONE);
			};break; 


			// FINE SUBPROCESS
			case "validazioneResponsabile-start": {
				LOGGER.info("**** inizio SUBPROCESS");
			};break; 		
			case "validazioneResponsabile-end": {
				LOGGER.info("**** fine SUBPROCESS");
			};break; 		


			// FINE FLUSSO
			case "endevent-annullata-start": {
				execution.setVariable("statoFinaleDomanda", Enum.StatoDomandeSmartWorkingEnum.ANNULLATA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.ANNULLATA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.ANNULLATA);
			};break; 		
			case "endevent-comunicata-start": {
				execution.setVariable("statoFinaleDomanda", Enum.StatoDomandeSmartWorkingEnum.COMUNICATA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.COMUNICATA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.COMUNICATA);
			};break; 			

			case "end-process-start": {
				LOGGER.info("**** end-process-start");
			};break; 			
			case "process-end": {
				LOGGER.info("**** fine PROCESS");
			};break; 			


			// DEFAULT  
			default:  {
			};break;    



			}
		}
	}
}