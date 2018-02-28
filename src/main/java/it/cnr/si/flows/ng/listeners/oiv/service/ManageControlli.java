package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;


import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ManageControlli {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageControlli.class);

	public void valutazioneEsperienze(DelegateExecution execution) throws IOException, ParseException {
		String numeroValutazioniPositive = execution.getVariable("numeroValutazioniPositive").toString();
		String numeroValutazioniNegative = execution.getVariable("numeroValutazioniNegative").toString();
		String valutazioneIstruttore = execution.getVariable("valutazioneIstruttore").toString();
		if((numeroValutazioniPositive.equals("0")) && valutazioneIstruttore.equals("domanda_da_approvare")){
			LOGGER.info("-- numeroValutazioniPositive: " + numeroValutazioniPositive );
			throw new BpmnError("412", "La scelta 'domanda_da_approvare' non risulta congruente<br>con la valutazione negativa di tutte le esperienze<br>");
		}
		if((numeroValutazioniNegative.equals("0")) && valutazioneIstruttore.equals("domanda_da_respingere")){
			LOGGER.info("-- numeroValutazioniNegative: " + numeroValutazioniNegative );
			throw new BpmnError("412", "La scelta 'domanda_da_respingere' non risulta congruente<br>con la valutazione positiva di tutte le esperienze<br>");
		}
	}

	// Verifica che almento un elemento sia richiesto per il soccorsoistruttorio 
	public void verificaPuntiSoccorso(DelegateExecution execution) throws IOException, ParseException {
		String valutazioneEsperienzeJson = execution.getVariable("valutazioneEsperienze_json").toString();
		String jsonStr = valutazioneEsperienzeJson;
		LOGGER.debug("--- jsonStr: {}", jsonStr);

		JSONArray valutazioni =  new JSONArray(jsonStr);
		int numeroPuntiOggettoDiSoccorso = 0;
		for (int i = 0 ; i < valutazioni.length(); i++) {
			JSONObject obj = valutazioni.getJSONObject(i);
			if (obj.has("oggettoDiSoccorso")){
				if (obj.getString("oggettoDiSoccorso").equals("SI")) {
					numeroPuntiOggettoDiSoccorso = numeroPuntiOggettoDiSoccorso +1;
				} 
			} 
		}

		LOGGER.debug("--- numeroPuntiOggettoDiSoccorso: {} ", numeroPuntiOggettoDiSoccorso);
		// Chiamta REST applicazione Elenco OIV per il calcolo punteggio
		// invio campi json e recupero fascia e punteggio
		if (numeroPuntiOggettoDiSoccorso == 0) {
			throw new BpmnError("412", "Per effettuare la scelta 'richiesta_soccorso_istruttorio' <br>almeno un'esperienza deve essere seleziona come 'Oggetto di soccorso'<br>");
		} else {
			LOGGER.debug("--- numeroPuntiOggettoDiSoccorso: {} > 0 ---> OK", numeroPuntiOggettoDiSoccorso);
		}
	}
}

