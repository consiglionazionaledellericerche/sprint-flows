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
import it.cnr.si.flows.ng.service.FlowsPdfService;
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
	@Inject
	private FlowsPdfService flowsPdfService;

	private Expression faseEsecuzione;

	public void pubblicaFileMultipli(DelegateExecution execution, String nomeDocumento, Boolean pubblicaFlag) {
		for (int i = 0; i < 1000; i++) {
			if(execution.getVariable(nomeDocumento +"[" + i + "]") != null) {
				FlowsAttachment documentoCorrente = (FlowsAttachment) execution.getVariable(nomeDocumento +"[" + i + "]");
				LOGGER.info("-- documentoCorrente: " + documentoCorrente );
				if(!(nomeDocumento.equals("allegatiPubblicazioneTrasparenza") && documentoCorrente.getMetadati().toString().contains("RimozioneDaPubblicazione"))) {
					attachmentService.setPubblicabile(execution.getId(), documentoCorrente.getName(), pubblicaFlag);
				}
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
		case "firma-decisione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "decisioneContrattare");
			}
		};break; 
		case "protocollo-decisione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "decisioneContrattare");
			}
		};break;  
		case "endevent-decisione-contrattare-revoca-start": {
		};break;     
		case "endevent-decisione-contrattare-revoca-end": {
			execution.setVariable("direzioneFlusso", "RevocaSemplice");
		};break;   
		case "endevent-decisione-contrattare-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break;
		// END DECISIONE-CONTRATTARE 

		case "espletamento-procedura-end": {	
			if (execution.getVariable("strumentoAcquisizioneId") != null && execution.getVariable("strumentoAcquisizioneId").equals("23")) {
				acquistiService.OrdinaElencoDitteCandidate(execution);
			}
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
				acquistiService.ProponiDittaAggiudicataria(execution);
		};break; 
		case "firma-provvedimento-aggiudicazione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "provvedimentoAggiudicazione");
			}
		};break;  
		case "protocollo-provvedimento-aggiudicazione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "provvedimentoAggiudicazione");
			}
		};break;
		case "endevent-provvedimento-aggiudicazione-revoca-end": {
			execution.setVariable("direzioneFlusso", "RevocaConProvvedimento");
		};break;    
		case "endevent-provvedimento-aggiudicazione-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break;     
		case "endevent-provvedimento-aggiudicazione-altro-candidato-end": {
			execution.setVariable("direzioneFlusso", "SelezionaAltroCandidato");
		};break;

		// END PROVVEDIMENTO-AGGIUDICAZIONE

		// START CONTRATTO FUORI MEPA  
		case "predisposizione-contratto-start": {
			if ((execution.getVariable("gestioneRTIDittaAggiudicataria") != null) && (execution.getVariable("gestioneRTIDittaAggiudicataria").toString().equals("SI"))) {
				//dittaCandidata.aggiornaDittaRTIInvitata(execution);
			}
		};break;
		case "firma-contratto-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "contratto");
			}
		};break; 
		case "protocollo-contratto-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "contratto");
			}
		};break;  
		case "endevent-contratto-fuori-mepa-revoca-end": {
			execution.setVariable("direzioneFlusso", "RevocaConProvvedimento");
		};break; 
		case "endevent-contratto-fuori-mepa-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break;     
		case "endevent-contratto-fuori-mepa-altro-candidato-end": {
			execution.setVariable("direzioneFlusso", "SelezionaAltroCandidato");
		};break;
		// END CONTRATTO FUORI MEPA

		// START CONSUNTIVO  
		case "consuntivo-start": {
			flowsPdfService.makePdf("avvisoPostInformazione", processInstanceId);
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
			pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);

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
				if ((execution.getVariable("gestioneRTIDittaAggiudicataria") != null) && (execution.getVariable("gestioneRTIDittaAggiudicataria").toString().equals("SI"))) {
					dittaCandidata.aggiornaDittaRTIInvitata(execution);
				}
			}			
		};break; 
		case "endevent-stipula-mepa-consip-revoca-end": {
			execution.setVariable("direzioneFlusso", "RevocaConProvvedimento");
		};break;
		case "endevent-stipula-mepa-consip-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break; 
		// END STIPULA MEPA  

		// START REVOCA

		case "firma-revoca-end": {
			firmaDocumentoService.eseguiFirma(execution, "ProvvedimentoDiRevoca");
		};break; 
		case "protocollo-revoca-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "ProvvedimentoDiRevoca");
			}
		};break;  
		// END REVOCA  

		// FINE ACQUISTI  

		case "end-revocato-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "REVOCATO");
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
				pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);
			}
		};break;

		case "PROVVEDIMENTO-AGGIUDICAZIONE-start": {
			attachmentService.setPubblicabile(execution.getId(), "giustificazioniAnomalie", true);
			attachmentService.setPubblicabile(execution.getId(), "provvedimentoNominaCommissione", true);
			attachmentService.setPubblicabile(execution.getId(), "provvedimentoAmmessiEsclusi", true);
			attachmentService.setPubblicabile(execution.getId(), "esitoValutazioneAnomalie", true);
			attachmentService.setPubblicabile(execution.getId(), "elencoDitteInvitate", true);
			attachmentService.setPubblicabile(execution.getId(), "elencoVerbali", true);
			pubblicaFileMultipli(execution, "bandoAvvisi", true);
			pubblicaFileMultipli(execution, "letteraInvito", true);
			pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);
		};break;	

		case "PROVVEDIMENTO-AGGIUDICAZIONE-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}
			} else {					
				attachmentService.setPubblicabile(execution.getId(), "provvedimentoAggiudicazione", true);
				pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);
			}
		};break;	


		case "CONTRATTO-FUORI-MEPA-start": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			pubblicaFileMultipli(execution, "bandoAvvisi", true);
			pubblicaFileMultipli(execution, "letteraInvito", true);
			pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);
		};break;	


		case "CONTRATTO-FUORI-MEPA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}
			} else {	
				if ( execution.getVariable("importoTotaleNetto") != null && Double.parseDouble(execution.getVariable("importoTotaleNetto").toString()) > 1000000) {
					attachmentService.setPubblicabile(execution.getId(), "contratto", true);
				}
				pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);
			}
		};break;



		case "STIPULA-MEPA-start": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			pubblicaFileMultipli(execution, "bandoAvvisi", true);
			pubblicaFileMultipli(execution, "letteraInvito", true);
			pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);
		};break;	

		case "STIPULA-MEPA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}
			} else {					
				pubblicaFileMultipli(execution, "allegatiPubblicazioneTrasparenza", true);
			}
		};break;		
		case "REVOCA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			for (String key : attachmentList.keySet()) {
				FlowsAttachment value = attachmentList.get(key);
				LOGGER.info("Key = " + key + ", Value = " + value);
				//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
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
