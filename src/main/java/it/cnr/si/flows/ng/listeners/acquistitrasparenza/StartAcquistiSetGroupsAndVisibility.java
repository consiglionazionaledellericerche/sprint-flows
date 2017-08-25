package it.cnr.si.flows.ng.listeners.acquistitrasparenza;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

import java.util.List;
import java.util.stream.Collectors;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.MembershipService;

@Component
public class StartAcquistiSetGroupsAndVisibility implements ExecutionListener {
    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StartAcquistiSetGroupsAndVisibility.class);

    @Autowired
    private MembershipService membershipService;
    @Autowired
    private RuntimeService runtimeService;

    @Override
    public void notify(DelegateExecution execution) throws Exception {

        String initiator = (String) execution.getVariable("initiator");
        LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

        List<GrantedAuthority> authorities = membershipService.getAllAdditionalAuthoritiesForUser(initiator);

        List<String> groups = authorities.stream()
                .map(a -> a.getAuthority())
                .map(Utils::removeLeadingRole)
                .filter(g -> g.startsWith("ra@"))
                .collect(Collectors.toList());

        if ( groups.size() == 0 )
            throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
        else if ( groups.size() > 1 )
            throw new BpmnError("500", "L'utente appartiene a piu' di un gruppo Responsabile Tecnico");
        else {

            String gruppoRT = groups.get(0);
            String struttura = gruppoRT.substring(gruppoRT.lastIndexOf('@') +1);
            // idStruttura variabile che indica che il flusso è diviso per strutture (implica la visibilità distinta tra strutture)
            execution.setVariable("idStruttura", struttura);
            String gruppoDirettore = "direttore@"+ struttura;
            String gruppoRA = "ra@"+ struttura;
            String gruppoSFD = "sfd@"+ struttura;
            String rup = execution.getVariable("rup", String.class);

            LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}, {}", gruppoRT, gruppoSFD, gruppoRA, gruppoDirettore);

            //Check se il gruppo SFD ha membri
            List<String> members = membershipService.findMembersInGroup(gruppoSFD);
            if ( members.size() == 0 ) {
                execution.setVariable("organizzazioneStruttura", "Semplice");
            } else {
                execution.setVariable("organizzazioneStruttura", "Complessa");
            }

            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoRT, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirettore, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoRA, PROCESS_VISUALIZER);
            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoSFD, PROCESS_VISUALIZER);
            runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), rup, PROCESS_VISUALIZER);
//            runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "segreteria@" + struttura, PROCESS_VISUALIZER);

            execution.setVariable("gruppoRT", gruppoRT);
            execution.setVariable("gruppoDirettore", gruppoDirettore);
            execution.setVariable("gruppoRA", gruppoRA);
            execution.setVariable("gruppoSFD", gruppoSFD);
        }

    }
}