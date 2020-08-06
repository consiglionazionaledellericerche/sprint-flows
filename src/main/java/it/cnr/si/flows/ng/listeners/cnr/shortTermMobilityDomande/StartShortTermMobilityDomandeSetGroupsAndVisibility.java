package it.cnr.si.flows.ng.listeners.cnr.shortTermMobilityDomande;



import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsTimerService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;

import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
import org.h2.util.New;
import org.joda.time.DateTime;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")

@Service
public class StartShortTermMobilityDomandeSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartShortTermMobilityDomandeSetGroupsAndVisibility.class);

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
		//SET TIMER
		//		LOGGER.debug("scadenzaPresentazioneDomande {}",  execution.getVariable("scadenzaPresentazioneDomande").toString());
		String scadenzaPresentazioneDomande = execution.getVariable("scadenzaPresentazioneDomande", String.class);
		execution.setVariable("statoFinaleDomanda",  Enum.StatoDomandeSTMEnum.APERTA.toString());


		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date currentTimerDate = sdf.parse(scadenzaPresentazioneDomande); 
		Date newTimerDate = sdf.parse(scadenzaPresentazioneDomande); 
		if (execution.getVariable("idDomanda") != null) {
			Long sec = Long.parseLong(execution.getVariable("idDomanda").toString());
			sec = sec%200;
			newTimerDate =  Date.from(currentTimerDate.toInstant().plusSeconds(sec));
			execution.setVariable("scadenzaPresentazioneDomande",  newTimerDate);
		}
		////SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  
		//Date newTimerDate = formatter.parse(execution.getVariable("scadenzaPresentazioneDomande").toString().substring(0, 19));
		//LocalDateTime newTimerLocalDate = LocalDateTime.parse( execution.getVariable("scadenzaPresentazioneDomande").toString() ) ;
		//Date newTimerDate = Date.from(newTimerLocalDate.atZone(ZoneId.systemDefault()).toInstant());

		//2011-03-11T12:13:14
		//2019-09-21T01:12:00.000Z
		String timerId = "timerChiusuraBando";

		List<Job> jobTimerChiusuraBando = flowsTimerService.getTimer(execution.getProcessInstanceId(),timerId);
		if(jobTimerChiusuraBando.size() > 0){
			LOGGER.info("------ DATA: {} per timer: {} " + jobTimerChiusuraBando.get(0).getDuedate(), timerId);
			managementService.executeCommand(new SetTimerDuedateCmd(jobTimerChiusuraBando.get(0).getId(), newTimerDate));
		} else {
			LOGGER.info("------ " + timerId + ": TIMER SCADUTO: ");	
		}

		String userNameProponente = execution.getVariable("userNameProponente", String.class);
		String cdsuoAppartenenzaUtente = null;
		String usernameDirettoreAce = null;
		String denominazioneEODirettore = null;
		String denominazioneEOProponente = null;
		BossDto direttoreAce = null;
		Integer idEntitaOrganizzativaDirettore = 0;
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", userNameProponente, execution.getId(), execution.getVariable("title"));

		// VERIFICA AFFERENZA
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(userNameProponente).getCdsuo();
		} catch(UnexpectedResultException | FeignException e) {
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(userNameProponente).get("codice_uo").toString();
			throw new BpmnError("412", "l'utenza: " + userNameProponente + " non risulta associata ad alcuna struttura<br>");

		}
		// VERIFICA DIRETTORE
		String usernameDirettoreSiper = "";
		try {
			//direttoreAce = aceService.bossFirmatarioByUsername(userNameProponente);
			direttoreAce = aceService.bossFirmatarioUoByUsername(userNameProponente);
			usernameDirettoreAce = direttoreAce.getUsername();
			idEntitaOrganizzativaDirettore = direttoreAce.getIdEntitaOrganizzativa();
			//CHECK CORRISPONDENZA EO TRA DICHIARATO UTENTE E ACE
			denominazioneEODirettore = direttoreAce.getDenominazioneEO();
			if (!execution.getVariable("istitutoProponente").toString().equals("SEDE CENTRALE - DIPARTIMENTO")){
				denominazioneEOProponente = execution.getVariable("istitutoProponente").toString().substring(9);
				if (!denominazioneEODirettore.equalsIgnoreCase(denominazioneEOProponente)) {
					throw new BpmnError("400", "La struttura dichiarata dall'utente: " + userNameProponente + ": <br>" 
							+ denominazioneEOProponente
							+ "<br>non coincide con quella di afferenza amministrativa"
							+ "<br>presente in anagrafica:<br>" + denominazioneEODirettore
							+ "<br>contattare l'help desk in merito<br>");
				}
			}
		} catch(UnexpectedResultException | FeignException e) {
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(userNameProponente).get("codice_uo").toString();
			throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + userNameProponente + " <br>Si prega di contattare l'help desk in merito<br>");
		}
		try {
			usernameDirettoreSiper = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
		} catch(UnexpectedResultException | FeignException | HttpClientErrorException | HttpServerErrorException e) {
			usernameDirettoreSiper = "not found";
		}
		finally {
			//CONFRONTO DIRETTORE SIPER CON DIRETTORE ACE
			if (!usernameDirettoreAce.equals(usernameDirettoreSiper)) {
				LOGGER.info("--- WARNING MISMATCH DIRETTORE - L'utente {} ha  {} come direttore in ACE e {} come come direttore in SIPER", userNameProponente, usernameDirettoreAce, usernameDirettoreSiper);
			}
			String gruppoValidatoriShortTermMobility = "validatoriShortTermMobility@0000";
			String gruppoUfficioProtocollo = "ufficioProtocolloShortTermMobility@0000";
			String gruppoValutatoreScientificoSTMDipartimento = "valutatoreScientificoSTMDipartimento@0000";
			String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
			//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
			String gruppoDirigenteProponente = "responsabile-struttura@" + idEntitaOrganizzativaDirettore;

			String applicazioneSTM = "app.stm";
			String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

			LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriShortTermMobility, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);
			LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriShortTermMobility, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriShortTermMobility, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneSTM, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirigenteProponente, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoSTMDipartimento, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);

			execution.setVariable("strutturaValutazioneDirigente", idEntitaOrganizzativaDirettore + "-" + direttoreAce.getDenominazioneEO());
			execution.setVariable("gruppoValidatoriShortTermMobility", gruppoValidatoriShortTermMobility);
			execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
			execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
			execution.setVariable("applicazioneSTM", applicazioneSTM);
			execution.setVariable("gruppoDirigenteProponente", gruppoDirigenteProponente);
			execution.setVariable("gruppoValutatoreScientificoSTMDipartimento", gruppoValutatoreScientificoSTMDipartimento);
			execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
			execution.setVariable("cdsuoProponente", cdsuoAppartenenzaUtente);
		}
	}
}