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


		execution.setVariable("statoFinaleDomanda",  Enum.StatoDomandeSmartWorkingEnum.APERTA.toString());
		String userNameProponente = execution.getVariable("userNameProponente", String.class);

		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", userNameProponente, execution.getId(), execution.getVariable("title"));
		// idAceStrutturaDomandaRichiedente VARIABILE CHE CONTIENE L'ID EO DEL DIRETTORE E DELLA SEGRETERIA
		Integer idAceStrutturaDomandaRichiedente = 0;
		SimpleEntitaOrganizzativaWebDto entitaOrganizzativaDirettore = null;
		LocalDate dateRif = LocalDate.now();
		BossDto responsabileStruttura = null;
		
		//DATI STRUTTURA DICHIARATA RICHIEDENTE
		String idNsipRichiedente =  execution.getVariable("idNsipRichiedente", String.class);
		String idAceStrutturaRichiedente = aceService.getSedeIdByIdNsip(idNsipRichiedente);
		SimpleEntitaOrganizzativaWebDto sedeRichiedente = aceService.entitaOrganizzativaById(Integer.parseInt(idAceStrutturaRichiedente));
		String cdsuoStrutturaRichiedente = sedeRichiedente.getCdsuo();
		execution.setVariable("idNsipRichiedente", idNsipRichiedente);
		execution.setVariable("cdsuoStrutturaRichiedente", cdsuoStrutturaRichiedente);
		execution.setVariable("idAceStrutturaRichiedente", idAceStrutturaRichiedente);

		// VERIFICA PROFILO RICHIEDENTE
		String profiloDomanda = "NON_AMMESSO";
		SimplePersonaWebDto personaProponente = aceService.getPersonaByUsername(userNameProponente);
		String livelloRichiedente = personaProponente.getLivello();
		String profiloRichiedente = personaProponente.getProfilo();
		String nomeProponente =  personaProponente.getNome().toString();
		String cognomeProponente =  personaProponente.getCognome().toString();
		String matricolaRichiedente =  personaProponente.getMatricola().toString();
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

		// PROFILO RICHIEDENTE direttore-responsabile			
		//		Object[] ruoliRichiedente = membershipService.getAllRolesForUser(userNameProponente).toArray();
		//		if (Arrays.asList(ruoliRichiedente).contains("responsabile-struttura")) {
		//			profiloDomanda = "direttore-responsabile";
		//		}



		// VERIFICA direttore-responsabile
		if(profiloDomanda.equals("direttore-responsabile") ) {
			String idSedeDirettoregenerale = aceService.getSedeIdByIdNsip("630000");
			idAceStrutturaDomandaRichiedente = Integer.parseInt(idSedeDirettoregenerale);
		} else {
			try {
				// responsabileStruttura = aceService.findResponsabileStruttura(userNameProponente, dateRif, TipoAppartenenza.SEDE, "responsabile-struttura");
				//responsabileStruttura = aceService.findResponsabileStrutturaByCodiceSede(idNsipRichiedente, dateRif, "responsabile-struttura");
				responsabileStruttura = aceService.findResponsabileStrutturaByCodiceSede(idNsipRichiedente, dateRif, null);
				
				if (responsabileStruttura.getUtente()== null) {
					throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + userNameProponente + " <br>Si prega di contattare l'help desk in merito<br>");
				} 
				if (responsabileStruttura.getEntitaOrganizzativa().getId()== null) {
					throw new BpmnError("412", "l'utenza: " + userNameProponente + " non risulta associata ad alcuna struttura<br>");
				} 
				
				if (responsabileStruttura.getUtente().getUsername().equals(userNameProponente.toString())) {
					profiloDomanda = "direttore-responsabile";
					String idSedeDirettoregenerale = aceService.getSedeIdByIdNsip("630000");
					idAceStrutturaDomandaRichiedente = Integer.parseInt(idSedeDirettoregenerale);
				} else {
					idAceStrutturaDomandaRichiedente = responsabileStruttura.getEntitaOrganizzativa().getId();
				}

			} catch ( FeignException  e) {
				throw new BpmnError("412", "Errore nell'avvio del flusso " + e.getMessage().toString());
			}
		}

		// DETERMINA PERCORSO FLUSSO
		String profiloFlusso = "Indefinito";
		if(profiloDomanda.equals("direttore-responsabile") || profiloDomanda.equals("ricercatore-tecnologo")) {
			profiloFlusso = "PresaVisione";
		} 
		if(profiloDomanda.equals("collaboratore") ) {
			profiloFlusso = "Validazione";
		} 		
		
		//DATI STRUTTURA VALIDAZIONE
		entitaOrganizzativaDirettore = aceService.entitaOrganizzativaById(idAceStrutturaDomandaRichiedente);
		String cdsuoStrutturaDomandaRichiedente = entitaOrganizzativaDirettore.getCdsuo();
		String idNsipStrutturaDomandaRichiedente = entitaOrganizzativaDirettore.getIdnsip();
		if(profiloDomanda.equals("direttore-responsabile") ) {
			LOGGER.info("L'utente {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameProponente, entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());
		} else {
			LOGGER.info("L'utente {} ha come responsabile-struttura [{}] (per SEDE) {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameProponente, responsabileStruttura.getRuolo().getDescr(), responsabileStruttura.getUtente().getUsername(), entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());
		}
		//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
		String gruppoDirigenteProponente = "responsabile-struttura@" + idAceStrutturaDomandaRichiedente;
		String gruppoResponsabileSegreteria = "rs@" + idAceStrutturaDomandaRichiedente;	

		String applicazioneSiper = "app.siper";
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneSiper, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirigenteProponente, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileSegreteria, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);

		execution.setVariable("idNsipStrutturaDomandaRichiedente", idNsipStrutturaDomandaRichiedente);
		execution.setVariable("strutturaValutazioneDirigente", cdsuoStrutturaDomandaRichiedente + "-" + entitaOrganizzativaDirettore.getDenominazione());
		execution.setVariable("idAceStrutturaDomandaRichiedente", idAceStrutturaDomandaRichiedente);
		execution.setVariable("idStruttura", String.valueOf(idAceStrutturaDomandaRichiedente));
		execution.setVariable("cdsuoStrutturaDomandaRichiedente", cdsuoStrutturaDomandaRichiedente);
		execution.setVariable("livelloRichiedente", livelloRichiedente);
		
		execution.setVariable("profiloDomanda", profiloDomanda);
		execution.setVariable("profiloFlusso", profiloFlusso);

		execution.setVariable("gruppoDirigenteProponente", gruppoDirigenteProponente);
		execution.setVariable("gruppoResponsabileSegreteria", gruppoResponsabileSegreteria);

		execution.setVariable("applicazioneSiper", applicazioneSiper);
		execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
	}
}