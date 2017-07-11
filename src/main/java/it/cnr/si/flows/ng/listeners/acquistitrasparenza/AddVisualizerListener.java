package it.cnr.si.flows.ng.listeners.acquistitrasparenza;

import it.cnr.si.service.MembershipService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

//@Component
//public class AddVisualizerListener implements ActivitiEventListener {
@Component
public class AddVisualizerListener implements ExecutionListener {


    private static final Logger LOGGER = LoggerFactory.getLogger(AddVisualizerListener.class);


    @Inject
    private RuntimeService runtimeService;
    @Inject
    private MembershipService membershipService;


    @Override
    public void notify(DelegateExecution execution) throws Exception {

        String processDefinition = execution.getProcessDefinitionId();
        processDefinition = processDefinition.substring(0, processDefinition.indexOf(":"));
        switch (processDefinition) {
            case "acquisti-trasparenza":
                String struttura = (String) execution.getVariable("gruppoRA");
                struttura = struttura.substring(struttura.indexOf('@') + 1, struttura.length());

                runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "ra@" + struttura, PROCESS_VISUALIZER);
                LOGGER.info("Aggiunta IdentityLink al flusso {} per il gruppo {}", execution.getId(), "ra@" + struttura);
                runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "direttore@" + struttura, PROCESS_VISUALIZER);
                LOGGER.info("Aggiunta IdentityLink al flusso {} per il gruppo {}", execution.getId(), "direttore@" + struttura);
                runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "segreteria@" + struttura, PROCESS_VISUALIZER);
                LOGGER.info("Aggiunta IdentityLink al flusso {} per il gruppo {}", execution.getId(), "segreteria@" + struttura);
                runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "rt@" + struttura, PROCESS_VISUALIZER);
                LOGGER.info("Aggiunta IdentityLink al flusso {} per il gruppo {}", execution.getId(), "rt@" + struttura);
                runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), "sfd@" + struttura, PROCESS_VISUALIZER);
                LOGGER.info("Aggiunta IdentityLink al flusso {} per il gruppo {}", execution.getId(), "sfd@" + struttura);
                break;

            case "permessi-ferie":

                break;
        }


    }
}
