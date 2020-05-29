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
import org.springframework.web.client.HttpServerErrorException;

import feign.FeignException;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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
		//String cdsuoAppartenenzaUtente = null;
		String idnsipAppartenenzaUtente = null;
		String cdsuoAppartenenzaUtente = null;
		String usernameDirettoreAce = null;
		BossDto direttoreAce = null;
		Integer IdEntitaOrganizzativaDirettore = 0;
		String denominazioneEO  = null;


		// VERIFICA DIRETTORE
		String usernameDirettoreSiper = "";
		try {
			//VERIFICA DIPENDENTI CESSATI
			if (aceService.getPersonaByUsername(initiator.toString()).getDataCessazione() != null) {			
				LocalDate dateRif = LocalDate.of(Integer.parseInt(execution.getVariable("anno").toString()), Integer.parseInt(execution.getVariable("meseNumerico").toString()), 1);
				if (aceService.getPersonaByUsername(initiator.toString()).getDataCessazione().minusDays(1).isBefore(dateRif)) {
					throw new BpmnError("416", "l'utenza: " + initiator + " non risulta associata <br>ad alcuna struttura per il periodo di riferimento<br>");
				} else {
					direttoreAce = aceService.bossFirmatarioByUsername(initiator, dateRif);
					idnsipAppartenenzaUtente = aceService.entitaOrganizzativaById(direttoreAce.getIdEntitaOrganizzativa()).getIdnsip();
					cdsuoAppartenenzaUtente = aceService.entitaOrganizzativaById(direttoreAce.getIdEntitaOrganizzativa()).getCdsuo();
				}
			} else {
				direttoreAce = aceService.bossFirmatarioByUsername(initiator);
				// VERIFICA AFFERENZA
				try {
					//cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(initiator.toString()).getCdsuo();
					idnsipAppartenenzaUtente = aceBridgeService.getAfferenzaUtentePerSede(initiator.toString()).getIdnsip();
					cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtentePerSede(initiator.toString()).getCdsuo();
				} catch(UnexpectedResultException | FeignException e) {
					//cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(initiator.toString()).get("codice_uo").toString();
					idnsipAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(initiator.toString()).get("codice_sede").toString();
					throw new BpmnError("412", "l'utenza: " + initiator + " non risulta associata ad alcuna struttura<br>");
				}
			}
			//direttoreAce = aceService.bossDirettoreByUsername(initiator);
			usernameDirettoreAce = direttoreAce.getUsername();
			IdEntitaOrganizzativaDirettore = direttoreAce.getIdEntitaOrganizzativa();
			denominazioneEO = direttoreAce.getDenominazioneEO();
		} catch(UnexpectedResultException | FeignException e) {
			//cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(initiator.toString()).get("codice_uo").toString();
			idnsipAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(initiator.toString()).get("codice_sede").toString();
			throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + initiator + " <br>Si prega di contattare l'help desk in merito<br>");
		}

		try {
			//usernameDirettoreSiper = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
			usernameDirettoreSiper = siperService.getDirettoreIDNSIP(idnsipAppartenenzaUtente).get(0).get("uid").toString();

		} catch(UnexpectedResultException | FeignException | HttpClientErrorException | HttpServerErrorException e) {
			//	} catch(UnexpectedResultException | FeignException | HttpClientErrorException e) {
			usernameDirettoreSiper = "not found";
		}
		finally {

			//CONFRONTO DIRETTORE SIPER CON DIRETTORE ACE
			if (!usernameDirettoreAce.equals(usernameDirettoreSiper)) {
				LOGGER.info("--- WARNING MISMATCH DIRETTORE - L'utente {} ha  {} come direttore in ACE e {} come come direttore in SIPER per idNsip = {}", initiator.toString(), usernameDirettoreAce, usernameDirettoreSiper, idnsipAppartenenzaUtente);
			}

			LOGGER.info("L'utente {} ha come  {} per la struttura {} ({}} - id:{}", initiator.toString(), direttoreAce.getSiglaRuolo(), usernameDirettoreAce, direttoreAce.getDenominazioneEO(), direttoreAce.getSiglaEO(), IdEntitaOrganizzativaDirettore);

			String gruppoResponsabileProponente = "responsabile-struttura@" + IdEntitaOrganizzativaDirettore;

			String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";


			//EntitaOrganizzativaWebDto utenteAce = aceBridgeService.getAfferenzaUtentePerSede(execution.getVariable("initiator").toString());
			UtenteDto utente = aceService.getUtente(execution.getVariable("initiator").toString());

			execution.setVariable("matricola", utente.getPersona().getMatricola());
			execution.setVariable("nomeCognomeUtente", utente.getPersona().getNome() + " " + utente.getPersona().getCognome());
			execution.setVariable("userNameUtente", utente.getUsername());
			execution.setVariable("tipoContratto", utente.getPersona().getTipoContratto());
			execution.setVariable("cds", cdsuoAppartenenzaUtente);
			execution.setVariable("idnsip", idnsipAppartenenzaUtente);
			execution.setVariable("direttore", direttoreAce.getNome() + " " +  direttoreAce.getCognome());
			execution.setVariable("denominazioneEO", denominazioneEO);


			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileProponente, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);


			execution.setVariable("gruppoResponsabileProponente", gruppoResponsabileProponente);
			execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		}
	}
}