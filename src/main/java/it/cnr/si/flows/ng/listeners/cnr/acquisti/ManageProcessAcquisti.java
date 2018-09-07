package it.cnr.si.flows.ng.listeners.cnr.acquisti;



import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;

import javax.inject.Inject;


@Component
@Profile("cnr")
public class ManageProcessAcquisti implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAcquisti.class);

	@Inject
	private StartAcquistiSetGroupsAndVisibility startAcquistiSetGroupsAndVisibility;
	@Inject
	private ManageSceltaUtenteAcquisti manageSceltaUtenteAcquisti;
	@Inject
	private DittaCandidata dittaCandidata;	
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
		
	private Expression faseEsecuzione;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		//(OivPdfService oivPdfService = new OivPdfService();


		String processInstanceId =  execution.getProcessInstanceId();
		String executionId =  execution.getId();
		String stato =  execution.getCurrentActivityName();
		String sceltaUtente = "start";
		if(execution.getVariable("sceltaUtente") != null) {
			sceltaUtente =  (String) execution.getVariable("sceltaUtente");	
		}

		LOGGER.info("ProcessInstanceId: " + processInstanceId);
		String faseEsecuzioneValue = "noValue";
		boolean aggiornaGiudizioFinale = true;
		boolean nonAggiornaGiudizioFinale = false;
		faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		switch(faseEsecuzioneValue){  
		// START
		case "process-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			startAcquistiSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    
		// START DECISIONE-CONTRATTARE
		case "verifica-decisione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
		};break;     
		case "verifica-decisione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "modifica-decisione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "modifica-decisione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "firma-decisione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "firma-decisione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;   
		case "revoca-decisione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "revoca-decisione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;   
		case "protocollo-decisione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "protocollo-decisione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "endevent-decisione-contrattare-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("sceltaUtente", "RevocaSemplice");
		};break;     
		case "endevent-decisione-contrattare-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "endevent-decisione-contrattare-protocollo-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-decisione-contrattare-protocollo-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;
		// END DECISIONE-CONTRATTARE 

		case "espletamento-procedura-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "espletamento-procedura-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;

		// START PROVVEDIMENTO-AGGIUDICAZIONE  
		case "predisposizione-provvedimento-aggiudicazione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			dittaCandidata.evidenzia(execution);
		};break;     
		case "predisposizione-provvedimento-aggiudicazione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "verifica-provvedimento-aggiudicazione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "verifica-provvedimento-aggiudicazione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "modifica-provvedimento-aggiudicazione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "modifica-provvedimento-aggiudicazione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "firma-provvedimento-aggiudicazione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "firma-provvedimento-aggiudicazione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "protocollo-provvedimento-aggiudicazione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "protocollo-provvedimento-aggiudicazione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "revoca-proposta-aggiudicazione-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "revoca-proposta-aggiudicazione-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "verifica-requisiti-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "verifica-requisiti-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "endevent-provvedimento-aggiudicazione-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("sceltaUtente", "RevocaConProvvedimento");
		};break;     
		case "endevent-provvedimento-aggiudicazione-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "endevent-provvedimento-aggiudicazione-protocollo-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-provvedimento-aggiudicazione-protocollo-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;
		// END PROVVEDIMENTO-AGGIUDICAZIONE

		// START CONTRATTO FUORI MEPA  
		case "predisposizione-contratto-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "predisposizione-contratto-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "firma-contratto-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "firma-contratto-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "modifica-contratto-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "modifica-contratto-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "protocollo-contratto-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "protocollo-contratto-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "revoca-contratto-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "revoca-contratto-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "endevent-contratto-fuori-mepa-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-contratto-fuori-mepa-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break; 
		case "endevent-contratto-fuori-mepa-protocollo-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-contratto-fuori-mepa-protocollo-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;
		// END CONTRATTO FUORI MEPA

		// START CONSUNTIVO  
		case "consuntivo-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "consuntivo-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break; 
		case "end-stipulato-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "end-stipulato-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;
		// END CONSUNTIVO  

		// START STIPULA MEPA  
		case "carica-stipula-mepa-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "carica-stipula-mepa-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "revoca-stipula-mepa-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "revoca-stipula-mepa-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;   
		case "protocollo-invio-stipula-mepa-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "protocollo-invio-stipula-mepa-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "endevent-stipula-mepa-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-stipula-mepa-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-stipula-mepa-protocollo-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-stipula-mepa-protocollo-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break; 
		// END STIPULA MEPA  

		case "conferma-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "conferma-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break; 

		// START REVOCA
		case "firma-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "firma-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "modifica-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "modifica-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		case "protocollo-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "protocollo-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;   
		case "endevent-revoca-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "endevent-revoca-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		// END REVOCA  

		// FINE ACQUISTI  

		case "end-revocato-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "end-revocato-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  

		// FINE FLUSSO  
		case "process-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;  
		
		// DEFAULT  
		default:  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    

		} 
		// Codice per gestire le Scelte
		manageSceltaUtenteAcquisti.azioneScelta(execution, faseEsecuzioneValue, sceltaUtente);
		LOGGER.info("sceltaUtente: " + sceltaUtente);

	}
}
