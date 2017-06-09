package it.cnr.si.flows.ng.listeners;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.spi.impl.operationexecutor.classic.ClassicOperationExecutor;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;

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

        LOGGER.debug("Ricarico il file {} originale, ma pulito degli stati", nomeVariabileFile);
        originale.clearStato();
        execution.setVariable(nomeVariabileFile, originale);

        LOGGER.debug("Salvo una copia tra gli allegati per futuro riferimento");
        copia.setAzione(FlowsAttachment.Azione.Sostituzione);
        copia.addStato(FlowsAttachment.Stato.Sostituito);
        copia.setName("Provvedimento di Aggiudicazione Sostiutito");
        int nextIndex = attachmentService.getNextIndex(executionId, "allegati", new HashMap<>());
        execution.setVariable("allegati["+ nextIndex +"]", copia);


//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss") ;
//        String newFileName = nomeVariabileFile + "-" + dateFormat.format(date);
//        originale.setName(newFileName);
//        originale.clearStato();
//        originale.setName(nomeVariabileFile);
//        execution.setVariable(nomeVariabileFile, originale);
//
//        copia.setAzione(FlowsAttachment.Azione.Sostituzione);
//        copia.addStato(FlowsAttachment.Stato.Sostituito);
//        copia.setName("Provvedimento di Aggiudicazione Sostiutito");
//        execution.setVariable(newFileName, copia);
    }

}
