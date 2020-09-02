package it.cnr.si.flows.ng.listeners.cnr.missioniOrdine;



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
public class StartMissioniOrdineSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartMissioniOrdineSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;

	
	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		// TODO  DA CAMBIARE IL NOME DEL GRUPPO da  responsabile-struttura@ a responsbile-missioni@
		String gruppoFirmatarioUo = "responsabile-struttura@" + (String) execution.getVariable("idStrutturaUoMissioni");
		String gruppoFirmatarioSpesa = "responsabile-struttura@" + (String) execution.getVariable("idStrutturaUoMissioni").toString();
		String tipologiaFirmaMissione = execution.getVariable("validazioneSpesaFlag").toString();
		if (tipologiaFirmaMissione.equals("si")) {
			// TODO  DA CAMBIARE IL NOME DEL GRUPPO
			//gruppoFirmatarioSpesa = "responsbile-missioni@" + (String) execution.getVariable("idStrutturaSpesaMissioni");
			gruppoFirmatarioSpesa = "responsabile-struttura@" + (String) execution.getVariable("idStrutturaSpesaMissioni");
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmatarioSpesa, PROCESS_VISUALIZER);
		}
		String applicazioneMissioni = "app.missioni";

		LOGGER.debug("Imposto i gruppi del flusso {}, {}",  gruppoFirmatarioUo, gruppoFirmatarioSpesa);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmatarioUo, PROCESS_VISUALIZER);
		execution.setVariable("gruppoFirmatarioUo", gruppoFirmatarioUo);
		execution.setVariable("gruppoFirmatarioSpesa", gruppoFirmatarioSpesa);
	}
}