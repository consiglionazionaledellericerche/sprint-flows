package it.cnr.si.flows.ng.listeners.cnr.telelavoro;



import it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTimerService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
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
public class StartTelelavoroSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartTelelavoroSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceService aceService;

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		String utenteRichiedente = execution.getVariable("utenteRichiedente").toString();
		String usernameResponsabileTelelavoro = execution.getVariable("usernameResponsabileTelelavoro").toString();
		String meseTelelavoro = execution.getVariable("meseTelelavoro").toString();
		String annoTelelavoro = execution.getVariable("annoTelelavoro").toString();
		String codiceSedeTelelavoro = execution.getVariable("codiceSedeTelelavoro").toString();
		String idStruttura = aceService.getSedeIdByIdNsip(codiceSedeTelelavoro).toString();
		String codiceCdsuoTelelavoro = aceService.entitaOrganizzativaById(Integer.parseInt(idStruttura)).getCdsuo();

		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {} per l'utente {} - mese:  {} - anno  {})", initiator, execution.getId(), execution.getVariable("titolo"), utenteRichiedente, meseTelelavoro, annoTelelavoro );

		String gruppoValidatoriTelelavoro = "valida-attestati#attestati-app@" + idStruttura;

		LOGGER.debug("Imposto i gruppi del flusso: gruppoValidatoriTelelavoro {} ",  gruppoValidatoriTelelavoro);
		execution.setVariable("gruppoValidatoriTelelavoro", gruppoValidatoriTelelavoro);
		execution.setVariable("codiceSedeTelelavoro", codiceSedeTelelavoro);
		execution.setVariable("codiceCdsuoTelelavoro", codiceCdsuoTelelavoro);
		execution.setVariable("idStruttura", idStruttura);
		
		//runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), usernameResponsabileTelelavoro, PROCESS_VISUALIZER);
	}
}