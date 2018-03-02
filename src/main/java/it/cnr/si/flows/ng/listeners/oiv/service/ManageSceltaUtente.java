package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import static it.cnr.si.flows.ng.utils.Enum.PdfType.*;

@Service
public class ManageSceltaUtente {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageSceltaUtente.class);


	@Inject
	private CreateOivPdf createOivPdf;
	@Inject
	private ManageControlli manageControlli;
	@Inject
	private DeterminaAttore determinaAttore;
	public void azioneScelta(DelegateExecution execution, String faseEsecuzioneValue, String sceltaUtente) throws IOException, ParseException {
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
					manageControlli.valutazioneEsperienze(execution);
				}
				if(sceltaUtente.equals("richiesta_soccorso_istruttorio")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					manageControlli.verificaPuntiSoccorso(execution);
				}
			};break;
			case "valutazione-end": {
				if(sceltaUtente.equals("genera_PDF_preavviso_di_rigetto")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
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
			};break;    
			case "soccorso-istruttorio-start": {
				LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
				execution.setVariable("soccorsoIstruttoriaFlag", "1");
			};break;
			case "valutazione-preavviso-end": {
				if(sceltaUtente.equals("genera_PDF_rigetto")) {
					LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
					execution.setVariable("pdfRigettoFlag", "1");
					if(((execution.getVariable("tempiPreavvisoRigetto")  != null) && (execution.getVariable("tempiPreavvisoRigetto").toString().equals("SCADUTI")))){
						createOivPdf.CreaPdfOiv(execution, RigettoDef10Giorni.name());
					} else {
						createOivPdf.CreaPdfOiv(execution, rigettoMotivato.name());
					}
				}
			};break;
			default:  {
				LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			};break;    
			}
		}
	}
}
