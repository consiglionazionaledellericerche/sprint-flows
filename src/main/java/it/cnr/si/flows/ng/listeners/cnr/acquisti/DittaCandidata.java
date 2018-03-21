package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import java.io.IOException;
import java.text.ParseException;
import org.activiti.engine.delegate.DelegateExecution;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import it.cnr.si.flows.ng.listeners.oiv.service.DeterminaAttore;


@Service
public class DittaCandidata {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeterminaAttore.class);


	public void evidenzia(DelegateExecution execution) throws IOException, ParseException {


		String ditteCandidateString = (String) execution.getVariable("ditteCandidate_json");
		LOGGER.info("ditteCandidate_json: " + ditteCandidateString);

		JSONArray ditteCandidate = new JSONArray(ditteCandidateString);
		int nrTotaleDitte = ditteCandidate.length();
		LOGGER.info("nrTotaleDitte: " + nrTotaleDitte);
		execution.setVariable("ditteDisponibili", "presenti");

		if (execution.getVariable("nrElencoDitteCorrente") == null) {
			execution.setVariable("nrElencoDitteCorrente", 0);
		} 
		int nrElencoDitteCorrente = (int) execution.getVariable("nrElencoDitteCorrente");
		JSONObject dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente);
		execution.setVariable("pIvaCodiceFiscaleDittaCandidata", dittaCorrente.get("pIvaCodiceFiscaleDittaCandidata"));
		execution.setVariable("ragioneSocialeDittaCandidata", dittaCorrente.get("ragioneSocialeDittaCandidata"));
	}
}
