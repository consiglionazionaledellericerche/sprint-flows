package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliDomande;


import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.h2.util.New;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import feign.FeignException;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("!oiv")

@Service
public class StartAccordiInternazionaliDomandeSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartAccordiInternazionaliDomandeSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private AceService aceService;
	@Inject
	private SiperService siperService;

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		String richiedente = execution.getVariable("userNameRichiedente", String.class);
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		//Integer cdsuoAppartenenzaUtente = aceBridgeService.getEntitaOrganizzativaDellUtente(richiedente.toString()).getId();
		String cdsuoAppartenenzaUtente = null;
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(richiedente.toString()).getCdsuo();
		} catch(UnexpectedResultException | FeignException e) {
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(richiedente.toString()).get("codice_uo").toString();
		}
		finally {
			LOGGER.debug("getDirettoreCDSUO  FUNZIONA ");
			Object insdipResponsabileUo = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("codice_sede");
			Integer idEntitaorganizzativaResponsabileUtente = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0).getId();
			String gruppoValidatoriAccordiInternazionali = "validatoriAccordiInternazionali@0000";
			String gruppoUfficioProtocollo = "ufficioProtocolloAccordiInternazionali@0000";
			String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@0000";
			String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
			//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
			String gruppoDirigenteRichiedente = "responsabile-struttura@" + idEntitaorganizzativaResponsabileUtente;

			String applicazioneAccordiInternazionali = "app.abil";
			String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

			LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriAccordiInternazionali, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriAccordiInternazionali, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneAccordiInternazionali, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirigenteRichiedente, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);

			execution.setVariable("strutturaValutazioneDirigente", aceBridgeService.getAfferenzaUtente(richiedente.toString()).getId() + "-" + aceBridgeService.getAfferenzaUtente(richiedente.toString()).getDenominazione());
			execution.setVariable("gruppoValidatoriAccordiInternazionali", gruppoValidatoriAccordiInternazionali);
			execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
			execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
			execution.setVariable("applicazioneAccordiInternazionali", applicazioneAccordiInternazionali);
			execution.setVariable("gruppoDirigenteRichiedente", gruppoDirigenteRichiedente);
			execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
			execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
			execution.setVariable("cdsuoRichiedente", aceBridgeService.getAfferenzaUtente(richiedente.toString()).getCdsuo());
		}
	}
}