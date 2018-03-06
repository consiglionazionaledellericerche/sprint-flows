package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;

import org.activiti.engine.delegate.DelegateExecution;

import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;





@Service
public class CalcolaPunteggioFascia {
	private static final Logger LOGGER = LoggerFactory.getLogger(CalcolaPunteggioFascia.class);



	public void calcolaAggiornaGiudizioFinale(DelegateExecution execution, boolean aggiornaGiudizioFinale) throws IOException, ParseException {
		String valutazioneEsperienzeJson = execution.getVariable("valutazioneEsperienze_json").toString();
		String jsonStr = valutazioneEsperienzeJson;
		LOGGER.debug("--- jsonStr: {}", jsonStr);

		JSONArray valutazioni =  new JSONArray(jsonStr);
		int numeroValutazioniPositive = 0;
		int numeroValutazioniNegative = 0;
		int numeroAmbitoPianificazioneKo = 0;
		int numeroAmbitoControlloGestioneKo = 0;
		int numeroAmbitoMisurazionePerformanceKo = 0;
		int numeroAmbitoProgrammazioneFinanziariaKo = 0;
		int numeroAmbitoRiskManagmentKo = 0;
		int numeroAmbitiKo = 0;
		String elencoValutazioniNegative = "";
		String elencoAmbitiKo = "";

		for (int i = 0 ; i < valutazioni.length(); i++) {
			JSONObject obj = valutazioni.getJSONObject(i);
			if (obj.has("giudizioFinaleIstruttore")){
				if((aggiornaGiudizioFinale)){
					obj.put("giudizioFinale", obj.getString("giudizioFinaleIstruttore"));
					LOGGER.info("-- aggiorno giudizioFinale: " + obj.getString("giudizioFinale") + " con giudizioFinaleIstruttore: " + obj.getString("giudizioFinaleIstruttore"));
				}
				if (obj.getString("giudizioFinale").equals("OK")) {
					numeroValutazioniPositive = numeroValutazioniPositive +1;
				} else {
					if(numeroValutazioniNegative >= 1) {
						elencoValutazioniNegative = elencoValutazioniNegative.concat("; ");
					}
					elencoValutazioniNegative = elencoValutazioniNegative.concat(obj.getString("numeroEsperienza"));
					numeroValutazioniNegative = numeroValutazioniNegative +1;
					switch(obj.getString("ambitoEsperienza")){  
					case "CONTROLLO DI GESTIONE": {
						numeroAmbitoControlloGestioneKo = numeroAmbitoControlloGestioneKo +1;
						LOGGER.info("-- ambitoEsperienza: " + obj.getString("ambitoEsperienza") + " nr KO: " + numeroAmbitoControlloGestioneKo);
						if(numeroAmbitoControlloGestioneKo == 1) {
							elencoAmbitiKo = elencoAmbitiKo.concat(" - " + obj.getString("ambitoEsperienza"));
							numeroAmbitiKo = numeroAmbitiKo + 1;
						}
					};break;    
					case "PIANIFICAZIONE": {
						numeroAmbitoPianificazioneKo = numeroAmbitoPianificazioneKo +1;
						LOGGER.info("-- ambitoEsperienza: " + obj.getString("ambitoEsperienza") + " nr KO: " + numeroAmbitoPianificazioneKo);
						if(numeroAmbitoPianificazioneKo == 1) {
							elencoAmbitiKo = elencoAmbitiKo.concat(" - " + obj.getString("ambitoEsperienza"));
							numeroAmbitiKo = numeroAmbitiKo + 1;
						}
					};break;    
					case "MISURAZIONE E VALUTAZIONE DELLA PERFORMANCE ORGANIZZATIVA E INDIVIDUALE": {
						numeroAmbitoMisurazionePerformanceKo = numeroAmbitoMisurazionePerformanceKo +1;
						LOGGER.info("-- ambitoEsperienza: " + obj.getString("ambitoEsperienza") + " nr KO: " + numeroAmbitoMisurazionePerformanceKo);
						if(numeroAmbitoMisurazionePerformanceKo == 1) {
							elencoAmbitiKo = elencoAmbitiKo.concat(" - " + obj.getString("ambitoEsperienza"));
							numeroAmbitiKo = numeroAmbitiKo + 1;
						}
					};break;    
					case "PROGRAMMAZIONE FINANZIARIA E DI BILANCIO": {
						numeroAmbitoProgrammazioneFinanziariaKo = numeroAmbitoProgrammazioneFinanziariaKo +1;
						LOGGER.info("-- ambitoEsperienza: " + obj.getString("ambitoEsperienza") + " nr KO: " + numeroAmbitoProgrammazioneFinanziariaKo);
						if(numeroAmbitoProgrammazioneFinanziariaKo == 1) {
							elencoAmbitiKo = elencoAmbitiKo.concat(" - " + obj.getString("ambitoEsperienza"));
							numeroAmbitiKo = numeroAmbitiKo + 1;
						}
					};break;    
					case "RISK MANAGEMENT": {
						numeroAmbitoRiskManagmentKo = numeroAmbitoRiskManagmentKo +1;
						LOGGER.info("-- ambitoEsperienza: " + obj.getString("ambitoEsperienza") + " nr KO: " + numeroAmbitoRiskManagmentKo);
						if(numeroAmbitoRiskManagmentKo == 1) {
							elencoAmbitiKo = elencoAmbitiKo.concat(" - " + obj.getString("ambitoEsperienza"));
							numeroAmbitiKo = numeroAmbitiKo + 1;
						}
					};break;
					}
				}
			}
		}
		if((aggiornaGiudizioFinale)){
			execution.setVariable("valutazioneEsperienze_json", valutazioni.toString());
		}
		execution.setVariable("numeroValutazioniNegative", numeroValutazioniNegative);
		execution.setVariable("numeroValutazioniPositive", numeroValutazioniPositive);
		execution.setVariable("elencoValutazioniNegative", elencoValutazioniNegative);
		execution.setVariable("numeroAmbitoPianificazioneKo", String.valueOf(numeroAmbitoPianificazioneKo));
		execution.setVariable("numeroAmbitoControlloGestioneKo", String.valueOf(numeroAmbitoControlloGestioneKo));
		execution.setVariable("numeroAmbitoMisurazionePerformanceKo", String.valueOf(numeroAmbitoMisurazionePerformanceKo));
		execution.setVariable("numeroAmbitoProgrammazioneFinanziariaKo", String.valueOf(numeroAmbitoProgrammazioneFinanziariaKo));
		execution.setVariable("numeroAmbitoRiskManagmentKo", String.valueOf(numeroAmbitoRiskManagmentKo));
		execution.setVariable("numeroAmbitiKo", String.valueOf(numeroAmbitiKo));
		execution.setVariable("elencoAmbitiKo", elencoAmbitiKo);

		LOGGER.debug("--- numeroValutazioniNegative: {} numeroValutazioniPositive: {}", numeroValutazioniNegative, numeroValutazioniPositive);
		LOGGER.debug("--- elencoValutazioniNegative: {} ", elencoValutazioniNegative);
		// Chiamta REST applicazione Elenco OIV per il calcolo punteggio
		// invio campi json e recupero fascia e punteggio
		execution.setVariable("punteggioEsperienzeAttribuito", "42");
		execution.setVariable("fasciaAppartenenzaAttribuita", "1");
	}


	public void settaNoAllOggettoSoccrso(DelegateExecution execution) throws IOException, ParseException {
		String valutazioneEsperienzeJson = execution.getVariable("valutazioneEsperienze_json").toString();
		String jsonStr = valutazioneEsperienzeJson;
		LOGGER.debug("--- jsonStr: {}", jsonStr);

		JSONArray valutazioni =  new JSONArray(jsonStr);

		for (int i = 0 ; i < valutazioni.length(); i++) {
			JSONObject obj = valutazioni.getJSONObject(i);
			obj.put("oggettoDiSoccorso", "NO");
			LOGGER.info("-- setto tutti NO ad oggettoDiSoccorso: " + obj.getString("oggettoDiSoccorso"));
		}
		execution.setVariable("valutazioneEsperienze_json", valutazioni.toString());
	}
}