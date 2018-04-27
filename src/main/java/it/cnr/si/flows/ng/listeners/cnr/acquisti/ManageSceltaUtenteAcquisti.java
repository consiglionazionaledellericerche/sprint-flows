package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import java.io.IOException;
import java.text.ParseException;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;


@Service
public class ManageSceltaUtenteAcquisti {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageSceltaUtenteAcquisti.class);

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;

	public void azioneScelta(DelegateExecution execution, String faseEsecuzioneValue, String sceltaUtente) throws IOException, ParseException {
		String processInstanceId =  execution.getProcessInstanceId();
		LOGGER.info("-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
		if (sceltaUtente != null){
			switch(faseEsecuzioneValue){  
			case "firma-decisione-end": {
				if(sceltaUtente.equals("Firma")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					firmaDocumentoService.eseguiFirma(execution, "decisioneContrattare");
				}
			};break;  
			case "protocollo-decisione-end": {
				if(sceltaUtente.equals("Protocolla")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					protocolloDocumentoService.protocolla(execution, "decisioneContrattare");
				}
			};break;	  
			case "firma-provvedimento-aggiudicazione-end": {
				if(sceltaUtente.equals("Firma")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					firmaDocumentoService.eseguiFirma(execution, "provvedimentoAggiudicazione");
				}
			};break;  
			case "protocollo-provvedimento-aggiudicazione-end": {
				if(sceltaUtente.equals("Protocolla")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					protocolloDocumentoService.protocolla(execution, "provvedimentoAggiudicazione");
				}
			};break;	  
			case "firma-contratto-end": {
				if(sceltaUtente.equals("Firma")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					firmaDocumentoService.eseguiFirma(execution, "contratto");
				}
			};break;  
			case "protocollo-contratto-end": {
				if(sceltaUtente.equals("Protocolla")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					protocolloDocumentoService.protocolla(execution, "contratto");
				}
			};break;	  
			case "protocollo-invio-stipula-mepa-end": {
				if(sceltaUtente.equals("Protocolla")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					protocolloDocumentoService.protocolla(execution, "stipulaMepa");
				}
			};break;	  
			case "firma-revoca-end": {
				if(sceltaUtente.equals("Firma")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					firmaDocumentoService.eseguiFirma(execution, "provvedimentoRevoca");
				}
			};break;  
			case "protocollo-revoca-end": {
				if(sceltaUtente.equals("Protocolla")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					protocolloDocumentoService.protocolla(execution, "provvedimentoRevoca");
				}
			};break;		
			default:  {
				LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			};break;    
			}
		}
	}
}
