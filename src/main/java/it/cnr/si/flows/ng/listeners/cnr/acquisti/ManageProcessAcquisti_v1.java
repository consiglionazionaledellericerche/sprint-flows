package it.cnr.si.flows.ng.listeners.cnr.acquisti;



import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
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
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;

	private Expression faseEsecuzione;

	public void pubblicaFilePubblicabili(DelegateExecution execution, Boolean pubblicaFlag) {
		for (int i = 0; i < 1000; i++) {
			String nomeFile = "allegati" + i;
			if(execution.getVariable(nomeFile) != null) {
				FlowsAttachment documentoCorrente = (FlowsAttachment) execution.getVariable(nomeFile);
				LOGGER.info("-- documentoCorrente: " + documentoCorrente.getLabel() );
				if(documentoCorrente.isPubblicazioneTrasparenza()) {
					attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), pubblicaFlag);
				}
				if(documentoCorrente.isPubblicazioneUrp()) {
					attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), pubblicaFlag);
				}
			} else {
				break;
			}
		}
	}



	public void pubblicaTuttiFilePubblicabili(DelegateExecution execution) {

		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);

			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente);
			if(documentoCorrente.isPubblicazioneUrp()) {
				attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), true);					
			}
			if(documentoCorrente.isPubblicazioneTrasparenza()) {
				attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), true);					
			}
		}
	}
	public void pubblicaFilePubblicabiliURP(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);
			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente);
			if(documentoCorrente.isPubblicazioneUrp()) {
				attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), true);					
			}
		}
	}

	public void pubblicaFilePubblicabiliTrasparenza(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);
			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente);
			if(documentoCorrente.isPubblicazioneTrasparenza()) {
				attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), true);					
			}
		}
	}
	public void pubblicaFileMultipliPubblicabili(DelegateExecution execution, String nomeDocumento, Boolean pubblicaFlag) {
		for (int i = 0; i < 1000; i++) {
			if(execution.getVariable(nomeDocumento +"[" + i + "]") != null) {
				FlowsAttachment documentoCorrente = (FlowsAttachment) execution.getVariable(nomeDocumento +"[" + i + "]");
				LOGGER.info("-- documentoCorrente: " + documentoCorrente );
				if(documentoCorrente.isPubblicazioneTrasparenza()) {
					attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), pubblicaFlag);
				}
				if(documentoCorrente.isPubblicazioneUrp()) {
					attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), pubblicaFlag);
				}
			} else {
				break;
			}
		}
	}
	// FUNZIONE CHE CONTROLLA LA LISTA DEI SOCUMENTI CHE DEVONO ESSERE PUBBLICATI IN TRASPARENZA (SE PRESENTI DEVONO ESSERE PUBBLICATI ALTRIMENTI IL FLUSSO SI BLOCCA)
	public void controllaFilePubblicabiliTrasparenza(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		String errorMessage = "<b>il flusso non può essere terminato perché<br>i seguenti file devono risulare pubblicati in trasparenza:<br>";
		int nrFilesMancanti = 0;
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);
			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente.getFilename());
			if((documentoCorrente.getName().equals("decisioneContrattare")  
					|| documentoCorrente.getName().equals("modificheVariantiArt106")
					|| documentoCorrente.getName().equals("bandoAvvisi")
					|| documentoCorrente.getName().equals("letteraInvito")
					|| documentoCorrente.getName().equals("provvedimentoAmmessiEsclusi")
					|| documentoCorrente.getName().equals("provvedimentoNominaCommissione")
					|| documentoCorrente.getName().equals("provvedimentoAggiudicazione")
					|| documentoCorrente.getName().equals("elencoVerbali")
					|| documentoCorrente.getName().equals("modificheVariantiArt106")
					|| documentoCorrente.getName().equals("stipula")
					|| documentoCorrente.getName().equals("avvisoPostInformazione"))
					& (!documentoCorrente.getStati().toString().contains("PubblicatoTrasparenza"))) {
				nrFilesMancanti = nrFilesMancanti +1;
				errorMessage = errorMessage + " - " + documentoCorrente.getName();					
			}
		}
		if (nrFilesMancanti>0) {
			throw new BpmnError("500", errorMessage+"</b><br>");
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
			pubblicaTuttiFilePubblicabili(execution);
		};break;
		// START PROVVEDIMENTO-AGGIUDICAZIONE  
		case "predisposizione-provvedimento-aggiudicazione-start": {
			if (execution.getVariable("nrElencoDitteInit") != null) {
				//				acquistiService.SostituisciDocumento(execution, "provvedimentoAggiudicazione");
				acquistiService.ScorriElencoDitteCandidate(execution);	
			}
			dittaCandidata.evidenzia(execution);
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
			String nomeFile="avvisoPostInformazione";
			String labelFile="Avviso di Post-Informazione";
			acquistiService.ProponiDittaAggiudicataria(execution);
			flowsPdfService.makePdf(nomeFile, processInstanceId);
			FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
			documentoGenerato.setLabel(labelFile);
			documentoGenerato.setPubblicazioneTrasparenza(true);
			flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato);

		};break;
		case "consuntivo-end": {
			if(sceltaUtente != null && !sceltaUtente.equals("RevocaConProvvedimento")) {
				pubblicaTuttiFilePubblicabili(execution);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				//attachmentService.setPubblicabileTrasparenza(execution.getId(), "avvisoPostInformazione", true);
				//attachmentService.setPubblicabileTrasparenza(execution.getId(), "modificheVariantiArt106", true);
				if ( execution.getVariable("importoTotaleNetto") != null && Double.compare(Double.parseDouble(execution.getVariable("importoTotaleNetto").toString()), 1000000) > 0) {
					attachmentService.setPubblicabileTrasparenza(execution, "stipula", true);
				}
				if(execution.getVariable("numeroProtocollo_stipula") != null) {
					protocolloDocumentoService.protocollaDocumento(execution, "stipula", execution.getVariable("numeroProtocollo_stipula").toString(), execution.getVariable("dataProtocollo_stipula").toString());
				}
				if(execution.getVariable("numeroProtocollo_contratto") != null) {
					protocolloDocumentoService.protocollaDocumento(execution, "contratto", execution.getVariable("numeroProtocollo_contratto").toString(), execution.getVariable("dataProtocollo_contratto").toString());
				}
			}
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
					attachmentService.setPubblicabileTrasparenza(execution, "ProvvedimentoDiRevoca", true);
			}

		};break; 
		case "end-stipulato-start": {
			pubblicaTuttiFilePubblicabili(execution);
			controllaFilePubblicabiliTrasparenza(execution);
			execution.setVariable(STATO_FINALE_DOMANDA, "STIPULATO");
		};break;     
		case "end-stipulato-end": {
		};break;
		// END CONSUNTIVO  

		// START STIPULA MEPA  
		//		case "stipula-mepa-consip-start": {
		//			if (execution.getVariable("strumentoAcquisizioneId").toString().equals("21")) {
		//				dittaCandidata.evidenzia(execution);
		//			} else {
		//				if ((execution.getVariable("gestioneRTIDittaAggiudicataria") != null) && (execution.getVariable("gestioneRTIDittaAggiudicataria").toString().equals("SI"))) {
		//					dittaCandidata.aggiornaDittaRTIInvitata(execution);
		//				}
		//			}			
		//		};break; 
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
			//			if(execution.getVariable(STATO_FINALE_DOMANDA).toString().equals("STIPULATO")){
			//				pubblicaTuttiFilePubblicabili(execution);
			//			}
		};break;  

		//SUBFLUSSI
		case "DECISIONE-CONTRATTARE-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaSemplice")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					attachmentService.setPubblicabileTrasparenza(execution, value.getName(), false);					
				}
			} else {					
				//attachmentService.setPubblicabileTrasparenza(execution.getId(), "decisioneContrattare", true);
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;

		case "PROVVEDIMENTO-AGGIUDICAZIONE-start": {
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "giustificazioniAnomalie", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "provvedimentoNominaCommissione", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "provvedimentoAmmessiEsclusi", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "esitoValutazioneAnomalie", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "elencoDitteInvitate", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "elencoVerbali", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "bandoAvvisi", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "letteraInvito", true);
			pubblicaTuttiFilePubblicabili(execution);
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
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;	


		case "CONTRATTO-FUORI-MEPA-start": {
			//			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			//			pubblicaFileMultipliPubblicabili(execution, "bandoAvvisi", true);
			//			pubblicaFileMultipliPubblicabili(execution, "letteraInvito", true);
			pubblicaTuttiFilePubblicabili(execution);

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
					attachmentService.setPubblicabileTrasparenza(execution, "contratto", true);
				}
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;



		case "STIPULA-MEPA-start": {
			//			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			//			pubblicaFileMultipliPubblicabili(execution, "bandoAvvisi", true);
			//			pubblicaFileMultipliPubblicabili(execution, "letteraInvito", true);
			pubblicaTuttiFilePubblicabili(execution);

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
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;		
		case "REVOCA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			for (String key : attachmentList.keySet()) {
				FlowsAttachment value = attachmentList.get(key);
				LOGGER.info("Key = " + key + ", Value = " + value);
				//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
			}
			attachmentService.setPubblicabileTrasparenza(execution, "ProvvedimentoDiRevoca", true);
		};break;	
		case "end-revocato": {
		};break;

		// DEFAULT  
		default:  {
		};break;    

		} 
	}
}
