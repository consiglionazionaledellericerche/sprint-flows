package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;
import it.cnr.si.security.SecurityUtils;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;


@Service
public class ManageSceltaUtenteAcquisti {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageSceltaUtenteAcquisti.class);
	public static final String STATO_FINALE_DOMANDA = "statoFinaleDomanda";

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsAttachmentService attachmentService;
	private Expression nomeFileDaPubblicare;

	public void azioneScelta(DelegateExecution execution, String faseEsecuzioneValue, String sceltaUtente) throws IOException, ParseException {
		String processInstanceId =  execution.getProcessInstanceId();
		LOGGER.info("-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
		Map<String, FlowsAttachment> attachmentList;




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
			case "protocollo-revoca-end": {
				if(sceltaUtente.equals("Protocolla")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					protocolloDocumentoService.protocolla(execution, "provvedimentoRevoca");
				}
			};break;


			case "DECISIONE-CONTRATTARE-end": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				if(sceltaUtente.equals("RevocaSemplice")) {
					for (String key : attachmentList.keySet()) {
						FlowsAttachment value = attachmentList.get(key);
						LOGGER.info("Key = " + key + ", Value = " + value);
						attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
					}
				} else {					
					attachmentService.setPubblicabile(execution.getId(), "decisioneContrattare", true);
					pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
				}
			};break;

			case "PROVVEDIMENTO-AGGIUDICAZIONE-start": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentService.setPubblicabile(execution.getId(), "giustificazioniAnomalie", true);
				attachmentService.setPubblicabile(execution.getId(), "provvedimentoNominaCommissione", true);
				attachmentService.setPubblicabile(execution.getId(), "provvedimentoAmmessiEsclusi", true);
				attachmentService.setPubblicabile(execution.getId(), "esitoValutazioneAnomalie", true);
				attachmentService.setPubblicabile(execution.getId(), "elencoDitteCandidate", true);
				attachmentService.setPubblicabile(execution.getId(), "elencoVerbali", true);
				pubblicaFileMultipli(execution, "bandoAvvisi", true);
				pubblicaFileMultipli(execution, "letteraInvito", true);
				pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
			};break;	

			case "PROVVEDIMENTO-AGGIUDICAZIONE-end": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				if(sceltaUtente.equals("RevocaConProvvedimento")) {
					for (String key : attachmentList.keySet()) {
						FlowsAttachment value = attachmentList.get(key);
						LOGGER.info("Key = " + key + ", Value = " + value);
						attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
					}
				} else {					
						attachmentService.setPubblicabile(execution.getId(), "provvedimentoAggiudicazione", true);
						pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
				}
			};break;	


			case "CONTRATTO-FUORI-MEPA-start": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
				pubblicaFileMultipli(execution, "bandoAvvisi", true);
				pubblicaFileMultipli(execution, "letteraInvito", true);
				pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
			};break;	


			case "CONTRATTO-FUORI-MEPA-end": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				if(sceltaUtente.equals("RevocaConProvvedimento")) {
					for (String key : attachmentList.keySet()) {
						FlowsAttachment value = attachmentList.get(key);
						LOGGER.info("Key = " + key + ", Value = " + value);
						attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
					}
				} else {					
					attachmentService.setPubblicabile(execution.getId(), "contratto", true);
					pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
				}
			};break;



			case "STIPULA-MEPA-start": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
				pubblicaFileMultipli(execution, "bandoAvvisi", true);
				pubblicaFileMultipli(execution, "letteraInvito", true);
				pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
			};break;	

			case "STIPULA-MEPA-end": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				if(sceltaUtente.equals("RevocaConProvvedimento")) {
					for (String key : attachmentList.keySet()) {
						FlowsAttachment value = attachmentList.get(key);
						LOGGER.info("Key = " + key + ", Value = " + value);
						attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
					}
				} else {					
					pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
				}
			};break;		


			case "REVOCA-end": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}

			};break;	

			case "consuntivo-end": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				attachmentService.setPubblicabile(execution.getId(), "avvisoPostInformazione", true);
				attachmentService.setPubblicabile(execution.getId(), "modificheVariantiArt106", true);
				attachmentService.setPubblicabile(execution.getId(), "stipula", true);
				protocolloDocumentoService.protocollaDocumento(execution, "stipula", execution.getVariable("numeroProtocollo_stipula").toString(), execution.getVariable("dataProtocollo_stipula").toString());
				pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
			};break;			


			case "firma-revoca-end": {
				if(sceltaUtente.equals("Firma")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					firmaDocumentoService.eseguiFirma(execution, "provvedimentoRevoca");
				}
			};break;  
			case "end-stipulato": {
				execution.setVariable(STATO_FINALE_DOMANDA, "STIPULATO");
				LOGGER.info("-- FLUSSO TERMINATO in stato STIPULATO");
			};break;  
			case "end-revocato": {
				execution.setVariable(STATO_FINALE_DOMANDA, "REVOCATO");
				LOGGER.info("-- FLUSSO TERMINATO in stato REVOCATO");
			};break;
			default:  {
				LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			};break;    
			}
		}
	}
	
	public void pubblicaFileMultipli(DelegateExecution execution, String nomeDocumento, Boolean pubblicaFlag) {
		for (int i = 0; i < 1000; i++) {
			if(execution.getVariable(nomeDocumento +"[" + i + "]") != null) {
				FlowsAttachment documentoCorrente = (FlowsAttachment) execution.getVariable(nomeDocumento +"[" + i + "]");
				LOGGER.info("-- documentoCorrente: " + documentoCorrente );
				attachmentService.setPubblicabile(execution.getId(), documentoCorrente.getName(), pubblicaFlag);
			} else {
				break;
			}
		}
	}
}
