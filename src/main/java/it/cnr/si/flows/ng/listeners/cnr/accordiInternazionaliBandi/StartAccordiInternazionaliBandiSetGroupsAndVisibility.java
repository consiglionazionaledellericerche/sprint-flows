package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliBandi;


import it.cnr.si.flows.ng.utils.Enum;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("!oiv")

@Service
public class StartAccordiInternazionaliBandiSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartAccordiInternazionaliBandiSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		String gruppoValidatoriAccordiInternazionali = "validatoriAccordiInternazionali@0000";
		String gruppoUfficioProtocollo = "ufficioProtocolloAccordiInternazionali@0000";
		String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@0000";
		String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
		String applicazioneAccordiInternazionali = "app.abil";

		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriAccordiInternazionali, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);

		execution.setVariable("gruppoValidatoriAccordiInternazionali", gruppoValidatoriAccordiInternazionali);
		execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
		execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
		execution.setVariable("applicazioneAccordiInternazionali", applicazioneAccordiInternazionali);
		execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
	}
}