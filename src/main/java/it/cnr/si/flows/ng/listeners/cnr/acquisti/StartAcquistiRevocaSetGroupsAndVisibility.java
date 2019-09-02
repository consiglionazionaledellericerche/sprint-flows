package it.cnr.si.flows.ng.listeners.cnr.acquisti;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.service.MembershipService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;
import static it.cnr.si.security.PermissionEvaluatorImpl.ID_STRUTTURA;

@Component
@Profile("!oiv")
public class StartAcquistiRevocaSetGroupsAndVisibility implements ExecutionListener {
    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StartAcquistiRevocaSetGroupsAndVisibility.class);

    @Autowired(required = false)
    private AceBridgeService aceBridgeService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private MembershipService membershipService;

    @Override
    public void notify(DelegateExecution execution) throws Exception {

        String initiator = (String) execution.getVariable("initiator");
        LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

        List<String> groups = membershipService.getAllGroupsForUser(initiator).stream()
                .filter(g -> g.startsWith("responsabile#"))
                .collect(Collectors.toList());

        if ( groups.size() == 0 )
            throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
        else {
            String gruppoRT = groups.get(0);
            String struttura = gruppoRT.substring(gruppoRT.lastIndexOf('@') +1);
            // idStruttura variabile che indica che il flusso è diviso per strutture (implica la visibilità distinta tra strutture)
            execution.setVariable(ID_STRUTTURA, struttura);
            String gruppoFirmaAcquisti = "responsabileFirmaAcquisti@"+ struttura;
            String gruppoStaffAmministrativo = "staffAmministrativo@"+ struttura;
            String gruppoSFD = "sfd@"+ struttura;
            String rup = execution.getVariable("rup", String.class);
            String applicazioneSigla = "app.sigla";

            LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}, {}", gruppoRT, gruppoSFD, gruppoStaffAmministrativo, gruppoFirmaAcquisti);

            //Check se il gruppo SFD ha membri
            List<String> members = aceBridgeService.getUsersInAceGroup(gruppoSFD);
            if ( members.size() == 0 ) {
                execution.setVariable("organizzazioneStruttura", "Semplice");
            } else {
                execution.setVariable("organizzazioneStruttura", "Complessa");
            }
            execution.setVariable("nomeStruttura", aceBridgeService.getNomeStruturaById(Integer.parseInt(struttura)));
            
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoRT, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmaAcquisti, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoStaffAmministrativo, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoSFD, PROCESS_VISUALIZER);
            runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), rup, PROCESS_VISUALIZER);
            runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), applicazioneSigla, PROCESS_VISUALIZER);
//            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "segreteria@" + struttura, PROCESS_VISUALIZER);

            execution.setVariable("gruppoRT", gruppoRT);
            execution.setVariable("gruppoFirmaAcquisti", gruppoFirmaAcquisti);
            execution.setVariable("gruppoStaffAmministrativo", gruppoStaffAmministrativo);
            execution.setVariable("gruppoSFD", gruppoSFD);
            execution.setVariable("sigla", applicazioneSigla);
        }

    }
}