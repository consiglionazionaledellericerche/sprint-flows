package it.cnr.si.flows.ng.listeners.cnr.acquisti.service;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Sostituzione;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Sostituito;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
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
		execution.setVariable("pIvaCodiceFiscaleDittaCandidata", null);
		execution.setVariable("ragioneSocialeDittaCandidata", null);
		execution.setVariable("gestioneRTIDittaCandidata", null);
		if (codiceVerificheRequisiti.equals("1"))
		{
			execution.setVariable("esitoVerificaRequisiti", "inviaRisultato");
			execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", dittaCorrente.get("pIvaCodiceFiscaleDittaCandidata"));
			execution.setVariable("ragioneSocialeDittaAggiudicataria", dittaCorrente.get("ragioneSocialeDittaCandidata"));
			execution.setVariable("gestioneRTIDittaAggiudicataria", dittaCorrente.get("gestioneRTIDittaCandidata"));
		} else if (nrTotaleDitte <= (int) execution.getVariable("nrElencoDitteCorrente")) 
		{
			execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", "NESSUNA");
			execution.setVariable("ragioneSocialeDittaAggiudicataria", "NESSUNA");
			execution.setVariable("ditteRTI_json", null);
			execution.setVariable("esitoVerificaRequisiti", "revoca");
		} else {
			dittaCorrente = ditteCandidate.getJSONObject(nrElencoDitteCorrente);
			execution.setVariable("ditteRTI_json", null);
			execution.setVariable("pIvaCodiceFiscaleDittaAggiudicataria", dittaCorrente.get("pIvaCodiceFiscaleDittaCandidata"));
			execution.setVariable("ragioneSocialeDittaAggiudicataria", dittaCorrente.get("ragioneSocialeDittaCandidata"));
			execution.setVariable("gestioneRTIDittaAggiudicataria", dittaCorrente.get("gestioneRTIDittaCandidata"));
			execution.setVariable("esitoVerificaRequisiti", "procediAltroCandidato");
			execution.setVariable("verificheRequisiti", "da verificare");
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
        attachmentService.saveAttachment(execution, nomeFileDaSostituire, originale);

        LOGGER.debug("Salvo una copia per futuro riferimento");
        copia.setAzione(Sostituzione);
        copia.addStato(Sostituito);
        copia.setName("Provvedimento di Aggiudicazione Sostiutito");
        // TODO il nome "provvedimentiRespinti" dovrebbe sempre essere un Expression
        attachmentService.saveAttachmentInArray(execution, "provvedimentiRespinti", copia);
    }
}
