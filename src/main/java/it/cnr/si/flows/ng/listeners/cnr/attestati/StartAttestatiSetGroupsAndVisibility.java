package it.cnr.si.flows.ng.listeners.cnr.attestati;



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
public class StartAttestatiSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartAttestatiSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceService aceService;

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		String utenteRichiedente = execution.getVariable("utenteRichiedente").toString();
		String meseAttestato = execution.getVariable("meseAttestato").toString();
		String annoAttestato = execution.getVariable("annoAttestato").toString();
		String codiceSedeAttestato = execution.getVariable("codiceSedeAttestato").toString();
		String idStruttura = aceService.getSedeIdByIdNsip(codiceSedeAttestato).toString();
		String codiceCdsuoAttestato = aceService.entitaOrganizzativaById(Integer.parseInt(idStruttura)).getCdsuo();

		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {} per l'utente {} - mese:  {} - anno  {})", initiator, execution.getId(), execution.getVariable("titolo"), utenteRichiedente, meseAttestato, annoAttestato );

		String gruppoValidatoriAttestati = "valida-attestati@" + idStruttura;

		LOGGER.debug("Imposto i gruppi del flusso: gruppoValidatoriAttestati {} ",  gruppoValidatoriAttestati);
		execution.setVariable("gruppoValidatoriAttestati", gruppoValidatoriAttestati);
		execution.setVariable("codiceSedeAttestato", codiceSedeAttestato);
		execution.setVariable("codiceCdsuoAttestato", codiceCdsuoAttestato);
		execution.setVariable("idStruttura", idStruttura);

	}
}