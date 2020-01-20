package it.cnr.si.flows.ng.listeners.cnr.shortTermMobilityBandoDipartimento;



import it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTimerService;
import it.cnr.si.flows.ng.utils.Enum;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;



@Component
@Profile("!oiv")

@Service
public class StartShortTermMobilityBandoDipartimentoSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartShortTermMobilityBandoDipartimentoSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;

	
	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		String gruppoValidatoriShortTermMobility = "validatoriShortTermMobility@0000";
		String gruppoUfficioProtocollo = "ufficioProtocolloShortTermMobility@0000";
		String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@0000";
		String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
		String applicazioneShortTermMobility = "app.abil";

		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriShortTermMobility, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriShortTermMobility, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneShortTermMobility, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);

		execution.setVariable("gruppoValidatoriShortTermMobility", gruppoValidatoriShortTermMobility);
		execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
		execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
		execution.setVariable("applicazioneShortTermMobility", applicazioneShortTermMobility);
		execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
	}
}