package it.cnr.si.flows.ng.listeners.cnr.covid19;



import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsTimerService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.scritture.TipoEntitaOrganizzativaDto;
import it.cnr.si.service.dto.anagrafica.scritture.UtenteDto;

import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
import org.h2.util.New;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import feign.FeignException;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")

@Service
public class StartCovid19SetGroupsAndVisibility_v1 {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartCovid19SetGroupsAndVisibility_v1.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private AceService aceService;
	@Inject
	private SiperService siperService;
	@Inject
	private ManagementService managementService;
	@Inject
	private FlowsTimerService flowsTimerService;	

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());		
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		//Integer cdsuoAppartenenzaUtente = aceBridgeService.getEntitaOrganizzativaDellUtente(proponente.toString()).getId();
		String cdsuoAppartenenzaUtente = null;
		String usernameDirettoreAce = null;
		BossDto direttoreAce = null;
		Integer IdEntitaOrganizzativaDirettore = 0;
		// VERIFICA AFFERENZA
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(initiator.toString()).getCdsuo();
		} catch(UnexpectedResultException | FeignException e) {
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(initiator.toString()).get("codice_uo").toString();
			throw new BpmnError("412", "l'utenza: " + initiator + " non risulta associata ad alcuna struttura<br>");

		}
		// VERIFICA DIRETTORE
		String usernameDirettoreSiper = "";
		Integer tipologiaStrutturaUtente = aceBridgeService.getAfferenzaUtente(initiator.toString()).getTipo().getId();

		if((tipologiaStrutturaUtente != null) && ((tipologiaStrutturaUtente == 1) || (tipologiaStrutturaUtente == 21) || (tipologiaStrutturaUtente == 23) | (tipologiaStrutturaUtente == 24))) {
			try {
				direttoreAce = aceService.bossDirettoreByUsername(initiator);
				usernameDirettoreAce = direttoreAce.getUsername();
				IdEntitaOrganizzativaDirettore = direttoreAce.getIdEntitaOrganizzativa();
			} catch(UnexpectedResultException | FeignException e) {
				cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(initiator.toString()).get("codice_uo").toString();
				throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + initiator + " <br>Si prega di contattare l'help desk in merito<br>");
			}

		} else {

			try {
				direttoreAce = aceService.bossLevelByUsername(0,initiator);
				usernameDirettoreAce = direttoreAce.getUsername();
				IdEntitaOrganizzativaDirettore = direttoreAce.getIdEntitaOrganizzativa();
			} catch(UnexpectedResultException | FeignException e) {
				cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(initiator.toString()).get("codice_uo").toString();
				throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + initiator + " <br>Si prega di contattare l'help desk in merito<br>");
			}
		}

		try {
			usernameDirettoreSiper = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();

		} catch(UnexpectedResultException | FeignException | HttpClientErrorException e) {
			usernameDirettoreSiper = "not found";
		}
		finally {

			//CONFRONTO DIRETTORE SIPER CON DIRETTORE ACE
			if (!usernameDirettoreAce.equals(usernameDirettoreSiper)) {
				LOGGER.info("--- WARNING MISMATCH DIRETTORE - L'utente {} ha  {} come direttore in ACE e {} come come direttore in SIPER", initiator.toString(), usernameDirettoreAce, usernameDirettoreSiper);
			}

			LOGGER.info("L'utente {} ha come  {} per la struttura {} ({}} - id:{}", initiator.toString(), direttoreAce.getSiglaRuolo(), usernameDirettoreAce, direttoreAce.getDenominazioneEO(), direttoreAce.getSiglaEO(), IdEntitaOrganizzativaDirettore);

			String gruppoResponsabileProponente = "responsabile-struttura@" + IdEntitaOrganizzativaDirettore;

			String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";


			EntitaOrganizzativaWebDto utenteAce = aceBridgeService.getAfferenzaUtente(execution.getVariable("initiator").toString());
			UtenteDto utente = aceService.getUtente(execution.getVariable("initiator").toString());

			execution.setVariable("matricola", utente.getPersona().getMatricola());
			execution.setVariable("nomeCognomeUtente", utente.getPersona().getNome() + " " + utente.getPersona().getCognome());
			execution.setVariable("tipoContratto", utente.getPersona().getTipoContratto());
			execution.setVariable("cds", utenteAce.getCdsuo());
			execution.setVariable("direttore", direttoreAce.getNome() + " " +  direttoreAce.getCognome());


			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileProponente, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);


			execution.setVariable("gruppoResponsabileProponente", gruppoResponsabileProponente);
			execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		}
	}
}