package it.cnr.si.flows.ng.listeners.cnr.missioni;



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
public class StartMissioniSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartMissioniSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;


	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		String missioneEsteraFlag = execution.getVariable("missioneEsteraFlag").toString();
		String gruppoFirmatarioUo = execution.getVariable("gruppoFirmatarioUo").toString();
		String gruppoFirmatarioUoSigla = gruppoFirmatarioUo.split("@")[0];
		int idStrutturaUoMissioni = Integer.parseInt(gruppoFirmatarioUo.split("@")[1].toString());
		String gruppoFirmatarioSpesa = null;

		String tipologiaFirmaMissione = execution.getVariable("validazioneSpesaFlag").toString();
		if (tipologiaFirmaMissione.equals("si")) {
			gruppoFirmatarioSpesa = execution.getVariable("gruppoFirmatarioUo").toString();
			String gruppoFirmatarioSpesaSigla = gruppoFirmatarioSpesa.split("@")[0];
			int idStrutturaSpesaMissioni = Integer.parseInt(gruppoFirmatarioSpesa.split("@")[1].toString());
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmatarioSpesa, PROCESS_VISUALIZER);
			execution.setVariable("idStrutturaSpesaMissioni", idStrutturaSpesaMissioni);
			execution.setVariable("idStruttureSupervisione", idStrutturaSpesaMissioni);	
		}

		LOGGER.debug("Imposto i gruppi del flusso gruppoFirmatarioUo {} - gruppoFirmatarioSpesa {} - per il flusso con missioneEsteraFlag {} - tipologiaFirmaMissione {}",  gruppoFirmatarioUo, gruppoFirmatarioSpesa, missioneEsteraFlag, tipologiaFirmaMissione);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmatarioUo, PROCESS_VISUALIZER);
		if (execution.getVariable("userNameUtenteMissione") != null) {
			runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), execution.getVariable("userNameUtenteMissione").toString(), PROCESS_VISUALIZER);
		}
		if (execution.getVariable("userNameRichiedente") != null) {
			runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), execution.getVariable("userNameRichiedente").toString(), PROCESS_VISUALIZER);
		}

		execution.setVariable("gruppoFirmatarioUo", gruppoFirmatarioUo);
		execution.setVariable("idStrutturaUoMissioni", idStrutturaUoMissioni);
		execution.setVariable("idStruttura", idStrutturaUoMissioni);
		//FLAG CHE VERRA' IMPOSTATO IN FIRMA UO END
		execution.setVariable("firmaSpesaFlag", "no");
	}
}