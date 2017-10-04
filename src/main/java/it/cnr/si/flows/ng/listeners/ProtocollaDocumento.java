package it.cnr.si.flows.ng.listeners;


import it.cnr.si.flows.ng.dto.FlowsAttachment;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Protocollo;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Protocollato;

@Component
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
        att.setAzione(Protocollo);
        att.addStato(Protocollato);
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
