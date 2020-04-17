package it.cnr.si.flows.ng.listeners;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.event.ActivitiEntityWithVariablesEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.cnr.si.flows.ng.service.FlowsAttachmentService.NUMERI_PROTOCOLLO;
import static it.cnr.si.security.PermissionEvaluatorImpl.ID_STRUTTURA;

@Component
public class AddFlowsAttachmentsListener implements ActivitiEventListener {

    private final Logger LOGGER = LoggerFactory.getLogger(AddFlowsAttachmentsListener.class);

    @Inject
    private RuntimeService runtimeService;
    @Inject
    private TaskService taskService;
    @Autowired(required = false)
    private AceBridgeService aceBridgeService;
    @Inject
    private Environment env;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;

    @Override
    public void onEvent(ActivitiEvent event) {
        LOGGER.info("Sono in AddFlowsAttachmentsListener con evento "+ event.getType() + event.getExecutionId());

        String path, taskId, taskName, key;

        Map<String, Object> processVariables = runtimeService.getVariables(event.getExecutionId());
        Map<String, FlowsAttachment> attachments = new HashMap<>();

        if (event.getType() == ActivitiEventType.PROCESS_STARTED) {
            // setto il path per il salvataggio dei documenti
            path = getDefaultPathFascicoloDocumenti(event.getExecutionId());
            runtimeService.setVariable(event.getExecutionId(), "pathFascicoloDocumenti", path);
            taskId = "start";
            taskName = "Avvio del flusso";
            key = runtimeService.getVariable(event.getExecutionId(), "key", String.class);

        } else if (event.getType() == ActivitiEventType.TASK_COMPLETED) {
            // task = get task from event
            ActivitiEntityWithVariablesEvent entityEvent = (ActivitiEntityWithVariablesEvent) event;
            TaskEntity task = (TaskEntity) entityEvent.getEntity();
            path = runtimeService.getVariable(event.getExecutionId(), "pathFascicoloDocumenti", String.class);
            taskId = task.getId();
            taskName = task.getName();
            key = runtimeService.getVariable(event.getExecutionId(), "key", String.class);

        } else {
            throw new BpmnError("400", "Questo listener (AddFlowsAttachmentsListener) supporta solo eventi di tipo PROCESS_STARTED e TASK_COMPLETED");
        }

        processVariables.keySet().stream()
                .filter(name -> name.endsWith("_aggiorna"))
                .filter(name -> "true".equals(processVariables.get(name)))
                .map(name -> name.replace("_aggiorna", ""))
                .forEach(fileName -> {

                    FlowsAttachment att = (FlowsAttachment) processVariables.get(fileName); // att puo' essere null, nel qual caso verra' creato un nuovo att
                    att = flowsAttachmentService.extractSingleAttachment(att, processVariables, taskId, taskName, key, fileName, path);
                    if (att != null)
                        attachments.put(fileName, att);

                    processVariables.keySet().stream()
                            .filter(variableName -> variableName.startsWith(fileName+"_"))
                            .forEach(variableName -> runtimeService.removeVariable(event.getExecutionId(), variableName));

                    runtimeService.setVariable(event.getExecutionId(), fileName, att);
                });

        String protocolliUniti = flowsAttachmentService.mergeProtocolli(attachments, taskId);
        runtimeService.setVariable(event.getExecutionId(), NUMERI_PROTOCOLLO, protocolliUniti);
    }

    /**
     * Path nella forma
     * Comunicazioni al CNR/flows/acquisti/000411/2019/Acquisti-2019-16
     *
     * Se il cdsuo non e' presente, verra' saltato
     *
     * @param processInstanceId
     * @return
     */
    private String getDefaultPathFascicoloDocumenti(String processInstanceId) {

        String path = runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class);
        if ( path != null ) {
            return path;
        } else {

            String processDefinitionId = runtimeService.getVariable(processInstanceId, "processDefinitionId", String.class).split(":")[0];
            String key = runtimeService.getVariable(processInstanceId, "key", String.class);

            Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

            String profile = "flows";
            if (activeProfiles.contains("dev")) {
                profile = "flows-dev";
            }
            if (activeProfiles.contains("test")) {
                profile = "flows-test";
            }
            if (activeProfiles.contains("demo")) {
                profile = "flows-demo";
            }

            String cdsuo = null;
            if (activeProfiles.contains("cnr")) {
                String idStruttura = runtimeService.getVariable(processInstanceId, ID_STRUTTURA, String.class);
                cdsuo = Optional.ofNullable(idStruttura)
                        .map(id -> aceBridgeService.getUoById(Integer.parseInt(id)).getCdsuo())
                        .orElse(null);
            }

            String anno = key.split("-")[1];

            path = Stream.of("/Comunicazioni al CNR", profile, processDefinitionId, cdsuo, anno, key)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("/"));

            LOGGER.debug("Path calcolato per il flusso " + key + ": " + path);

            return path;
        }

    }
    /**
     * martin
     *
     * In questo caso se c'e' un errore TUTTA la transazione deve fallire,
     * senno' ci ritroviamo con stato inconsistente
     *
     * @return true
     */
    @Override
    public boolean isFailOnException() {
        return true;
    }
}
