package it.cnr.si.flows.ng.listeners.cnr.approvvigionamenti;



import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
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
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.idStruttura;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;


@Component
@Profile("cnr")

@Service
public class StartApprovvigionamentiSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartApprovvigionamentiSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceService aceService;
	@Inject
	private MembershipService membershipService;
	@Inject
	private AceBridgeService aceBridgeService;	


	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {
		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		List<String> groups = membershipService.getAllRolesForUser(initiator).stream()
				.filter(g -> g.startsWith("staffApprovvigionamenti@"))
				.collect(Collectors.toList());
		if (groups.isEmpty())
			throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
		else {
			String struttura = execution.getVariable(idStruttura.name()).toString();
			String gruppoResponsabileApprovvigionamenti = "responsabileApprovvigionamenti@0000";
			String gruppoLavorazione = "responsabileApprovvigionamenti@0000";
			
			if (execution.getVariable("tipologiaRichiesta").toString().startsWith("Telefonia-Fissa")){
				gruppoLavorazione = "gruppoLavorazioneTelefoniaFissa@0000";
			} 			
			if (execution.getVariable("tipologiaRichiesta").toString().startsWith("Telefonia-Mobile")){
				gruppoLavorazione = "gruppoLavorazioneTelefoniaMobile@0000";
			} 			
			if (execution.getVariable("tipologiaRichiesta").toString().startsWith("Cablaggio")){
				gruppoLavorazione = "gruppoLavorazioneCablaggio@0000";
			} 			
			if (execution.getVariable("tipologiaRichiesta").toString().startsWith("Desktop")){
				gruppoLavorazione = "gruppoLavorazioneDesktop@0000";
			} 

			LOGGER.debug("Imposto i gruppi del flusso: gruppoResponsabileApprovvigionamenti: {} e gruppoLavorazione: {}", gruppoResponsabileApprovvigionamenti, gruppoLavorazione);


			execution.setVariable("nomeStruttura", aceBridgeService.getNomeStruturaById(Integer.parseInt(struttura)));

			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileApprovvigionamenti, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoLavorazione, PROCESS_VISUALIZER);
			
			SimpleEntitaOrganizzativaWebDto strutturaAcquisto = aceService.entitaOrganizzativaById(Integer.parseInt(struttura));
			execution.setVariable("idStruttura", struttura);
			execution.setVariable("cdsuo", strutturaAcquisto.getCdsuo());
			execution.setVariable("idnsip", strutturaAcquisto.getIdnsip());
			execution.setVariable("denominazione", strutturaAcquisto.getDenominazione());
			execution.setVariable("gruppoResponsabileApprovvigionamenti", gruppoResponsabileApprovvigionamenti);
			execution.setVariable("gruppoLavorazione", gruppoLavorazione);
			execution.setVariable("userNameRichiedente", initiator);
		}
	}
}
