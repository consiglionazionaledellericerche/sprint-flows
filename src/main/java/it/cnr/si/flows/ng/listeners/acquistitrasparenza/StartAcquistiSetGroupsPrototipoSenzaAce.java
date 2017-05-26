package it.cnr.si.flows.ng.listeners.acquistitrasparenza;

import java.util.List;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import it.cnr.si.service.MembershipService;

@Component
public class StartAcquistiSetGroupsPrototipoSenzaAce implements ExecutionListener {
    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StartAcquistiSetGroupsPrototipoSenzaAce.class);

    @Autowired
    private MembershipService membershipService;

//    public StartAcquistiSetGroupsPrototipoSenzaAce() {
//        applicationContext.getAutowireCapableBeanFactory().autowireBean(this);
//    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {

        try {
            String initiator = (String) execution.getVariable("initiator");
            List<GrantedAuthority> authorities = membershipService.getAllAdditionalAuthoritiesForUser(initiator);

            List<GrantedAuthority> groups = authorities.stream().filter(a -> a.getAuthority().endsWith("_rt")).collect(Collectors.toList());
            if ( groups.size() == 0 )
                throw new BpmnError("403", "L'utente non e' abilitato ad avviare questo flusso");
            else if ( groups.size() > 1 )
                throw new BpmnError("500", "L'utente appartiene a piu' di un gruppo Responsabile Tecnico");
            else {
                GrantedAuthority groupRT = groups.get(0);
                String struttura = groupRT.getAuthority().substring(0, groupRT.getAuthority().lastIndexOf('_'));

                String gruppoRT = groupRT.getAuthority();
                String gruppoDirettore = struttura +"_direttore";
                String gruppoRA = struttura +"_ra";
                String gruppoSFD = struttura +"_sfd";

                execution.setVariable("gruppoRT", gruppoRT);
                execution.setVariable("gruppoDirettore", gruppoDirettore);
                execution.setVariable("gruppoRA", gruppoRA);
                execution.setVariable("gruppoSFD", gruppoSFD);
            }
        }catch (Exception e) {
            LOGGER.error(e.getMessage(), e);

            execution.setVariable("gruppoRT", "sisinfo_rt");
            execution.setVariable("gruppoDirettore", "sisinfo_direttore");
            execution.setVariable("gruppoRA", "sisinfo_ra");
            execution.setVariable("gruppoSFD", "sisinfo_sfd");
        }
    }
}
