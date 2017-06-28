package it.cnr.si.flows.ng.listeners.acquistitrasparenza;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;



@Component
public class ScorriElencoDitteCandidate implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ScorriElencoDitteCandidate.class);


	@Override
	public void notify(DelegateExecution execution) throws Exception {

		String ditteCandidateString = (String) execution.getVariable("ditteCandidate_json");
		LOGGER.info("ditteCandidate_json: " + ditteCandidateString);

		JSONArray ditteCandidate = new JSONArray(ditteCandidateString);
		int nrTotaleDitte = ditteCandidate.length();
		LOGGER.info("nrTotaleDitte: " + nrTotaleDitte);
		execution.setVariable("ditteDisponibili", "presenti");

		if (execution.getVariable("nrElencoDitteInit") == null) {
			execution.setVariable("nrElencoDitteTot", ditteCandidate.length());
			execution.setVariable("nrElencoDitteCorrente", 1);
			execution.setVariable("nrElencoDitteInit", "true");

		} else
		{			
			execution.setVariable("nrElencoDitteCorrente", (int) execution.getVariable("nrElencoDitteCorrente") +1);
		}
		int nrElencoDitteCorrente = (int) execution.getVariable("nrElencoDitteCorrente");
		JSONObject dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente);
		execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", dittaCorrente.get("pIvaCodiceFiscaleDittaCandidata"));
		execution.setVariable("ragioneSocialeDittaAggiudicataria", dittaCorrente.get("ragioneSocialeDittaCandidata"));
		if (nrTotaleDitte <= (int) execution.getVariable("nrElencoDitteCorrente")) 
		{
			execution.setVariable("ditteDisponibili", 0);
			execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", "NESSUNA");
			execution.setVariable("ragioneSocialeDittaAggiudicataria", "NESSUNA");
		}
		String codiceVerificheRequisiti = execution.getVariable("verificheRequisitiid").toString();
		if (codiceVerificheRequisiti.equals("1") || codiceVerificheRequisiti.equals("3"))
		{
			execution.setVariable("esitoVerificaRequisiti", "inviaRisultato");
			dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente -1);
			execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", dittaCorrente.get("pIvaCodiceFiscaleDittaCandidata"));
			execution.setVariable("ragioneSocialeDittaAggiudicataria", dittaCorrente.get("ragioneSocialeDittaCandidata"));
		} else if (execution.getVariable("ditteDisponibili").toString().equals("0"))
		{
			execution.setVariable("esitoVerificaRequisiti", "revocaConProvvedimento");
		} else {
			execution.setVariable("esitoVerificaRequisiti", "procediAltroCandidato");
			execution.setVariable("verificheRequisiti", "da verificare");

		}
	}
}

