package it.cnr.si.flows.ng.listeners.cnr.acquisti.service;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Sostituzione;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Sostituito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.util.json.JSONException;
import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;

import it.cnr.si.flows.ng.dto.FlowsAttachment;


@Profile(value = "cnr")
@Service
public class AcquistiService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AcquistiService.class);

	@Inject
	private FlowsAttachmentService attachmentService;

	public void OrdinaElencoDitteCandidate(DelegateExecution execution) {

		String ditteInvitateString = null;
		if(execution.getVariable("ditteInvitate_json") != null && !execution.getVariable("tipologiaAcquisizione").toString().equalsIgnoreCase("Procedura aperta")) {
			ditteInvitateString = (String) execution.getVariable("ditteInvitate_json");
			LOGGER.info("ditteInvitate_json: " + ditteInvitateString);
		} else {
			//Se si tratta di procedura aperta
			if(execution.getVariable("ditteCandidateInput_json") != null && execution.getVariable("tipologiaAcquisizione").toString().equalsIgnoreCase("Procedura aperta")) {
				ditteInvitateString = (String) execution.getVariable("ditteCandidateInput_json");
				LOGGER.info("ditteCandidateInput_json: " + ditteInvitateString);
			}
		}
		JSONArray ditteInvitate = new JSONArray(ditteInvitateString);
		int nrTotaleDitte = ditteInvitateString.length();
		LOGGER.info("nrTotaleDitte: " + nrTotaleDitte);

		JSONArray sortedJsonArray = new JSONArray();

		List<JSONObject> jsonValues = new ArrayList<JSONObject>();
		for (int i = 0; i < ditteInvitate.length(); i++) {
			//Se si tratta di procedura aperta
			if(execution.getVariable("ditteInvitate_json") != null && !execution.getVariable("tipologiaAcquisizione").toString().equalsIgnoreCase("Procedura aperta")) {
				if (ditteInvitate.getJSONObject(i).get("offertaPresentataDittaInvitata").toString().equals("SI")) {
					jsonValues.add(ditteInvitate.getJSONObject(i));
				}
			} else {
				jsonValues.add(ditteInvitate.getJSONObject(i));
			}
			if (!ditteInvitate.getJSONObject(i).has("pIvaCodiceFiscaleDittaInvitata")) {
				ditteInvitate.getJSONObject(i).put("pIvaCodiceFiscaleDittaInvitata", "non presente");
			}
		}
		JSONArray ditteAppo = new JSONArray(jsonValues.toString());
		Collections.sort( jsonValues, new Comparator<JSONObject>() {
			//You can change "Name" with "ID" if you want to sort by ID
			private static final String KEY_NAME = "valutazioneDittaInvitata";

			@Override
			public int compare(JSONObject a, JSONObject b) {
				int valA = 0;
				int valB = 0;

				try {
					valA = (int) a.get(KEY_NAME);
					valB = (int) b.get(KEY_NAME);
				} 
				catch (JSONException e) {
					//do something
				}

				return -Integer.compare(valA, valB);
				//if you want to change the sort order, simply use the following:
				//return -valA.compareTo(valB);
			}
		});

		for (int i = 0; i < ditteAppo.length(); i++) {
			sortedJsonArray.put(jsonValues.get(i));
		}
		LOGGER.info("sortedJsonArray: " + sortedJsonArray);

		execution.setVariable("ditteCandidate_json", sortedJsonArray.toString());
	}


	public void ScorriElencoDitteCandidate(DelegateExecution execution) {
		String ditteCandidateString = (String) execution.getVariable("ditteCandidate_json");
		LOGGER.info("ditteCandidate_json: " + ditteCandidateString);

		JSONArray ditteCandidate = new JSONArray(ditteCandidateString);
		int nrTotaleDitte = ditteCandidate.length();
		LOGGER.info("nrTotaleDitte: " + nrTotaleDitte);

		if (execution.getVariable("nrElencoDitteInit") == null) {
			execution.setVariable("nrElencoDitteTot", ditteCandidate.length());
			execution.setVariable("nrElencoDitteCorrente", 1);
			execution.setVariable("nrElencoDitteInit", "true");
			execution.setVariable("ditteRimanenti", nrTotaleDitte -1);
		} else
		{			
			execution.setVariable("nrElencoDitteCorrente", (int) execution.getVariable("nrElencoDitteCorrente") +1);
			execution.setVariable("ditteRimanenti", (nrTotaleDitte - (int) execution.getVariable("nrElencoDitteCorrente")) -1);
		}
		int nrElencoDitteCorrente = (int) execution.getVariable("nrElencoDitteCorrente");
		JSONObject dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente -1);
		String codiceVerificheRequisiti = execution.getVariable("verificheRequisitiid").toString();
		execution.setVariable("pIvaCodiceFiscaleDittaInvitata", null);
		execution.setVariable("ragioneSocialeDittaInvitata", null);
		execution.setVariable("gestioneRTIDittaInvitata", null);
		if (codiceVerificheRequisiti.equals("1")) {
			execution.setVariable("esitoVerificaRequisiti", "inviaRisultato");
		} else if (nrTotaleDitte <= (int) execution.getVariable("nrElencoDitteCorrente")) {
			execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", "NESSUNA");
			execution.setVariable("ragioneSocialeDittaAggiudicataria", "NESSUNA");
			execution.setVariable("ditteRTI_json", null);
			execution.setVariable("esitoVerificaRequisiti", "revoca");
		} else {
			dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente);
			execution.setVariable("ditteRTI_json", null);
			execution.setVariable("esitoVerificaRequisiti", "procediAltroCandidato");
			execution.setVariable("verificheRequisiti", "da verificare");
		}
	}

	public void ProponiDittaAggiudicataria(DelegateExecution execution) {
		if (execution.getVariable("verificheRequisitiid") != null && execution.getVariable("verificheRequisitiid").toString().equals("1")) {
			if (execution.getVariable("pIvaCodiceFiscaleDittaAggiudicataria") == null) {
				execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", execution.getVariable("pIvaCodiceFiscaleDittaCandidata"));
				execution.setVariable("ragioneSocialeDittaAggiudicataria", execution.getVariable("ragioneSocialeDittaCandidata"));
				execution.setVariable("gestioneRTIDittaAggiudicataria", execution.getVariable("gestioneRTIDittaCandidata"));
			}
		}
	}

	public void SostituisciDocumento(DelegateExecution execution, String nomeFileDaSostituire) {

		if (nomeFileDaSostituire == null)
			throw new IllegalStateException("Questo metodo ha bisogno del campo 'nomeFileDaSostituire' nella process definition (nel Task Listener - Fields).");

		FlowsAttachment originale = (FlowsAttachment) execution.getVariable(nomeFileDaSostituire);
		FlowsAttachment copia     = SerializationUtils.clone(originale);

		LOGGER.debug("Ricarico il file {} originale, ma con gli stati puliti", nomeFileDaSostituire);
		originale.clearStato();
		originale.setAzione(Sostituzione);
		attachmentService.saveAttachment(execution, nomeFileDaSostituire, originale, null);

		LOGGER.debug("Salvo una copia per futuro riferimento");
		copia.setAzione(Sostituzione);
		copia.addStato(Sostituito);
		copia.setName("Provvedimento di Aggiudicazione Sostiutito");
		// TODO il nome "provvedimentiRespinti" dovrebbe sempre essere un Expression
		attachmentService.saveAttachmentInArray(execution, "provvedimentiRespinti", copia);
	}
}
