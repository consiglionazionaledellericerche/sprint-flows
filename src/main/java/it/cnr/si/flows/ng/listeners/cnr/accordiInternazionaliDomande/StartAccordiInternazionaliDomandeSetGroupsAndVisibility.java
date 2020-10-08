package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliDomande;


import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.enums.TipoAppartenenza;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")

@Service
public class StartAccordiInternazionaliDomandeSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartAccordiInternazionaliDomandeSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;

	@Inject
	private AceService aceService;


	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		//String richiedente = execution.getVariable("userNameRichiedente", String.class);
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		//Integer cdsuoAppartenenzaUtente = aceBridgeService.getEntitaOrganizzativaDellUtente(richiedente.toString()).getId();
		String cdsuoAppartenenzaUtente = null;
		Integer IdEntitaOrganizzativaDirettore = 0;
		EntitaOrganizzativaWebDto entitaOrganizzativaDirettore = null;
		LocalDate dateRif = LocalDate.now();
		BossDto responsabileStruttura = null;
		String denominazioneEntitaorganizzativaResponsabileUtente = null;

		String userNameRichiedente = execution.getVariable("userNameRichiedente", String.class);
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", userNameRichiedente, execution.getId(), execution.getVariable("title"));

		// VERIFICA RESPOSNABILE STRUTTURA AFFERENZA CDSUO
		//direttoreAce = aceService.bossFirmatarioByUsername(userNameRichiedente, dateRif);
		responsabileStruttura = aceService.findResponsabileStruttura(userNameRichiedente, dateRif, TipoAppartenenza.AFFERENZA_UO, "responsabile-struttura");
		if (responsabileStruttura.getUtente()== null) {
			throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + userNameRichiedente + " <br>Si prega di contattare l'help desk in merito<br>");
		} else {
		}
		if (responsabileStruttura.getEntitaOrganizzativa().getId()== null) {
			throw new BpmnError("412", "l'utenza: " + userNameRichiedente + " non risulta associata ad alcuna struttura<br>");
		} else {
			IdEntitaOrganizzativaDirettore = responsabileStruttura.getEntitaOrganizzativa().getId();
			entitaOrganizzativaDirettore = aceService.entitaOrganizzativaById(IdEntitaOrganizzativaDirettore);
			cdsuoAppartenenzaUtente = entitaOrganizzativaDirettore.getCdsuo();
			denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirettore.getDenominazione();
		}

		//		Object insdipResponsabileUo = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("codice_sede");
		//		String usernameDirettore = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
		//		EntitaOrganizzativaWebDto entitaOrganizzativaDirUo = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0);
		//		Integer idEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getId();
		//		String siglaEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getSigla().toString();
		//		String denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getDenominazione().toString();
		//		String cdsuoEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getCdsuo().toString();
		//		String idnsipEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getIdnsip().toString();	

		LOGGER.info("L'utente {} ha come direttore {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameRichiedente, responsabileStruttura.getUtente().getUsername(), denominazioneEntitaorganizzativaResponsabileUtente, entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());

		String gruppoValidatoriAccordiInternazionali = "validatoriAccordiInternazionali@0000";
		String gruppoUfficioProtocollo = "ufficioProtocolloAccordiInternazionali@0000";
		String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@0000";
		String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
		//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
		String gruppoDirigenteRichiedente = "responsabile-struttura@" + IdEntitaOrganizzativaDirettore;

		String applicazioneAccordiInternazionali = "app.abil";
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriAccordiInternazionali, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);
		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriAccordiInternazionali, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirigenteRichiedente, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);

		execution.setVariable("strutturaValutazioneDirigente", IdEntitaOrganizzativaDirettore + "-" + denominazioneEntitaorganizzativaResponsabileUtente);
		execution.setVariable("gruppoValidatoriAccordiInternazionali", gruppoValidatoriAccordiInternazionali);
		execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
		execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
		execution.setVariable("applicazioneAccordiInternazionali", applicazioneAccordiInternazionali);
		execution.setVariable("gruppoDirigenteRichiedente", gruppoDirigenteRichiedente);
		execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
		execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		execution.setVariable("cdsuoRichiedente", cdsuoAppartenenzaUtente);
	}
}