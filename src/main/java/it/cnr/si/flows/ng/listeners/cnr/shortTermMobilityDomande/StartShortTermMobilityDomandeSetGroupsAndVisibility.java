package it.cnr.si.flows.ng.listeners.cnr.shortTermMobilityDomande;



import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsTimerService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;

import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
import org.h2.util.New;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

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
@Profile("!oiv")

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
		Date newTimerDate = sdf.parse(scadenzaPresentazioneDomande); 
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
		
		
		String proponente = execution.getVariable("userNameProponente", String.class);
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		//Integer cdsuoAppartenenzaUtente = aceBridgeService.getEntitaOrganizzativaDellUtente(proponente.toString()).getId();
		String cdsuoAppartenenzaUtente = null;
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(proponente.toString()).getCdsuo();
		} catch(UnexpectedResultException | FeignException e) {
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(proponente.toString()).get("codice_uo").toString();
		}
		finally {
			LOGGER.debug("getDirettoreCDSUO  FUNZIONA ");
			Object insdipResponsabileUo = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("codice_sede");
			String usernameDirettore = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
			EntitaOrganizzativaWebDto entitaOrganizzativaDirUo = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0);
			Integer idEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getId();
			String siglaEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getSigla().toString();
			String denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getDenominazione().toString();
			String cdsuoEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getCdsuo().toString();
			String idnsipEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getIdnsip().toString();			
			LOGGER.info("L'utente {} ha come direttore {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", proponente.toString(), usernameDirettore, denominazioneEntitaorganizzativaResponsabileUtente, siglaEntitaorganizzativaResponsabileUtente, idEntitaorganizzativaResponsabileUtente, cdsuoEntitaorganizzativaResponsabileUtente, idnsipEntitaorganizzativaResponsabileUtente);
			
			String gruppoValidatoriShortTermMobility = "validatoriShortTermMobility@0000";
			String gruppoUfficioProtocollo = "ufficioProtocolloShortTermMobility@0000";
			String gruppoValutatoreScientificoSTMDipartimento = "valutatoreScientificoSTMDipartimento@0000";
			String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
			//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
			String gruppoDirigenteProponente = "responsabile-struttura@" + idEntitaorganizzativaResponsabileUtente;

			String applicazioneSTM = "app.abil";
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

			execution.setVariable("strutturaValutazioneDirigente", idEntitaorganizzativaResponsabileUtente + "-" + denominazioneEntitaorganizzativaResponsabileUtente);
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