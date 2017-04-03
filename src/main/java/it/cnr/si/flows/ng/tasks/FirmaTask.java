package it.cnr.si.flows.ng.tasks;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.FirmaService;

public class FirmaTask implements TaskListener {

    private static final long serialVersionUID = -56001764662303256L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmaTask.class);

    private FirmaService firmaService = new FirmaService();

    private Expression nomeFileDaFirmare;

    @Override
    public void notify(DelegateTask delegateTask) {

        if (!delegateTask.getEventName().equals(TaskListener.EVENTNAME_COMPLETE))
            throw new IllegalStateException("Questo Listener accetta solo eventi 'complete'.");
        if (nomeFileDaFirmare.getValue(delegateTask) == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaFirmare' nella process definition (nel Task Listener - Fields).");

        String value = (String) nomeFileDaFirmare.getValue(delegateTask);

        String username = (String) delegateTask.getVariable("username");
        String password = (String) delegateTask.getVariable("password");
        String otp = (String) delegateTask.getVariable("otp");

        FlowsAttachment att = (FlowsAttachment) delegateTask.getVariable(value);
        byte[] bytes = att.getBytes();

        try {
            byte[] bytesfirmati = firmaService.firma(username, password, otp, bytes);
            att.setBytes(bytesfirmati);
            att.setFilename(getSignedFilename(att.getFilename()));

            delegateTask.setVariable(value, att);

        } catch (ArubaSignServiceException e) {
            LOGGER.error("firma non riuscita", e);
            throw new TaskFailedException(e);
        }

        String isError = (String) delegateTask.getVariable("error");
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
