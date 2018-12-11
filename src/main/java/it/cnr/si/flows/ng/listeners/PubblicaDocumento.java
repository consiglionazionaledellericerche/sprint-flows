package it.cnr.si.flows.ng.listeners;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;

@Component
public class PubblicaDocumento implements ExecutionListener {

    private static final long serialVersionUID = -56001764662303256L;

    private static final Logger LOGGER = LoggerFactory.getLogger(PubblicaDocumento.class);
    
    @Inject
    private FlowsAttachmentService attachmentService;
    
    private Expression nomeFileDaPubblicare;
    private Expression fileDaPubblicareFlag;


    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if (nomeFileDaPubblicare.getValue(execution) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaPubblicare' nella process definition (nel Task Listener - Fields).");
        if (fileDaPubblicareFlag.getValue(execution) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'fileDaPubblicareFlag' nella process definition (nel Task Listener - Fields).");

        String nomeVariabileFile = (String) nomeFileDaPubblicare.getValue(execution);
        Boolean flagPubblicazione =  Boolean.parseBoolean((String) fileDaPubblicareFlag.getValue(execution));
        attachmentService.setPubblicabileTrasparenza(execution.getId(), nomeVariabileFile, flagPubblicazione);
    }

}
