package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.rest.common.api.DataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;


@Service
public class ManageControlli {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageControlli.class);

	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;	
    @Inject
    HistoryService historyService;
    
	public void verificaUnicaDomandaAttivaUtente(DelegateExecution execution) throws IOException, ParseException {
		String processDefinitionId = execution.getProcessDefinitionId();
		String[] parts = processDefinitionId.split(":");
		String processDefinitionKey = parts[0];        
        String codiceFiscaleRichiedente = "textEqual=" + (String) execution.getVariable("codiceFiscaleRichiedente");
		boolean active = true;
		String order = "ASC";
		int firstResult = 1;
		int maxResults = 100;

		//Map<String, String> req = new HashMap<String, String>();
        Map<String, String> req = new HashMap<>();

//todo:oiv
		req.put("codiceFiscaleRichiedente", codiceFiscaleRichiedente);
//		Map<String, Object> map = flowsProcessInstanceService.search(req, processDefinitionKey, active, order, firstResult, maxResults);
		DataResponse map = flowsProcessInstanceService.search(req, processDefinitionKey, active, order, firstResult, maxResults, false);


		//LOGGER.info("-- map[0]: " +  map[0]);
		//if(map.size() > 0){
//		if(!map.get("totalItems").toString().equals("0")){
		if(map.getTotal() != 0 ){
			LOGGER.info("-- Impossibile avviare il flusso un altro flusso risulta giù attivo per il codice fiscale '"+ codiceFiscaleRichiedente +"'" );
			throw new BpmnError("412", "Impossibile avviare il flusso<br>un altro flusso risulta giù attivo per il codice fiscale '"+ codiceFiscaleRichiedente +"'<br>");
		}
	}
	
	public void valutazioneEsperienze(DelegateExecution execution, String esitoValutazione) throws IOException, ParseException {
		String numeroValutazioniPositive = execution.getVariable("numeroValutazioniPositive").toString();
		String numeroValutazioniNegative = execution.getVariable("numeroValutazioniNegative").toString();
		LOGGER.info("-- numeroValutazioniPositive: " + numeroValutazioniPositive + "-- numeroValutazioniNegative: " + numeroValutazioniNegative );
		if((numeroValutazioniPositive.equals("0")) && esitoValutazione.equals("positiva")){
			LOGGER.info("-- numeroValutazioniPositive: " + numeroValutazioniPositive );
			throw new BpmnError("412", "La scelta '"+ execution.getVariable("sceltaUtente").toString() + "' non risulta congruente<br>con la valutazione negativa di tutte le esperienze<br>");
		}
		if((numeroValutazioniNegative.equals("0")) && esitoValutazione.equals("negativa")){
			LOGGER.info("-- numeroValutazioniNegative: " + numeroValutazioniNegative );
			throw new BpmnError("412", "La scelta '"+ execution.getVariable("sceltaUtente").toString() + "' non risulta congruente<br>con la valutazione positiva di tutte le esperienze<br>");
		}
	}

	
	public void valutazioneEsperienzeGenerazionePdf(DelegateExecution execution) throws IOException, ParseException {
		String numeroValutazioniPositive = execution.getVariable("numeroValutazioniPositive").toString();
		String numeroValutazioniNegative = execution.getVariable("numeroValutazioniNegative").toString();
		LOGGER.info("-- numeroValutazioniPositive: " + numeroValutazioniPositive + "-- numeroValutazioniNegative: " + numeroValutazioniNegative );
		if(numeroValutazioniNegative.equals("0")){
			LOGGER.info("-- numeroValutazioniNegative: " + numeroValutazioniNegative );
			throw new BpmnError("412", "La scelta '"+ execution.getVariable("sceltaUtente").toString() + "' non risulta congruente<br>con la valutazione positiva di tutte le esperienze<br>");
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

