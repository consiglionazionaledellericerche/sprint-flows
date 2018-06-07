package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;

import javax.inject.Inject;

import it.cnr.si.flows.ng.listeners.oiv.FaseEsecuzioneEnum;
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
		FaseEsecuzioneEnum faseEsecuzione = FaseEsecuzioneEnum.fromValue(faseEsecuzioneValue);
		if (sceltaUtente != null){
			switch(faseEsecuzione){
				case SMISTAMENTO_END: {
				if(sceltaUtente.equals("prendo_in_carico_la_domanda")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					determinaAttore.determinaIstruttore(execution);
				}
				if(sceltaUtente.equals("richiesta_soccorso_istruttorio")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.verificaPuntiSoccorso(execution);
				}
			};break;
				case ISTRUTTORIA_END: {
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
				case VALUTAZIONE_END: {
				if(sceltaUtente.equals("genera_PDF_preavviso_di_rigetto")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.valutazioneEsperienzeGenerazionePdf(execution);
					execution.setVariable("pdfPreavvisoRigettoFlag", "1");					
					switch(execution.getVariable("tipologiaRichiesta").toString()){  
					case "Iscrizione": {
						createOivPdf.creaPdfOiv(execution, preavvisoRigetto.name());
					};break;
					case "rinnovo": {
						createOivPdf.creaPdfOiv(execution, preavvisoRigetto.name());
					};break;
					case "modifica_fascia": {
						createOivPdf.creaPdfOiv(execution, preavvisoRigetto.name());
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
				case SOCCORSO_ISTRUTTORIO_START: {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				execution.setVariable("soccorsoIstruttoriaFlag", "1");
			};break;
				case ISTRUTTORIA_SU_PREAVVISO_END: {
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
				case VALUTAZIONE_PREAVVISO_END: {
				if(sceltaUtente.equals("genera_PDF_rigetto")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.valutazioneEsperienzeGenerazionePdf(execution);
					execution.setVariable("pdfRigettoFlag", "1");
					if(((execution.getVariable("tempiPreavvisoRigetto")  != null) && (execution.getVariable("tempiPreavvisoRigetto").toString().equals("SCADUTI")))){
						createOivPdf.creaPdfOiv(execution, RigettoDef10Giorni.name());
					} else {
						createOivPdf.creaPdfOiv(execution, rigettoMotivato.name());
					}
				}
				if(sceltaUtente.equals("approva")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					String esitoValutazione = "positiva";
					manageControlli.valutazioneEsperienze(execution, esitoValutazione);
				}
			};break;
				case FIRMA_DG_RIGETTO_END: {
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
