package it.cnr.si.flows.ng.listeners.acquistitrasparenza;

import it.cnr.si.flows.ng.dto.FlowsAttachment.ProcessDefinitionEnum;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
public class AddVisualizerListener implements ExecutionListener {

    private static final long serialVersionUID = 5263454627295290775L;


    private static final Logger LOGGER = LoggerFactory.getLogger(AddVisualizerListener.class);


    @Inject
    private RuntimeService runtimeService;

    @Override
    public void notify(DelegateExecution execution) throws Exception {

        String processDefinitionString = execution.getProcessDefinitionId();
        ProcessDefinitionEnum processDefinition = ProcessDefinitionEnum.valueOf(processDefinitionString.substring(0, processDefinitionString.indexOf(":")));
        switch (processDefinition) {
            case acquistiTrasparenza:
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

            case permessiFerie:

                break;
        }


    }
}
