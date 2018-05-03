package it.cnr.si.flows.ng.listeners.oiv;



import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.listeners.oiv.service.CalcolaPunteggioFascia;
import it.cnr.si.flows.ng.listeners.oiv.service.ManageControlli;
import it.cnr.si.flows.ng.listeners.oiv.service.ManageSceltaUtente;
import it.cnr.si.flows.ng.listeners.oiv.service.OivSetGroupsAndVisibility;
import it.cnr.si.flows.ng.listeners.oiv.service.OperazioniTimer;
import it.cnr.si.flows.ng.listeners.oiv.service.OivSetGroupsAndVisibility;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;


@Component
public class ManageProcessIscrizioneElencoOiv implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessIscrizioneElencoOiv.class);

	@Inject
	private OperazioniTimer operazioniTimer;
	@Inject
	private CalcolaPunteggioFascia calcolaPunteggioFascia;
	@Inject
	@Autowired(required = false)
	private OivSetGroupsAndVisibility oivSetGroupsAndVisibility;
	@Inject
	private ManageSceltaUtente manageSceltaUtente;
	@Autowired(required = false)
	private RestTemplate oivRestTemplate;
	@Inject
	private Environment env;
	@Inject
	private ManageControlli manageControlli;

	private Expression faseEsecuzione;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		//(OivPdfService oivPdfService = new OivPdfService();


		String processInstanceId =  execution.getProcessInstanceId();
		String sceltaUtente = "start";
		if(execution.getVariable("sceltaUtente") != null) {
			sceltaUtente =  (String) execution.getVariable("sceltaUtente");	
		}
		Date dataNow = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		String simpleDataNow = formatter.format(dataNow);
		
		
		LOGGER.info("ProcessInstanceId: " + processInstanceId);
		String faseEsecuzioneValue = "noValue";
		boolean aggiornaGiudizioFinale = true;
		boolean nonAggiornaGiudizioFinale = false;
		faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		switch(faseEsecuzioneValue){  
		case "process-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			oivSetGroupsAndVisibility.configuraVariabiliStart(execution);
			manageControlli.verificaUnicaDomandaAttivaUtente(execution);
			calcolaPunteggioFascia.settaNoAllOggettoSoccrso(execution);
		};break;    
		case "smistamento-start": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
		};break;     
		case "smistamento-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			operazioniTimer.determinaTimerScadenzaTermini(execution, "boundarytimer3");
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
			calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
		};break;  
		case "soccorso-istruttorio-start":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			//Sospendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso di scadenza tempi proderumantali (boundarytimer6)
			operazioniTimer.sospendiTimerTempiProceduramentali(execution, "boundarytimer3", "boundarytimer6");
			execution.setVariable("dataInizioSoccorsoIstruttorio", simpleDataNow);
		};break;    
		case "soccorso-istruttorio-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			//Riprendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso di scadenza tempi proderumantali (boundarytimer6)
			operazioniTimer.riprendiTimerTempiProceduramentali(execution, "boundarytimer3", "boundarytimer6");
			execution.setVariable("dataFineSoccorsoIstruttorio", simpleDataNow);
			execution.setVariable("giorniDurataSoccorsoIstruttorio", operazioniTimer.calcolaGiorniTraDateString(execution.getVariable("dataInizioSoccorsoIstruttorio").toString(), execution.getVariable("dataFineSoccorsoIstruttorio").toString()));
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
			execution.setVariable("dataInizioPreavvisoRigetto", simpleDataNow);
		};break;    
		case "preavviso-rigetto-end":  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
			// Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 30 giorni
			operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer3", 0, 0, 30, 0, 0);
			// Estende  il timer di avviso scadenza tempi proderumantali (boundarytimer6) a 25 giorni
			operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer6", 0, 0, 25, 0, 0);
			execution.setVariable("dataFinePreavvisoRigetto", simpleDataNow);
			execution.setVariable("giorniDurataPreavvisoRigetto", operazioniTimer.calcolaGiorniTraDateString(execution.getVariable("dataInizioPreavvisoRigetto").toString(), execution.getVariable("dataFinePreavvisoRigetto").toString()));
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
			execution.setVariable("numeroIscrizioneInElenco",
				iscriviInElenco(Optional.ofNullable(execution.getVariable("idDomanda"))
						.filter(String.class::isInstance)
						.map(String.class::cast)
						.orElse(null)));
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
		case "process-end": {
			LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
			oivSetGroupsAndVisibility.configuraVariabiliEnd(execution);
		};break; 
		default:  {
			LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
		};break;    

		} 
		// Codice per gestire le Scelte
		manageSceltaUtente.azioneScelta(execution, faseEsecuzioneValue, sceltaUtente);
		LOGGER.info("sceltaUtente: " + sceltaUtente);
		//print della fase
		LOGGER.info("faseUltima: " + execution.getVariable("faseUltima"));

	}

	private String iscriviInElenco(String id) {
		final RelaxedPropertyResolver relaxedPropertyResolver = new RelaxedPropertyResolver(env, "oiv.");
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(relaxedPropertyResolver.getProperty("iscrivi-inelenco"))
				.queryParam("idDomanda", id);
		return Optional.ofNullable(oivRestTemplate.getForEntity(builder.buildAndExpand().toUri(), Map.class))
				.filter(mapResponseEntity -> mapResponseEntity.getStatusCode() == HttpStatus.OK)
				.map(ResponseEntity::getBody)
				.map(map -> map.get("progressivo"))
				.map(Integer.class::cast)
				.map(String::valueOf)
				.orElse(null);
	}

}
