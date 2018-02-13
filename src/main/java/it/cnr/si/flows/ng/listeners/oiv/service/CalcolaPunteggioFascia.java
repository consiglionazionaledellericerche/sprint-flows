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



	public void calcola(DelegateExecution execution) throws IOException, ParseException {
		String valutazioneEsperienzeJson = execution.getVariable("valutazioneEsperienze_json").toString();
		String jsonStr = valutazioneEsperienzeJson;
		LOGGER.debug("--- jsonStr: {}", jsonStr);

		JSONArray valutazioni =  new JSONArray(jsonStr);
		int numeroValutazioniPositive = 0;
		int numeroValutazioniNegative = 0;
		String elencoValutazioniNegative = "";
		for (int i = 0 ; i < valutazioni.length(); i++) {
			JSONObject obj = valutazioni.getJSONObject(i);
			if (obj.has("giudizioFinale")) {
				if (obj.getString("giudizioFinale").equals("OK")) {
					numeroValutazioniPositive = numeroValutazioniPositive +1;
				} else {
					if(numeroValutazioniNegative >= 1) {
						elencoValutazioniNegative = elencoValutazioniNegative.concat("; ");
					}
					elencoValutazioniNegative = elencoValutazioniNegative.concat(obj.getString("numeroEsperienza"));
					numeroValutazioniNegative = numeroValutazioniNegative +1;
				}
			}
		}

		execution.setVariable("numeroValutazioniNegative", numeroValutazioniNegative);
		execution.setVariable("numeroValutazioniPositive", numeroValutazioniPositive);
		execution.setVariable("elencoValutazioniNegative", elencoValutazioniNegative);

		LOGGER.debug("--- numeroValutazioniNegative: {} numeroValutazioniPositive: {}", numeroValutazioniNegative, numeroValutazioniPositive);
		LOGGER.debug("--- elencoValutazioniNegative: {} ", elencoValutazioniNegative);
		// Chiamta REST applicazione Elenco OIV per il calcolo punteggio
		// invio campi json e recupero fascia e punteggio
		execution.setVariable("punteggioEsperienzeAttribuito", "23");
		execution.setVariable("fasciaAppartenenzaAttribuita", "3");
	}

}
