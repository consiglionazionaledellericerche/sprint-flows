package it.cnr.si.flows.ng.listeners.cnr.smartWorkingDomanda;



import it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsTimerService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.dto.anagrafica.enums.TipoAppartenenza;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;

import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")

@Service
public class StartSmartWorkingDomandaSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartSmartWorkingDomandaSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private MembershipService membershipService;
	@Inject
	private AceService aceService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private ManagementService managementService;
	@Inject
	private FlowsTimerService flowsTimerService;	

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		//SET TIMER
		//		LOGGER.debug("scadenzaPresentazioneDomande {}",  execution.getVariable("scadenzaPresentazioneDomande").toString());
		String scadenzaPresentazioneDomande = execution.getVariable("scadenzaPresentazioneDomande", String.class);
		execution.setVariable("statoFinaleDomanda",  Enum.StatoDomandeSmartWorkingEnum.APERTA.toString());


		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date currentTimerDate = sdf.parse(scadenzaPresentazioneDomande); 
		Date newTimerDate = sdf.parse(scadenzaPresentazioneDomande); 
		if (execution.getVariable("idDomanda") != null) {
			Long sec = Long.parseLong(execution.getVariable("idDomanda").toString());
			sec = sec%200;
			newTimerDate =  Date.from(currentTimerDate.toInstant().plusSeconds(sec));
			execution.setVariable("scadenzaPresentazioneDomande",  newTimerDate);
		}
		String timerId = "timerChiusuraBando";

		List<Job> jobTimerChiusuraBando = flowsTimerService.getTimer(execution.getProcessInstanceId(),timerId);
		if(jobTimerChiusuraBando.size() > 0){
			LOGGER.info("------ DATA: {} per timer: {} " + jobTimerChiusuraBando.get(0).getDuedate(), timerId);
			managementService.executeCommand(new SetTimerDuedateCmd(jobTimerChiusuraBando.get(0).getId(), newTimerDate));
		} else {
			LOGGER.info("------ " + timerId + ": TIMER SCADUTO: ");	
		}

		String userNameProponente = execution.getVariable("userNameProponente", String.class);
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", userNameProponente, execution.getId(), execution.getVariable("title"));
		String cdsuoAppartenenzaUtente = null;
		Integer IdEntitaOrganizzativaDirettore = 0;
		SimpleEntitaOrganizzativaWebDto entitaOrganizzativaDirettore = null;
		LocalDate dateRif = LocalDate.now();
		BossDto responsabileStruttura = null;

		// VERIFICA PROFILO RICHIEDENTE
		String profiloDomanda = "NON_AMMESSO";
		String profiloRichiedente = aceService.getPersonaByUsername(userNameProponente).getLivello();
		// PROFILO RICHIEDENTE IV-VIII
		if(profiloRichiedente.contains("IV livello")
				|| profiloRichiedente.contains("V livello")
				|| profiloRichiedente.contains("VI livello")
				|| profiloRichiedente.contains("VII livello")
				|| profiloRichiedente.contains("VIII livello")
				) {profiloDomanda = "IV-VIII";}

		// PROFILO RICHIEDENTE I-III			
		if(profiloRichiedente.contains("I livello")
				|| profiloRichiedente.contains("II livello")
				|| profiloRichiedente.contains("III livello")
				) {profiloDomanda = "I-III";}

		// PROFILO RICHIEDENTE RESPONSABILE			
		Object[] ruoliRichiedente = membershipService.getAllRolesForUser(userNameProponente).toArray();
		if (Arrays.asList(ruoliRichiedente).contains("responsabile-struttura")) {
			profiloDomanda = "RESPONSABILE";
		}

		// DETERMINA PERCORSO FLUSSO
		String profiloFlusso = "Indefinito";
		if(profiloDomanda.equals("RESPONSABILE") || profiloDomanda.equals("I-III")) {
			profiloFlusso = "PresaVisione";
		} 
		if(profiloDomanda.equals("IV-VIII") ) {
			profiloFlusso = "Validazione";
		} 		

		// VERIFICA RESPONSABILE
		if(profiloDomanda.equals("RESPONSABILE") ) {
			String idSedeDirettoregenerale = aceService.getSedeIdByIdNsip("630000");
			IdEntitaOrganizzativaDirettore = Integer.parseInt(idSedeDirettoregenerale);
		} else {
			try {
				responsabileStruttura = aceService.findResponsabileStruttura(userNameProponente, dateRif, TipoAppartenenza.SEDE, "responsabile-struttura");
				if (responsabileStruttura.getUtente()== null) {
					throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + userNameProponente + " <br>Si prega di contattare l'help desk in merito<br>");
				} else {
				}
				if (responsabileStruttura.getEntitaOrganizzativa().getId()== null) {
					throw new BpmnError("412", "l'utenza: " + userNameProponente + " non risulta associata ad alcuna struttura<br>");
				} else {
					IdEntitaOrganizzativaDirettore = responsabileStruttura.getEntitaOrganizzativa().getId();
					entitaOrganizzativaDirettore = aceService.entitaOrganizzativaById(IdEntitaOrganizzativaDirettore);
					cdsuoAppartenenzaUtente = entitaOrganizzativaDirettore.getCdsuo();
				}

			} catch ( FeignException  e) {
				throw new BpmnError("412", "Errore nell'avvio del flusso " + e.getMessage().toString());
			}
			LOGGER.info("L'utente {} ha come responsabile-struttura [{}] (per SEDE) {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameProponente, responsabileStruttura.getRuolo().getDescr(), responsabileStruttura.getUtente().getUsername(), entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());
		}

		String gruppoValidatoriLaboratoriCongiunti = "validatoriLaboratoriCongiunti@0000";
		String gruppoUfficioProtocollo = "ufficioProtocolloLaboratoriCongiunti@0000";
		String gruppoValutatoreScientificoLABDipartimento = "valutatoreScientificoLABDipartimento@0000";
		String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
		//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
		String gruppoDirigenteProponente = "responsabile-struttura@" + IdEntitaOrganizzativaDirettore;

		String applicazioneLaboratoriCongiunti = "app.siper";
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriLaboratoriCongiunti, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);
		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriLaboratoriCongiunti, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriLaboratoriCongiunti, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneLaboratoriCongiunti, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirigenteProponente, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoLABDipartimento, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);


		execution.setVariable("profiloRichiedente", profiloRichiedente);
		execution.setVariable("profiloDomanda", profiloDomanda);
		execution.setVariable("profiloFlusso", profiloFlusso);
		
		execution.setVariable("strutturaValutazioneDirigente", IdEntitaOrganizzativaDirettore + "-" + entitaOrganizzativaDirettore.getDenominazione());
		execution.setVariable("gruppoValidatoriLaboratoriCongiunti", gruppoValidatoriLaboratoriCongiunti);
		execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
		execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
		execution.setVariable("applicazioneLaboratoriCongiunti", applicazioneLaboratoriCongiunti);
		execution.setVariable("gruppoDirigenteProponente", gruppoDirigenteProponente);
		execution.setVariable("gruppoValutatoreScientificoLABDipartimento", gruppoValutatoreScientificoLABDipartimento);
		execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		execution.setVariable("cdsuoProponente", cdsuoAppartenenzaUtente);
		execution.setVariable("idStruttura", String.valueOf(IdEntitaOrganizzativaDirettore));
	}
}