package it.cnr.si.flows.ng.listeners.cnr.smartWorkingRevoca;



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

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
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
public class StartSmartWorkingRevocaSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartSmartWorkingRevocaSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private HistoryService historyService;	
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




		String userNameDomanda = execution.getVariable("userNameDomanda", String.class);
		// idNsipRichiedente VARIABILE CHE CONTIENE L'IDNSIP DI APPARTENENZA DICHIARATO DALL'UTENTE
		String idNsipRichiedente = execution.getVariable("idNsipRichiedente", String.class);
		String idAceStrutturaAppartenenzaRichiedente = aceService.getSedeIdByIdNsip(idNsipRichiedente);
		String tipologiaRichiedente = execution.getVariable("tipologiaRichiedente", String.class);
		String idDomanda = execution.getVariable("idDomanda", String.class);

		List<HistoricProcessInstance> processinstancesListaDomandeSmartWorking = historyService.createHistoricProcessInstanceQuery()
				.includeProcessVariables()
				.variableValueEquals("idDomanda", idDomanda)
				.processDefinitionKey("smart-working-domanda")
				.list();

		if (processinstancesListaDomandeSmartWorking.size() != 1) {
			throw new BpmnError("412", "non risulta presente alcuna domanda per idDomanda: " + idDomanda + "  <br>");

		}
		String linkToOtherWorkflows = processinstancesListaDomandeSmartWorking.get(0).getId();


		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", userNameDomanda, execution.getId(), execution.getVariable("title"));
		String cdsuoDirettore = null;
		SimpleEntitaOrganizzativaWebDto entitaOrganizzativaDirettore = null;
		LocalDate dateRif = LocalDate.now();
		BossDto responsabileStruttura = null;

		// VERIFICA PROFILO RICHIEDENTE
		String profiloDomanda = "NON_AMMESSO";
		SimplePersonaWebDto personaProponente = aceService.getPersonaByUsername(userNameDomanda);

		String livelloRichiedente = personaProponente.getLivello();
		String profiloRichiedente = personaProponente.getProfilo();
		String nomeProponente =  personaProponente.getNome().toString();
		String cognomeProponente =  personaProponente.getCognome().toString();
		String matricolaRichiedente =  personaProponente.getMatricola().toString();

		execution.setVariable("linkToOtherWorkflows", linkToOtherWorkflows);
		execution.setVariable("livelloRichiedente", livelloRichiedente);
		execution.setVariable("profiloRichiedente", profiloRichiedente);
		execution.setVariable("matricolaRichiedente", matricolaRichiedente);
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

		// idAceStrutturaDomandaRichiedente VARIABILE CHE CONTIENE L'ID EO DEL DIRETTORE E DELLA SEGRETERIA
		String idAceStrutturaDomandaRichiedente;
		
		// VERIFICA direttore-responsabile
		if(profiloDomanda.equals("direttore-responsabile") ) {
			String idSedeDirettoregenerale = aceService.getSedeIdByIdNsip("630000");
			idAceStrutturaDomandaRichiedente = idSedeDirettoregenerale;
		} else {
			try {
				responsabileStruttura = aceService.findResponsabileStruttura(userNameDomanda, dateRif, TipoAppartenenza.SEDE, "responsabile-struttura");
				if (responsabileStruttura.getUtente()== null) {
					throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + userNameDomanda + " <br>Si prega di contattare l'help desk in merito<br>");
				} else {
				}
				if (responsabileStruttura.getEntitaOrganizzativa().getId()== null) {
					throw new BpmnError("412", "l'utenza: " + userNameDomanda + " non risulta associata ad alcuna struttura<br>");
				} else {
					idAceStrutturaDomandaRichiedente = responsabileStruttura.getEntitaOrganizzativa().getId().toString();
				}

			} catch ( FeignException  e) {
				throw new BpmnError("412", "Errore nell'avvio del flusso " + e.getMessage().toString());
			}
		}

		entitaOrganizzativaDirettore = aceService.entitaOrganizzativaById(Integer.parseInt(idAceStrutturaDomandaRichiedente));
		cdsuoDirettore = entitaOrganizzativaDirettore.getCdsuo();
		String idnsipDirettore = entitaOrganizzativaDirettore.getIdnsip();
		if(profiloDomanda.equals("direttore-responsabile") ) {
			LOGGER.info("L'utente {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameDomanda, entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());
		} else {
			LOGGER.info("L'utente {} ha come responsabile-struttura [{}] (per SEDE) {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameDomanda, responsabileStruttura.getRuolo().getDescr(), responsabileStruttura.getUtente().getUsername(), entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());
		}

		String gruppoPresaVisione = "responsabile-struttura@" + idAceStrutturaDomandaRichiedente;	
		String gruppoResponsabileSegreteria = "rs@" + idAceStrutturaDomandaRichiedente;	
		// DETERMINA PERCORSO FLUSSO
		if(tipologiaRichiedente.equals("direttore-responsabile")) {
			gruppoPresaVisione = "rs@" + idAceStrutturaDomandaRichiedente;	
		} 


		String applicazioneSiper = "app.siper";
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneSiper, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoPresaVisione, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileSegreteria, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);


		execution.setVariable("livelloRichiedente", livelloRichiedente);
		execution.setVariable("profiloDomanda", profiloDomanda);
		execution.setVariable("gruppoPresaVisione", gruppoPresaVisione);
		execution.setVariable("applicazioneSiper", applicazioneSiper);
		execution.setVariable("idAceStrutturaDomandaRichiedente", idAceStrutturaDomandaRichiedente);
		execution.setVariable("idNsipRichiedente", idNsipRichiedente);
		execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		execution.setVariable("cdsuoDirettore", cdsuoDirettore);
		execution.setVariable("idStruttura", String.valueOf(idAceStrutturaDomandaRichiedente));
	}
}