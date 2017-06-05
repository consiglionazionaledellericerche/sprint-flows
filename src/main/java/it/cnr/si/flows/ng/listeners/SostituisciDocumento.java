package it.cnr.si.flows.ng.listeners;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;

public class SostituisciDocumento implements ExecutionListener {

    private static final long serialVersionUID = -56001764662303256L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SostituisciDocumento.class);

    private Expression nomeFileDaSostituire;


    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if (!execution.getEventName().equals(ExecutionListener.EVENTNAME_TAKE))
            throw new IllegalStateException("Questo Listener accetta solo eventi 'take'.");
        if (nomeFileDaSostituire.getValue(execution) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaSostituire' nella process definition (nel Task Listener - Fields).");

        String nomeVariabileFile = (String) nomeFileDaSostituire.getValue(execution);



        FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);

        Date date = new Date() ;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss") ;
        String newFileName = nomeVariabileFile + "-" + dateFormat.format(date);
        att.setName(newFileName);
        FlowsAttachment attNew = (FlowsAttachment) execution.getVariable(newFileName);
        attNew.setAzione(FlowsAttachment.Azione.Sostituzione);
        attNew.addStato(FlowsAttachment.Stato.Sostituito);
        execution.setVariable(newFileName, attNew);

        att.clearStato();
        att.setName(nomeVariabileFile);
        execution.setVariable(nomeVariabileFile, att);
    }

}
