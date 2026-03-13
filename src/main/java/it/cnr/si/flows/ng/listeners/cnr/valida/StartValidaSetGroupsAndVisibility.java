package it.cnr.si.flows.ng.listeners.cnr.valida;


import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.MembershipService;
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
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.idStruttura;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;


@Component
@Profile("cnr")

@Service
public class StartValidaSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartValidaSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceService aceService;
	@Inject
	private MembershipService membershipService;
	@Inject
	private AceBridgeService aceBridgeService;	

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {
		String applicazioneSiper = "app.siper";
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";
		Integer idStruttura = null;
		
		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		String userNameRichiedente = execution.getVariable("userNameRichiedente", String.class);
		String idNsipStrutturaValidatore = execution.getVariable("idNsipStrutturaValidatore", String.class);
		String idStrutturaValidatore = aceService.getSedeIdByIdNsip(idNsipStrutturaValidatore);
		SimpleEntitaOrganizzativaWebDto strutturaRichiedente = aceService.getPersonaByUsername(userNameRichiedente).getSede();
		idStruttura = strutturaRichiedente.getId();
		
		String gruppoValidatori = "validatoriFlussoValida@"+ idStrutturaValidatore;

		LOGGER.debug("Imposto il gruppo del flusso {}", gruppoValidatori);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatori, PROCESS_VISUALIZER);

		execution.setVariable("cdsuo", execution.getVariable("codiceUo"));
		execution.setVariable("idnsip", strutturaRichiedente.getIdnsip());
		execution.setVariable("denominazione", strutturaRichiedente.getDenominazione());
		execution.setVariable("gruppoValidatori", gruppoValidatori);
		
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneSiper, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatori, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);
	}
}
