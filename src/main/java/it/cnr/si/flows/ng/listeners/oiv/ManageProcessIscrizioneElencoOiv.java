package it.cnr.si.flows.ng.listeners.oiv;

import it.cnr.si.flows.ng.listeners.oiv.service.CalcolaPunteggioFascia;
import it.cnr.si.flows.ng.listeners.oiv.service.DeterminaAttore;
import it.cnr.si.flows.ng.listeners.oiv.service.GestioneTimer;
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
	private GestioneTimer determinaTimer;
	@Inject
	private CalcolaPunteggioFascia calcolaPunteggioFascia;
	@Inject
	private StartOivSetGroupsAndVisibility startOivSetGroupsAndVisibility;
	@Inject
	private ManageSceltaUtente manageSceltaUtente;
	@Inject
	private DeterminaAttore determinaAttore;

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
		};break;    
		case "istruttoria-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
		};break;     
		case "istruttoria-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			determinaTimer.getTimer(execution, "boundarytimer3");
			calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
			determinaAttore.determinaIstruttore(execution);
		};break;  
		case "soccorso-istruttorio-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		case "soccorso-istruttorio-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
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
		};break;    
		case "preavviso-rigetto-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
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
		default:  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    
		} 
		// Codice per gestire le Scelte
		manageSceltaUtente.azioneScelta(execution, faseEsecuzioneValue, sceltaUtente);
		LOGGER.info("sceltaUtente: " + sceltaUtente);

	}
}
