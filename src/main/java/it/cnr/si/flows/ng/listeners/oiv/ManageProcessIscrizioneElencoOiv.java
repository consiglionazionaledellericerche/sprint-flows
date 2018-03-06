package it.cnr.si.flows.ng.listeners.oiv;

import it.cnr.si.flows.ng.listeners.oiv.service.CalcolaPunteggioFascia;
import it.cnr.si.flows.ng.listeners.oiv.service.DeterminaAttore;
import it.cnr.si.flows.ng.listeners.oiv.service.OperazioniTimer;
import it.cnr.si.flows.ng.listeners.oiv.service.ManageSceltaUtente;
import it.cnr.si.flows.ng.listeners.oiv.service.StartOivSetGroupsAndVisibility;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;


@Component
public class ManageProcessIscrizioneElencoOiv implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessIscrizioneElencoOiv.class);

	@Inject
	private OperazioniTimer operazioniTimer;
	@Inject
	private CalcolaPunteggioFascia calcolaPunteggioFascia;
	@Inject
	private StartOivSetGroupsAndVisibility startOivSetGroupsAndVisibility;
	@Inject
	private ManageSceltaUtente manageSceltaUtente;



	private Expression faseEsecuzione;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		//(OivPdfService oivPdfService = new OivPdfService();


		String processInstanceId =  execution.getProcessInstanceId();
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
		case "process-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			startOivSetGroupsAndVisibility.configuraVariabiliStart(execution);
			calcolaPunteggioFascia.settaNoAllOggettoSoccrso(execution);
		};break;    
		case "smistamento-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "smistamento-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "istruttoria-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			if((execution.getVariable("soccorsoIstruttoriaFlag") != null) && (execution.getVariable("soccorsoIstruttoriaFlag").toString().equals("1"))) {
				calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
			} else {
				calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
			}
		};break;     
		case "istruttoria-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			operazioniTimer.determinaTimerScadenzaTermini(execution, "boundarytimer3");
			calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
		};break;  
		case "soccorso-istruttorio-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			//Sospendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso di scadenza tempi proderumantali (boundarytimer6)
			operazioniTimer.sospendiTimerTempiProceduramentali(execution, "boundarytimer3", "boundarytimer6");
		};break;    
		case "soccorso-istruttorio-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			//Riprendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso di scadenza tempi proderumantali (boundarytimer6)
			operazioniTimer.riprendiTimerTempiProceduramentali(execution, "boundarytimer3", "boundarytimer6");
		};break;    
		case "cambio-istruttore-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "cambio-istruttore-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "valutazione-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "valutazione-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
		};break;        
		case "preavviso-rigetto-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			// Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 1 anno
			operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer3", 1, 0, 0, 0, 0);
			// Estende  il timer di avviso scadenza tempi proderumantali (boundarytimer6) a 25 giorni
			operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer6", 1, 0, 0, 0, 0);
		};break;    
		case "preavviso-rigetto-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			// Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 30 giorni
			operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer3", 0, 30, 0, 0, 0);
			// Estende  il timer di avviso scadenza tempi proderumantali (boundarytimer6) a 25 giorni
			operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer6", 0, 25, 0, 0, 0);
		};break;        
		case "istruttoria-su-preavviso-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "istruttoria-su-preavviso-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
		};break;        
		case "valutazione-preavviso-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "valutazione-preavviso-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
		};break;        
		case "firma-dg-rigetto-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "firma-dg-rigetto-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;       
		case "end-improcedibile":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("statoFinaleDomanda", "IMPROCEDIBILE");
		};break;           
		case "end-approvata":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("statoFinaleDomanda", "DOMANDA APPROVATA");
		};break;           
		case "end-respinta":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("statoFinaleDomanda", "RESPINTA");
		};break;
		case "avviso-scadenza-tempi-procedurali-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("tempiProcedimentaliDomanda", "IN SCADENZA");
		};break;           
		case "scadenza-tempi-procedurali-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("tempiProcedimentaliDomanda", "SCADUTI");
		};break;          
		case "scadenza-tempi-soccorso-istruttorio":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("tempiSoccorsoIstruttorio", "SCADUTI");
		};break;          
		case "scadenza-tempi-preavviso-rigetto":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			execution.setVariable("tempiPreavvisoRigetto", "SCADUTI");
		};break;
		default:  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    




		} 
		// Codice per gestire le Scelte
		manageSceltaUtente.azioneScelta(execution, faseEsecuzioneValue, sceltaUtente);
		LOGGER.info("sceltaUtente: " + sceltaUtente);

	}
}
