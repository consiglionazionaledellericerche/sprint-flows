package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.flows.ng.service.FlowsSiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.idStruttura;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")

@Service
public class StartAcquistiSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartAcquistiSetGroupsAndVisibility.class);

	@Inject
	private RelationshipService relationshipService;
	@Autowired(required = false)
	private AceBridgeService aceBridgeService;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private CounterService counterService;
	@Inject
	private AceService aceService;
	@Inject
	private FlowsSiperService flowsSiperService;
	@Inject
	private MembershipService membershipService;

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {


		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		List<String> groups = membershipService.getAllRolesForUser(initiator).stream()
				.filter(g -> g.startsWith("staffAmministrativo@"))
				.collect(Collectors.toList());

		if (groups.isEmpty())
			throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
		else {

			//String gruppoStaffAmministrativo = groups.get(0);
//			String struttura = gruppoStaffAmministrativo.substring(gruppoStaffAmministrativo.lastIndexOf('@') +1);
//			// idStruttura variabile che indica che il flusso è diviso per strutture (implica la visibilità distinta tra strutture)
//
//			// NUOVA PROCEDURA PER PRENDERE L'ENTITA' ORGANIZZATIVA DI RIFERIMENTO
//			String cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(initiator).getCdsuo();
//			Object insdipResponsabileUo = siperService.getResponsabileCDSUO(cdsuoAppartenenzaUtente).get(0).get("codice_sede");
//			String strutturaAppartenenza = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0).getId().toString();
//
//			if (!struttura.equals(strutturaAppartenenza)) {
//				LOGGER.info("TEST l'utente {} sta avviando il flusso {} con per una struttura [{}] diversa da quella di appartenenza [{}])", initiator, execution.getId(), struttura,strutturaAppartenenza);
//			}
//
//			execution.setVariable(idStruttura.name(), struttura);

			String struttura = execution.getVariable(idStruttura.name()).toString();

			String gruppoFirmaAcquisti = "responsabileFirmaAcquisti@"+ struttura;
			String gruppoStaffAmministrativo = "staffAmministrativo@"+ struttura;
			String gruppoSFD = "sfd@"+ struttura;
			String applicazioneSigla = "app.sigla";

			LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}, {}", gruppoStaffAmministrativo, gruppoSFD, gruppoStaffAmministrativo, gruppoFirmaAcquisti);

			//Check se il gruppo SFD ha membri
			Set<String> members = aceBridgeService.getUsersInAceGroup(gruppoSFD);
			if (members.isEmpty()) {
				execution.setVariable("organizzazioneStruttura", "Semplice");
			} else {
				execution.setVariable("organizzazioneStruttura", "Complessa");
			}
			execution.setVariable("nomeStruttura", aceBridgeService.getNomeStruturaById(Integer.parseInt(struttura)));

			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoStaffAmministrativo, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmaAcquisti, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoStaffAmministrativo, PROCESS_VISUALIZER);
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoSFD, PROCESS_VISUALIZER);
			runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), applicazioneSigla, PROCESS_VISUALIZER);
			//            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "segreteria@" + struttura, PROCESS_VISUALIZER);

			
			SimpleEntitaOrganizzativaWebDto strutturaAcquisto = aceService.entitaOrganizzativaById(Integer.parseInt(struttura));
			execution.setVariable("cdsuo", strutturaAcquisto.getCdsuo());
			execution.setVariable("idnsip", strutturaAcquisto.getIdnsip());
			execution.setVariable("denominazione", strutturaAcquisto.getDenominazione());
			execution.setVariable("gruppoStaffAmministrativo", gruppoStaffAmministrativo);
			execution.setVariable("gruppoFirmaAcquisti", gruppoFirmaAcquisti);
			execution.setVariable(Enum.VariableEnum.gruppoStaffAmministrativo.name(), gruppoStaffAmministrativo);
			execution.setVariable("gruppoSFD", gruppoSFD);
			execution.setVariable("sigla", applicazioneSigla);
			//SET VARIABILI Direzione flusso
			execution.setVariable("statoImpegni", "provvisori");
			if (execution.getVariable("tipologiaAffidamentoDiretto") == null) {
				execution.setVariable("tipologiaAffidamentoDiretto", "normale");
			}
			//SET CONTATORE ACQUISTO STRUTTURA
			String counterId = aceBridgeService.getUoById(Integer.parseInt(struttura)).getCdsuo() + "-ACQ-" + Calendar.getInstance().get(Calendar.YEAR);
			String key = counterId + "-" + counterService.getNext(counterId);
			execution.setVariable("codiceAcquistoStruttura", key);
		}

	}
}