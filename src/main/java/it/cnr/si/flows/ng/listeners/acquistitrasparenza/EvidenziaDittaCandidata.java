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
public class EvidenziaDittaCandidata implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(EvidenziaDittaCandidata.class);


@Override
public void notify(DelegateExecution execution) throws Exception {

	String ditteCandidateString = (String) execution.getVariable("ditteCandidate_json");
	LOGGER.info("ditteCandidate_json: " + ditteCandidateString);

	JSONArray ditteCandidate = new JSONArray(ditteCandidateString);
	int nrTotaleDitte = ditteCandidate.length();
	LOGGER.info("nrTotaleDitte: " + nrTotaleDitte);
	execution.setVariable("ditteDisponibili", "presenti");

	if (execution.getVariable("nrElencoDitteCorrente") == null) {
		execution.setVariable("nrElencoDitteCorrente", 1);
	} 
	int nrElencoDitteCorrente = (int) execution.getVariable("nrElencoDitteCorrente");
	JSONObject dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente -1);
	execution.setVariable("pIvaCodiceFiscaleDittaCandidata", dittaCorrente.get("pIvaCodiceFiscaleDittaCandidata"));
	execution.setVariable("ragioneSocialeDittaCandidata", dittaCorrente.get("ragioneSocialeDittaCandidata"));
}
}
