package it.cnr.si.flows.ng.listeners.oiv;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
public class StartOivSetGroupsAndVisibility implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(StartOivSetGroupsAndVisibility.class);

	@Inject
	private RelationshipService relationshipService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private RuntimeService runtimeService;

	@Override
	public void notify(DelegateExecution execution) throws Exception {

		String initiator = (String) execution.getVariable("initiator");
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		String struttura = "99999";
		String gruppoIstruttori = "gruppoIstruttori@"+ struttura;
		String gruppoDirettore = "direttore@"+ struttura;
		String gruppoCoordinatoreResponsabile = "coordinatoreresponsabile@"+ struttura;
		execution.setVariable("gruppoIstruttori", gruppoIstruttori);
		execution.setVariable("gruppoDirettore", gruppoDirettore);
		execution.setVariable("gruppoCoordinatoreResponsabile", gruppoCoordinatoreResponsabile);

		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}, {}", gruppoIstruttori, gruppoDirettore, gruppoCoordinatoreResponsabile);


		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoIstruttori, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirettore, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoCoordinatoreResponsabile, PROCESS_VISUALIZER);

	}
}