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
		execution.setVariable("nrElencoDitteInit", true);

		JSONArray ditteCandidate = new JSONArray(ditteCandidateString);
		int nrTotaleDitte = ditteCandidate.length();
		LOGGER.info("nrTotaleDitte: " + nrTotaleDitte);

		if (execution.getVariable("ditteRimanenti") == null) {
			execution.setVariable("ditteRimanenti", nrTotaleDitte -1);
		} 
		if (execution.getVariable("nrElencoDitteCorrente") == null) {
			execution.setVariable("nrElencoDitteCorrente", 0);
		}
		int nrElencoDitteCorrente = (int) execution.getVariable("nrElencoDitteCorrente");
		JSONObject dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente);
		execution.setVariable("pIvaCodiceFiscaleDittaCandidata", dittaCorrente.get("pIvaCodiceFiscaleDittaInvitata"));
		execution.setVariable("ragioneSocialeDittaCandidata", dittaCorrente.get("ragioneSocialeDittaInvitata"));
		execution.setVariable("gestioneRTIDittaCandidata", dittaCorrente.get("gestioneRTIDittaInvitata"));
	}

	public void aggiornaDittaRTIInvitata(DelegateExecution execution) throws IOException, ParseException {


		String ditteRTIString = (String) execution.getVariable("ditteRTI_json");
		LOGGER.info("ditteRTI_json: " + ditteRTIString);

		JSONArray ditteRTI = new JSONArray(ditteRTIString);
		int nrTotaleDitteRTI = ditteRTI.length();
		LOGGER.info("nrTotaleDitteRTI: " + nrTotaleDitteRTI);
		for(int i=0; i<nrTotaleDitteRTI; i++) {
			JSONObject dittaCorrente = ditteRTI.getJSONObject(i);
			if (dittaCorrente.get("tipologiaRTI").equals("MANDATARIA") || dittaCorrente.get("tipologiaRTI").equals("CAPOGRUPPO")) {
				execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", dittaCorrente.get("pIvaCodiceFiscaleDittaInvitata"));
				execution.setVariable("ragioneSocialeDittaAggiudicataria", dittaCorrente.get("ragioneSocialeDittaInvitata"));
			}
		}
	}

}
