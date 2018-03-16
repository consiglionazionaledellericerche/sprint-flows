package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsControlService;

import static it.cnr.si.flows.ng.utils.Enum.PdfType.*;

@Service
public class ManageSceltaUtente {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageSceltaUtente.class);

	@Autowired
	private FlowsAttachmentService attachmentService;
	@Inject
	private CreateOivPdf createOivPdf;
	@Inject
	private ManageControlli manageControlli;
	@Inject
	private DeterminaAttore determinaAttore;
	@Inject
	private FlowsControlService flowsControlService;

	public void azioneScelta(DelegateExecution execution, String faseEsecuzioneValue, String sceltaUtente) throws IOException, ParseException {
		String processInstanceId =  execution.getProcessInstanceId();
		LOGGER.info("-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
		if (sceltaUtente != null){
			switch(faseEsecuzioneValue){  
			case "smistamento-end": {
				if(sceltaUtente.equals("prendo_in_carico_la_domanda")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					determinaAttore.determinaIstruttore(execution);
				}
				if(sceltaUtente.equals("richiesta_soccorso_istruttorio")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.verificaPuntiSoccorso(execution);
				}
			};break;
			case "istruttoria-end": {
				if(sceltaUtente.equals("invio_valutazione")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					String valutazioneIstruttore = execution.getVariable("valutazioneIstruttore").toString();
					String esitoValutazione = "negativa";
					if(valutazioneIstruttore.equals("domanda_da_approvare")){
						esitoValutazione = "positiva";
					}
					manageControlli.valutazioneEsperienze(execution, esitoValutazione);
				}
				if(sceltaUtente.equals("richiesta_soccorso_istruttorio")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.verificaPuntiSoccorso(execution);
				}
			};break;
			case "valutazione-end": {
				if(sceltaUtente.equals("genera_PDF_preavviso_di_rigetto")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.valutazioneEsperienzeGenerazionePdf(execution);
					execution.setVariable("pdfPreavvisoRigettoFlag", "1");					
					switch(execution.getVariable("tipologiaRichiesta").toString()){  
					case "iscrizione": {
						createOivPdf.CreaPdfOiv(execution, preavvisoRigetto.name());
					};break;
					case "rinnovo": {
						createOivPdf.CreaPdfOiv(execution, preavvisoRigetto.name());
					};break;
					case "modifica_fascia": {
						createOivPdf.CreaPdfOiv(execution, preavvisoRigettoCambioFascia.name());
					};break;
					default:  {
						LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
					};break;    
					}			
				}
				if(sceltaUtente.equals("richiesta_soccorso_istruttorio")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.verificaPuntiSoccorso(execution);
				}
				if(sceltaUtente.equals("invia_preavviso_di_rigetto")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					String nomeFilePreavviso = "preavvisoRigetto";
					FlowsAttachment fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get("preavvisoRigetto");
					if (fileRecuperato != null){
						nomeFilePreavviso = fileRecuperato.getName();
					} else {
						fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get("preavvisoRigettoCambioFascia");
						if (fileRecuperato != null){
							nomeFilePreavviso = fileRecuperato.getName();
						}		
					}
					LOGGER.info("-- verificaFileFirmatoP7m: nomeFilePreavviso:" + nomeFilePreavviso);
					flowsControlService.verificaFileFirmato_Cades_Pades(execution, nomeFilePreavviso);
				}
				if(sceltaUtente.equals("approva")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					String esitoValutazione = "positiva";
					manageControlli.valutazioneEsperienze(execution, esitoValutazione);
				}
			};break;    
			case "soccorso-istruttorio-start": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				execution.setVariable("soccorsoIstruttoriaFlag", "1");
			};break;
			case "istruttoria-su-preavviso-end": {
				if(sceltaUtente.equals("invia_alla_valutazione")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					String valutazioneIstruttore = execution.getVariable("valutazioneIstruttore").toString();
					String esitoValutazione = "negativa";
					if(valutazioneIstruttore.equals("domanda_da_approvare")){
						esitoValutazione = "positiva";
					}
					manageControlli.valutazioneEsperienze(execution, esitoValutazione);
				}
			};break;	
			case "valutazione-preavviso-end": {
				if(sceltaUtente.equals("genera_PDF_rigetto")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.valutazioneEsperienzeGenerazionePdf(execution);
					execution.setVariable("pdfRigettoFlag", "1");
					if(((execution.getVariable("tempiPreavvisoRigetto")  != null) && (execution.getVariable("tempiPreavvisoRigetto").toString().equals("SCADUTI")))){
						createOivPdf.CreaPdfOiv(execution, RigettoDef10Giorni.name());
					} else {
						createOivPdf.CreaPdfOiv(execution, rigettoMotivato.name());
					}
				}
				if(sceltaUtente.equals("approva")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					String esitoValutazione = "positiva";
					manageControlli.valutazioneEsperienze(execution, esitoValutazione);
				}
			};break;
			case "firma-dg-rigetto-end": {
				if(sceltaUtente.equals("invia_rigetto_firmato")) {
					String nomeFileRigetto = "rigettoMotivato";
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					FlowsAttachment fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get("rigettoMotivato");
					if (fileRecuperato != null){
						nomeFileRigetto = fileRecuperato.getName();
					} else {
						fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get("RigettoDef10Giorni");
						if (fileRecuperato != null){
							nomeFileRigetto = fileRecuperato.getName();
						}		
					}
					LOGGER.info("-- verificaFileFirmatoP7m: nomeFileRigetto:" + nomeFileRigetto);
					flowsControlService.verificaFileFirmato_Cades_Pades(execution, nomeFileRigetto);
				}
			};break;		
			default:  {
				LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			};break;    
			}
		}
	}
}
