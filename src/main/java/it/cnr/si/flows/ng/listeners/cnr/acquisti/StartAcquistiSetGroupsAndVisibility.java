package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import it.cnr.si.flows.ng.listeners.oiv.service.OivSetGroupsAndVisibility;
import it.cnr.si.flows.ng.listeners.oiv.service.OperazioniTimer;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.CounterService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.idStruttura;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("!oiv")

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
    
	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {


        String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
       // LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
        LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

        List<GrantedAuthority> authorities = relationshipService.getAllGroupsForUser(initiator);

        List<String> groups = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .filter(g -> g.startsWith("staffAmministrativo@"))
                .collect(Collectors.toList());

        if (groups.isEmpty())
            throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
        else if ( groups.size() > 1 )
            throw new BpmnError("500", "L'utente appartiene a piu' di un gruppo Staff Amministrativo");
        else {

            String gruppoRT = groups.get(0);
            String struttura = gruppoRT.substring(gruppoRT.lastIndexOf('@') +1);
            // idStruttura variabile che indica che il flusso è diviso per strutture (implica la visibilità distinta tra strutture)
            execution.setVariable(idStruttura.name(), struttura);
            String gruppoFirmaAcquisti = "responsabileFirmaAcquisti@"+ struttura;
            String gruppoStaffAmministrativo = "staffAmministrativo@"+ struttura;
            String gruppoSFD = "sfd@"+ struttura;
            String applicazioneSigla = "app.sigla";

            LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}, {}", gruppoRT, gruppoSFD, gruppoStaffAmministrativo, gruppoFirmaAcquisti);

            //Check se il gruppo SFD ha membri
            List<String> members = aceBridgeService.getUsersInAceGroup(gruppoSFD);
            if (members.isEmpty()) {
                execution.setVariable("organizzazioneStruttura", "Semplice");
            } else {
                execution.setVariable("organizzazioneStruttura", "Complessa");
            }
            execution.setVariable("nomeStruttura", aceBridgeService.getNomeStruturaById(Integer.parseInt(struttura)));

            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoRT, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmaAcquisti, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoStaffAmministrativo, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoSFD, PROCESS_VISUALIZER);
            runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), applicazioneSigla, PROCESS_VISUALIZER);
//            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "segreteria@" + struttura, PROCESS_VISUALIZER);

            execution.setVariable("gruppoRT", gruppoRT);
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