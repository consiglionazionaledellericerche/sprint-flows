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
import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;

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
		String cdsuoDirettore = null;
		String idnsipAppartenenzaUtente = null;
		Integer IdEntitaOrganizzativaDirettore = 0;
		SimpleEntitaOrganizzativaWebDto entitaOrganizzativaDirettore = null;
		LocalDate dateRif = LocalDate.now();
		BossDto responsabileStruttura = null;

		// VERIFICA PROFILO RICHIEDENTE
		String profiloDomanda = "NON_AMMESSO";
		SimplePersonaWebDto personaProponente = aceService.getPersonaByUsername(userNameProponente);
		
		String livelloRichiedente = personaProponente.getLivello();
		String profiloRichiedente = personaProponente.getProfilo();
		String nomeProponente =  personaProponente.getNome().toString();
		String cognomeProponente =  personaProponente.getCognome().toString();
		String matricolaRichiedente =  personaProponente.getMatricola().toString();
		String idNsipRichiedente =  personaProponente.getSede().getIdnsip();
		String sedeRichiedente =  idNsipRichiedente + " - " + personaProponente.getSede().getDenominazione();
		execution.setVariable("livelloRichiedente", livelloRichiedente);
		execution.setVariable("profiloRichiedente", profiloRichiedente);
		execution.setVariable("matricolaRichiedente", matricolaRichiedente);
		execution.setVariable("idNsipRichiedente", idNsipRichiedente);
		execution.setVariable("sedeRichiedente", sedeRichiedente);
		execution.setVariable("nomeCognomeUtente", nomeProponente + " " + cognomeProponente);

		// PROFILO RICHIEDENTE collaboratore
		if(livelloRichiedente.equals("04")
				|| livelloRichiedente.equals("05")
				|| livelloRichiedente.equals("06")
				|| livelloRichiedente.equals("07")
				|| livelloRichiedente.equals("08")
				) {profiloDomanda = "collaboratore";}
		else {

			// PROFILO RICHIEDENTE ricercatore-tecnologo			
			if(livelloRichiedente.equals("01")
					|| livelloRichiedente.equals("02")
					|| livelloRichiedente.equals("03")
					) {profiloDomanda = "ricercatore-tecnologo";}
			else {
				profiloDomanda = "direttore-responsabile";
			}
		}

		// PROFILO RICHIEDENTE direttore-responsabile			
		//		Object[] ruoliRichiedente = membershipService.getAllRolesForUser(userNameProponente).toArray();
		//		if (Arrays.asList(ruoliRichiedente).contains("responsabile-struttura")) {
		//			profiloDomanda = "direttore-responsabile";
		//		}

		// DETERMINA PERCORSO FLUSSO
		String profiloFlusso = "Indefinito";
		if(profiloDomanda.equals("direttore-responsabile") || profiloDomanda.equals("ricercatore-tecnologo")) {
			profiloFlusso = "PresaVisione";
		} 
		if(profiloDomanda.equals("collaboratore") ) {
			profiloFlusso = "Validazione";
		} 		

		// VERIFICA direttore-responsabile
		if(profiloDomanda.equals("direttore-responsabile") ) {
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
				}

			} catch ( FeignException  e) {
				throw new BpmnError("412", "Errore nell'avvio del flusso " + e.getMessage().toString());
			}
		}

		entitaOrganizzativaDirettore = aceService.entitaOrganizzativaById(IdEntitaOrganizzativaDirettore);
		cdsuoDirettore = entitaOrganizzativaDirettore.getCdsuo();
		idnsipAppartenenzaUtente = entitaOrganizzativaDirettore.getIdnsip();
		if(profiloDomanda.equals("direttore-responsabile") ) {
			LOGGER.info("L'utente {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameProponente, entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());
		} else {
			LOGGER.info("L'utente {} ha come responsabile-struttura [{}] (per SEDE) {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameProponente, responsabileStruttura.getRuolo().getDescr(), responsabileStruttura.getUtente().getUsername(), entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());
		}
		//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
		String gruppoDirigenteProponente = "responsabile-struttura@" + IdEntitaOrganizzativaDirettore;

		String applicazioneSiper = "app.siper";
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneSiper, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirigenteProponente, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);


		execution.setVariable("livelloRichiedente", livelloRichiedente);
		execution.setVariable("profiloDomanda", profiloDomanda);
		execution.setVariable("profiloFlusso", profiloFlusso);

		execution.setVariable("strutturaValutazioneDirigente", IdEntitaOrganizzativaDirettore + "-" + entitaOrganizzativaDirettore.getDenominazione());
		execution.setVariable("applicazioneSiper", applicazioneSiper);
		execution.setVariable("gruppoDirigenteProponente", gruppoDirigenteProponente);
		execution.setVariable("idnsipAppartenenzaUtente", idnsipAppartenenzaUtente);
		execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		execution.setVariable("cdsuoDirettore", cdsuoDirettore);
		execution.setVariable("idStruttura", String.valueOf(IdEntitaOrganizzativaDirettore));
	}
}