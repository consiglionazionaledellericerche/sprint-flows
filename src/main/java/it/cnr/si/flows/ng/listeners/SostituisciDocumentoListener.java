package it.cnr.si.flows.ng.listeners;

import java.util.Date;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.dto.FlowsAttachment.Azione;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;

@Component
public class SostituisciDocumentoListener implements ExecutionListener {

    private static final long serialVersionUID = -56001764662303256L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SostituisciDocumentoListener.class);

    private Expression nomeFileDaSostituire;

    @Inject
    private FlowsAttachmentService attachmentService;

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if (!execution.getEventName().equals(ExecutionListener.EVENTNAME_TAKE))
            throw new IllegalStateException("Questo Listener accetta solo eventi 'take'.");
        if (nomeFileDaSostituire.getValue(execution) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaSostituire' nella process definition (nel Task Listener - Fields).");

        String executionId = execution.getId();

        String nomeVariabileFile = (String) nomeFileDaSostituire.getValue(execution);

        FlowsAttachment originale = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
        FlowsAttachment copia     = SerializationUtils.clone(originale);

        LOGGER.debug("Ricarico il file {} originale, ma con gli stati puliti", nomeVariabileFile);
        originale.clearStato();
        originale.setAzione(Azione.Sostituzione);
        originale.setTaskId(null);
        originale.setTaskName(null);
        originale.setTime(new Date());
        execution.setVariable(nomeVariabileFile, originale);

        LOGGER.debug("Salvo una copia per futuro riferimento");
        copia.setAzione(FlowsAttachment.Azione.Sostituzione);
        copia.addStato(FlowsAttachment.Stato.Sostituito);
        copia.setTaskId(null);
        copia.setTaskName(null);
        copia.setTime(new Date());
        copia.setName("Provvedimento di Aggiudicazione Sostiutito");
        int nextIndex = attachmentService.getNextIndexByProcessInstanceId(executionId, "provvedimentiRespinti");
        execution.setVariable("provvedimentiRespinti["+ nextIndex +"]", copia);

    }

}
