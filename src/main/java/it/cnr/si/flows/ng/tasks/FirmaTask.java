package it.cnr.si.flows.ng.tasks;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.FirmaService;

public class FirmaTask implements ExecutionListener {

    private static final long serialVersionUID = -56001764662303256L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmaTask.class);

    private FirmaService firmaService = new FirmaService();
    private Expression nomeFileDaFirmare;

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        if (!execution.getEventName().equals(ExecutionListener.EVENTNAME_TAKE))
            throw new IllegalStateException("Questo Listener accetta solo eventi 'take'.");
        if (nomeFileDaFirmare.getValue(execution) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaFirmare' nella process definition (nel Task Listener - Fields).");

        String nomeVariabileFile = (String) nomeFileDaFirmare.getValue(execution);

        String username = (String) execution.getVariable("username");
        String password = (String) execution.getVariable("password");
        String otp = (String) execution.getVariable("otp");

        FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
        byte[] bytes = att.getBytes();

        try {
            byte[] bytesfirmati = firmaService.firma(username, password, otp, bytes);
            att.setBytes(bytesfirmati);
            att.setFilename(getSignedFilename(att.getFilename()));
            att.setAzione(FlowsAttachment.Azione.Firma);
            att.addStato(FlowsAttachment.Stato.Firmato);

            execution.setVariable(nomeVariabileFile, att);

        } catch (ArubaSignServiceException e) {
            LOGGER.error("firma non riuscita", e);
            throw new TaskFailedException(e);
        }

        String isError = (String) execution.getVariable("error");
        if ("true".equals(isError)) {
            throw new IllegalStateException("L'utente ha selezionato l'opzione errore. Rollback.");
        }

    }


    private static String getSignedFilename(String filename) {
        String result = filename.substring(0, filename.lastIndexOf('.'));
        result += ".signed";
        result += filename.substring(filename.lastIndexOf('.'));
        return result;
    }

}
