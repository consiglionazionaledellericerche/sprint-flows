package it.cnr.si.flows.ng.listeners.cnr.acquisti;



import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;
import it.cnr.si.flows.ng.listeners.cnr.acquisti.service.AcquistiService;

import java.util.Map;

import javax.inject.Inject;

@Component
@Profile("cnr")
public class ManageProcessAcquisti_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAcquisti_v1.class);
	public static final String STATO_FINALE_DOMANDA = "statoFinaleDomanda";

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsAttachmentService attachmentService;
	@Inject
	private StartAcquistiSetGroupsAndVisibility startAcquistiSetGroupsAndVisibility;
	@Inject
	private DittaCandidata dittaCandidata;	
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private AcquistiService acquistiService;

	private Expression faseEsecuzione;

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
			startAcquistiSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    
		// START DECISIONE-CONTRATTARE
		case "verifica-decisione-start": {
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
		};break;     
		case "verifica-decisione-end": {
		};break;    
		case "modifica-decisione-start": {
		};break;     
		case "modifica-decisione-end": {
		};break;    
		case "firma-decisione-start": {
		};break;     
		case "firma-decisione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "decisioneContrattare");
			}
		};break;   
		case "revoca-decisione-start": {
		};break;     
		case "revoca-decisione-end": {
		};break;   
		case "protocollo-decisione-start": {
		};break;     
		case "protocollo-decisione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "decisioneContrattare");
			}
		};break;  
		case "endevent-decisione-contrattare-revoca-start": {
			execution.setVariable("sceltaUtente", "RevocaSemplice");
		};break;     
		case "endevent-decisione-contrattare-revoca-end": {
		};break;  
		case "endevent-decisione-contrattare-protocollo-start": {
		};break;     
		case "endevent-decisione-contrattare-protocollo-end": {
		};break;
		// END DECISIONE-CONTRATTARE 

		case "espletamento-procedura-start": {
		};break;     
		case "espletamento-procedura-end": {
		};break;

		// START PROVVEDIMENTO-AGGIUDICAZIONE  
		case "predisposizione-provvedimento-aggiudicazione-start": {
			if (execution.getVariable("nrElencoDitteInit") != null) {
//				acquistiService.SostituisciDocumento(execution, "provvedimentoAggiudicazione");
				acquistiService.ScorriElencoDitteCandidate(execution);	
			}
			dittaCandidata.evidenzia(execution);
		};break;     
		case "predisposizione-provvedimento-aggiudicazione-end": {
		};break;  
		case "verifica-provvedimento-aggiudicazione-start": {
		};break;     
		case "verifica-provvedimento-aggiudicazione-end": {
		};break;  
		case "modifica-provvedimento-aggiudicazione-start": {
		};break;     
		case "modifica-provvedimento-aggiudicazione-end": {
		};break;  
		case "firma-provvedimento-aggiudicazione-start": {
		};break;     
		case "firma-provvedimento-aggiudicazione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "provvedimentoAggiudicazione");
			}
		};break;  
		case "protocollo-provvedimento-aggiudicazione-start": {
		};break;     
		case "protocollo-provvedimento-aggiudicazione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "provvedimentoAggiudicazione");
			}
		};break;  
		case "revoca-proposta-aggiudicazione-start": {
		};break;     
		case "revoca-proposta-aggiudicazione-end": {
		};break;  
		case "verifica-requisiti-start": {
		};break;     
		case "verifica-requisiti-end": {
		};break;  
		case "endevent-provvedimento-aggiudicazione-revoca-start": {
			execution.setVariable("sceltaUtente", "RevocaConProvvedimento");
		};break;     
		case "endevent-provvedimento-aggiudicazione-revoca-end": {
		};break;  
		case "endevent-provvedimento-aggiudicazione-protocollo-start": {
		};break;     
		case "endevent-provvedimento-aggiudicazione-protocollo-end": {
		};break;
		// END PROVVEDIMENTO-AGGIUDICAZIONE

		// START CONTRATTO FUORI MEPA  
		case "predisposizione-contratto-start": {
			if (execution.getVariable("gestioneRTIDittaAggiudicataria").toString().equals("SI")) {
				dittaCandidata.aggiornaDittaRTICandidata(execution);
			}
		};break;     
		case "predisposizione-contratto-end": {
		};break;  
		case "firma-contratto-start": {
		};break;     
		case "firma-contratto-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "contratto");
			}
		};break;  
		case "modifica-contratto-start": {
		};break;     
		case "modifica-contratto-end": {
		};break;  
		case "protocollo-contratto-start": {
		};break;     
		case "protocollo-contratto-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "contratto");
			}
		};break;  
		case "revoca-contratto-start": {
		};break;     
		case "revoca-contratto-end": {
		};break;  
		case "endevent-contratto-fuori-mepa-revoca-start": {
		};break;     
		case "endevent-contratto-fuori-mepa-revoca-end": {
		};break; 
		case "endevent-contratto-fuori-mepa-protocollo-start": {
		};break;     
		case "endevent-contratto-fuori-mepa-protocollo-end": {
		};break;
		// END CONTRATTO FUORI MEPA

		// START CONSUNTIVO  
		case "consuntivo-start": {
		};break;     
		case "consuntivo-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			attachmentService.setPubblicabile(execution.getId(), "avvisoPostInformazione", true);
			attachmentService.setPubblicabile(execution.getId(), "modificheVariantiArt106", true);
			if ( execution.getVariable("importoTotaleNetto") != null && Double.compare(Double.parseDouble(execution.getVariable("importoTotaleNetto").toString()), 1000000) > 0) {
				attachmentService.setPubblicabile(execution.getId(), "stipula", true);
			}
			if(execution.getVariable("numeroProtocollo_stipula") != null) {
				protocolloDocumentoService.protocollaDocumento(execution, "stipula", execution.getVariable("numeroProtocollo_stipula").toString(), execution.getVariable("dataProtocollo_stipula").toString());
			}
			if(execution.getVariable("numeroProtocollo_contratto") != null) {
				protocolloDocumentoService.protocollaDocumento(execution, "contratto", execution.getVariable("numeroProtocollo_contratto").toString(), execution.getVariable("dataProtocollo_contratto").toString());
			}
			pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
		};break; 
		case "end-stipulato-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "STIPULATO");
		};break;     
		case "end-stipulato-end": {
		};break;
		// END CONSUNTIVO  

		// START STIPULA MEPA  
		case "stipula-mepa-consip-start": {
			if (execution.getVariable("strumentoAcquisizioneId").toString().equals("21")) {
				dittaCandidata.evidenzia(execution);
			} else {
				if (execution.getVariable("gestioneRTIDittaAggiudicataria").toString().equals("SI")) {
					dittaCandidata.aggiornaDittaRTICandidata(execution);
				}
			}			
		};break;      
		case "stipula-mepa-consip-end": {
		};break;   
		case "revoca-stipula-mepa-consip-start": {
		};break;     
		case "revoca-stipula-mepa-consip-end": {
		};break;   
		case "endevent-stipula-mepa-consip-revoca-start": {
		};break;     
		case "endevent-stipula-mepa-consip-revoca-end": {
		};break;     
		case "endevent-stipula-mepa-consip-ok-start": {
		};break;     
		case "endevent-stipula-mepa-consip-ok-end": {
		};break; 
		// END STIPULA MEPA  

		case "conferma-revoca-start": {
		};break;     
		case "conferma-revoca-end": {
		};break; 

		// START REVOCA
		case "firma-revoca-start": {
		};break;     
		case "firma-revoca-end": {
			firmaDocumentoService.eseguiFirma(execution, "provvedimentoRevoca");
		};break;  
		case "modifica-revoca-start": {
		};break;     
		case "modifica-revoca-end": {
		};break;  
		case "protocollo-revoca-start": {
		};break;     
		case "protocollo-revoca-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "provvedimentoRevoca");
			}
		};break;   
		case "endevent-revoca-start": {
		};break;     
		case "endevent-revoca-end": {
		};break;  
		// END REVOCA  

		// FINE ACQUISTI  

		case "end-revocato-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "REVOCATO");

		};break;     
		case "end-revocato-end": {
		};break;  

		// FINE FLUSSO  
		case "process-end": {
		};break;  

		//SUBFLUSSI
		case "DECISIONE-CONTRATTARE-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaSemplice")) {
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
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
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
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			pubblicaFileMultipli(execution, "bandoAvvisi", true);
			pubblicaFileMultipli(execution, "letteraInvito", true);
			pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
		};break;	


		case "CONTRATTO-FUORI-MEPA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}
			} else {	
				if ( execution.getVariable("importoTotaleNetto") != null && Double.parseDouble(execution.getVariable("importoTotaleNetto").toString()) > 1000000) {
					attachmentService.setPubblicabile(execution.getId(), "contratto", true);
				}
				pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
			}
		};break;



		case "STIPULA-MEPA-start": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			pubblicaFileMultipli(execution, "bandoAvvisi", true);
			pubblicaFileMultipli(execution, "letteraInvito", true);
			pubblicaFileMultipli(execution, "allegatiPubblicabili", true);
		};break;	

		case "STIPULA-MEPA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
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
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			for (String key : attachmentList.keySet()) {
				FlowsAttachment value = attachmentList.get(key);
				LOGGER.info("Key = " + key + ", Value = " + value);
				attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
			}

		};break;	
		case "end-stipulato": {
			execution.setVariable(STATO_FINALE_DOMANDA, "STIPULATO");
		};break;  
		case "end-revocato": {
		};break;

		// DEFAULT  
		default:  {
		};break;    

		} 
	}
}
