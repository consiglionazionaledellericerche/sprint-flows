package it.cnr.si.flows.ng.listeners.cnr.firmaPresidente;



import it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.FlowsTimerService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.FlowsUserService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.UserService;
import it.cnr.si.service.dto.anagrafica.letture.GerarchiaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.awt.print.Pageable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;
import static it.cnr.si.security.PermissionEvaluatorImpl.ID_STRUTTURA;


@Component
@Profile("cnr")

@Service
public class StartFirmaPresidenteSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartFirmaPresidenteSetGroupsAndVisibility.class);

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
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		// FLAG per sapere se la struttura richiedente è afferente ad un Direzione Centrale o deve andare direttamente al DG
		String flagDC = "no";
		//aceBridgeService.
		//${direttoreGenerale}
		//${presidente}
		//execution.setVariable(idStruttura, idStruttura);


		//strutturaDCSRSI = aceService.entitaOrganizzativaFind(offset, term, data, tipo)
		List<String> groups = membershipService.getAllRolesForUser(initiator).stream()
				.filter(g -> g.startsWith("seg#"))
				.collect(Collectors.toList());

		if ( groups.size() == 0 )
			throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
		else {
			String gruppoSegreteriaStrutturaRichiedente = groups.get(0);
			String idStruttura = gruppoSegreteriaStrutturaRichiedente.split("@")[1];
			// idStruttura variabile che indica che il flusso è diviso per strutture (implica la visibilità distinta tra strutture)
			execution.setVariable(idStruttura, idStruttura);
			execution.setVariable(gruppoSegreteriaStrutturaRichiedente, gruppoSegreteriaStrutturaRichiedente);
			LOGGER.debug("Imposto il gruppo gruppoSegreteriaStrutturaRichiedente del flusso {}", gruppoSegreteriaStrutturaRichiedente);
			SimpleEntitaOrganizzativaWebDto strutturaRichiedente = aceBridgeService.getStrutturaById(Integer.parseInt(idStruttura));
			Object tipoGerarchiaAppartenenza = aceService.tipiGerarchiaAppartenenza(Integer.parseInt(idStruttura));


			//
			//		execution.setVariable("gruppoFirmatarioUo", gruppoFirmatarioUo);
			//		execution.setVariable("idStrutturaUoMissioni", idStrutturaUoMissioni);
			//		execution.setVariable("idStruttura",  String.valueOf(idStrutturaUoMissioni));
			//		//FLAG CHE VERRA' IMPOSTATO IN FIRMA UO END
			//		execution.setVariable("firmaSpesaFlag", "no");

		}
	}
}
