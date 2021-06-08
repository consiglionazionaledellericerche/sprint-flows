package it.cnr.si.flows.ng.listeners.cnr.laboratoriCongiuntiBandoDipartimento;



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
@Profile("cnr")

@Service
public class StartLaboratoriCongiuntiBandoDipartimentoSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartLaboratoriCongiuntiBandoDipartimentoSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;

	
	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		String gruppoValidatoriLaboratoriCongiunti = "validatoriLaboratoriCongiunti@0000";
		String gruppoUfficioProtocollo = "ufficioProtocolloLaboratoriCongiunti@0000";
		String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoLABDipartimento@0000";
		String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
		String applicazioneLaboratoriCongiunti = "app.labcon";

		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriLaboratoriCongiunti, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriLaboratoriCongiunti, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneLaboratoriCongiunti, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);

		execution.setVariable("gruppoValidatoriLaboratoriCongiunti", gruppoValidatoriLaboratoriCongiunti);
		execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
		execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
		execution.setVariable("applicazioneLaboratoriCongiunti", applicazioneLaboratoriCongiunti);
		execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
	}
}