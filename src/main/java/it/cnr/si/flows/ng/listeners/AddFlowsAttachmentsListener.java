package it.cnr.si.flows.ng.listeners;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import org.activiti.bpmn.converter.export.BPMNDIExport;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityWithVariablesEventImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.cnr.si.flows.ng.service.FlowsAttachmentService.NUMERI_PROTOCOLLO;

@Component
public class AddFlowsAttachmentsListener implements ActivitiEventListener {

    private final Logger LOGGER = LoggerFactory.getLogger(VisibilitySetter.class);

    @Inject
    private RuntimeService runtimeService;
    @Inject
    private TaskService taskService;
    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private Environment env;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;

    @Override
    public void onEvent(ActivitiEvent event) {
        LOGGER.info("Sono in AddFlowsAttachmentsListener con evento "+ event.getType() + event);

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
            path = runtimeService.getVariable(event.getExecutionId(), "pathFascicoloDocumenti", String.class);
            // taskId = get taskId from event
            taskId = (runtimeService.getVariable(event.getExecutionId(), "taskId", String.class));
            taskName = taskService.createTaskQuery().taskId(taskId).singleResult().getName();
            key = runtimeService.getVariable(event.getExecutionId(), "key", String.class);

        } else {
            throw new BpmnError("400", "Questo listener (AddFlowsAttachmentsListener) supporta solo eventi di tipo PROCESS_STARTED e TASK_COMPLETED");
        }

        List<String> fileNames = processVariables.keySet().stream()
                .filter(name -> name.endsWith("_aggiorna"))
                .filter(name -> "true".equals(processVariables.get(name)))
                .map(name -> name.replace("_aggiorna", ""))
                .collect(Collectors.toList());

        try {
            for (String fileName : fileNames) {

                FlowsAttachment att = flowsAttachmentService.extractSingleAttachment(processVariables, taskId, taskName, key, fileName, path);
                if (att != null)
                    attachments.put(fileName, att);
                processVariables.keySet().stream()
                        .filter(variableName -> variableName.startsWith(fileName+"_"))
                        .forEach(variableName -> runtimeService.removeVariable(event.getExecutionId(), variableName));

                runtimeService.setVariable(event.getExecutionId(), fileName, att);
            }
        } catch (IOException e) {
            throw new BpmnError("500", "Errore nel processamento dei files");
        }

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

        String idStruttura = runtimeService.getVariable(processInstanceId, "idStruttura", String.class);
        String cdsuo = Optional.ofNullable(idStruttura)
                .map(id -> aceBridgeService.getUoById(Integer.parseInt(id)).getCdsuo())
                .orElse(null);

        String anno = key.split("-")[1];

        String path = Stream.of("/Comunicazioni al CNR", profile, processDefinitionId, cdsuo, anno, key)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("/"));

        LOGGER.debug("Path calcolato per il flusso "+ key +": "+ path);

        return path;

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
