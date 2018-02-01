package it.cnr.si.flows.ng.listeners.oiv;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;





@Component
public class CalcolaPunteggioFascia implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CalcolaPunteggioFascia.class);



	@Override
	public void notify(DelegateExecution execution) throws Exception {
		String valutazioneEsperienzeJson = execution.getVariable("valutazioneEsperienze_json").toString();
		String jsonStr = valutazioneEsperienzeJson;
		LOGGER.debug("--- jsonStr: {}", jsonStr);

		JSONArray valutazioni =  new JSONArray(jsonStr);
		int numeroValutazioniPositive = 0;
		for (int i = 0 ; i < valutazioni.length(); i++) {
			JSONObject obj = valutazioni.getJSONObject(i);
			if (obj.has("giudizioFinale")) {
				if (obj.getString("giudizioFinale").equals("OK")) {
					numeroValutazioniPositive = numeroValutazioniPositive +1;
				}
			}
			System.out.println("Si sono evidenziate nr. " + numeroValutazioniPositive + " valutazioni positive su un totale di: " + valutazioni.length());
		}
		int numeroValutazioniNegative = valutazioni.length() - numeroValutazioniPositive;

		execution.setVariable("numeroValutazioniNegative", numeroValutazioniNegative);
		execution.setVariable("numeroValutazioniPositive", numeroValutazioniPositive);

		LOGGER.debug("--- numeroValutazioniNegative: {} numeroValutazioniPositive: {}", numeroValutazioniNegative, numeroValutazioniPositive);
		// Chiamta REST applicazione Elenco OIV per il calcolo punteggio
		// invio campi json e recupero fascia e punteggio
		execution.setVariable("punteggioEsperienzeAttribuito", "23");
		execution.setVariable("fasciaAppartenenzaAttribuita", "3");
	}

}
