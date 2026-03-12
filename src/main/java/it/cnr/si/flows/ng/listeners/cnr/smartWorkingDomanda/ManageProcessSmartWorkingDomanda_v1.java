package it.cnr.si.flows.ng.listeners.cnr.smartWorkingDomanda;


import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.CNRPdfSignApparence;
import it.cnr.si.flows.ng.utils.Enum;

import it.cnr.si.flows.ng.utils.Enum.StatoDomandeSmartWorkingEnum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.service.SecurityService;

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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.io.IOException;
import java.time.LocalDate;
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
    private SecurityService securityService;
	
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
		String commento = "";
		String matricolaValidatore = "";
		if (execution.getVariable("commento") != null) {
			commento = execution.getVariable("commento").toString();
		}
		if (execution.getVariable("matricolaValidatore") != null) {
			matricolaValidatore = execution.getVariable("matricolaValidatore").toString();
		} 


		Map<String, Object> siperPayload = new HashMap<String, Object>();
		siperPayload.put("idDomanda", idDomanda);
		siperPayload.put("stato", statoDomanda.name().toString());
		siperPayload.put("commento", commento);
		siperPayload.put("matricola", matricolaValidatore);

		String url = urlSiper + pathDomandeSmartWorking;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, siperPayload, ExternalApplication.SIPER);
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
				String currentUser = securityService.getCurrentUserLogin();
				String matricolaValidatore = aceService.getPersonaByUsername(currentUser).getMatricola().toString();
				execution.setVariable("matricolaValidatore", matricolaValidatore);
				execution.setVariable("statoValidazione", "no");
			};break;  	 
			case "modifica-start": {
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.APERTA);
			};break;  	 
			case "modifica-end": {
				startSmartWorkingDomandaSetGroupsAndVisibility.configuraVariabiliStart(execution);
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
				if (sceltaUtente != null && sceltaUtente.equals("Firma")) {
					firmaDocumentoService.eseguiFirma(execution, Enum.PdfType.valueOf("domandaSmartWorking").name(), null);
				}
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
				if (sceltaUtente != null && sceltaUtente.equals("Firma")) {
					firmaDocumentoService.eseguiFirma(execution, Enum.PdfType.valueOf("domandaSmartWorking").name(), null);
				}
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.PRESA_VISIONE.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.PRESA_VISIONE);
			};break; 


			// NOTIFICHE gruppoDirigenteProponente
			//			case "notificatask-start": {
			//				LocalDate dateRif = LocalDate.now();
			//				LOGGER.info("**** notifica AL GRUPPO: " + execution.getVariable("gruppoDirigenteProponente").toString()   + " in data: " + dateRif);
			//			};break; 			

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
				if (execution.getVariable("sceltaUtente") == null || !execution.getVariable("sceltaUtente").toString().equals("Annulla")) {
					execution.setVariable("commento", "scadenza tempi previsti dalla procedura");
				} 
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.ANNULLATA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.ANNULLATA);
			};break; 		
			//			case "endevent-comunicata-start": {
			//				execution.setVariable("statoFinaleDomanda", Enum.StatoDomandeSmartWorkingEnum.COMUNICATA.toString());
			//				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeSmartWorkingEnum.COMUNICATA.toString());
			//				restToApplicazioneSiper(execution, Enum.StatoDomandeSmartWorkingEnum.COMUNICATA);
			//			};break; 			

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