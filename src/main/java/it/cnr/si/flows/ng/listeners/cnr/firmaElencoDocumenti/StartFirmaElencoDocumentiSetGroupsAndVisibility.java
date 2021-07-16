package it.cnr.si.flows.ng.listeners.cnr.firmaElencoDocumenti;



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
public class StartFirmaElencoDocumentiSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartFirmaElencoDocumentiSetGroupsAndVisibility.class);

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

		String utenteFirmatario = execution.getVariable("userNameFirmatario").toString();
		execution.setVariable("utenteFirmatario", utenteFirmatario);
		runtimeService.addParticipantUser(execution.getProcessInstanceId(), utenteFirmatario);
	}
}