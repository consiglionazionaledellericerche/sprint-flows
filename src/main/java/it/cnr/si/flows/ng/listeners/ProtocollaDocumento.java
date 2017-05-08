package it.cnr.si.flows.ng.listeners;

import java.util.Date;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.FirmaService;

public class ProtocollaDocumento implements ExecutionListener {

    private static final long serialVersionUID = -56001764662303256L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocollaDocumento.class);

    private Expression nomeFileDaProtocollare;


    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if (!execution.getEventName().equals(ExecutionListener.EVENTNAME_TAKE))
            throw new IllegalStateException("Questo Listener accetta solo eventi 'take'.");
        if (nomeFileDaProtocollare.getValue(execution) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaProtocollare' nella process definition (nel Task Listener - Fields).");

        String nomeVariabileFile = (String) nomeFileDaProtocollare.getValue(execution);
        String valoreNumeroProtocollo = (String) execution.getVariable("numeroProtocolloInput");
        String valoreDataProtocollo = (String) execution.getVariable("dataProtocolloInput");


        FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
        att.setAzione(FlowsAttachment.Azione.Protocollo);
        att.addStato(FlowsAttachment.Stato.Protocollato);
		att.setMetadato("numeroProtocollo", valoreNumeroProtocollo);
        att.setMetadato("dataProtocollo", valoreDataProtocollo);
        execution.setVariable(nomeVariabileFile, att);
        execution.setVariable("numeroProtocollo_" + nomeVariabileFile, valoreNumeroProtocollo);
        execution.setVariable("dataProtocollo_" + nomeVariabileFile, valoreDataProtocollo);

        String isError = (String) execution.getVariable("error");
        if ("true".equals(isError)) {
            throw new IllegalStateException("L'utente ha selezionato l'opzione errore. Rollback.");
        }

    }

}
