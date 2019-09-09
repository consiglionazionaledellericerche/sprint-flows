package it.cnr.si.flows.ng.service;

import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Firma;
import static it.cnr.si.flows.ng.utils.Enum.StatoAcquisti.Firmato;


@Service
public class FirmaDocumentoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FirmaDocumentoService.class);

    @Inject
    private FlowsFirmaService flowsFirmaService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;

    public void eseguiFirma(DelegateExecution execution, String nomeVariabileFile) {

        if (nomeVariabileFile == null)
            throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaFirmare' nella process definition (nel Task Listener - Fields).");
        if (execution.getVariable("sceltaUtente") != null &&
            !"Firma Multipla".equals(execution.getVariable("sceltaUtente")) &&
            "Firma".equals(execution.getVariable("sceltaUtente")) ) {

            String stringaOscurante = "******";
            // TODO: validare presenza di queste tre variabili
            String username = (String) execution.getVariable("username");
            String password = (String) execution.getVariable("password");
            String otp = (String) execution.getVariable("otp");
            String textMessage = "";

            FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
            byte[] bytes = flowsAttachmentService.getAttachmentContentBytes(att);

            try {
                byte[] bytesfirmati = flowsFirmaService.firma(username, password, otp, bytes);
                att.setFilename(getSignedFilename(att.getFilename()));
                att.setAzione(Firma);
                att.addStato(Firmato);
                //setto l`username dell`utente che sta eseguendo la firma e la data
                att.setUsername(SecurityUtils.getCurrentUserLogin());
                att.setTime(new Date());

                flowsAttachmentService.saveAttachment(execution, nomeVariabileFile, att, bytesfirmati);
                execution.setVariable("otp", stringaOscurante);
                execution.setVariable("password", stringaOscurante);
            } catch (ArubaSignServiceException e) {
                LOGGER.error("FIRMA NON ESEGUITA", e);
                if (e.getMessage().indexOf("error code 0001") != -1) {
                    textMessage = "controlla il formato del file sottoposto alla firma<br>";
                } else if(e.getMessage().indexOf("error code 0003") != -1) {
                    textMessage = "CREDENZIALI ERRATE<br>";
                } else if(e.getMessage().indexOf("error code 0004") != -1) {
                    textMessage = "PIN ERRATO<br>";
                } else {
                    textMessage = "errore generico<br>";
                }
                throw new BpmnError("500", "<b>FIRMA NON ESEGUITA<br>" + textMessage + "</b>");
            }
        }
    }

    public static String getSignedFilename(String filename) {
        String result = filename.substring(0, filename.lastIndexOf('.'));
        result += ".signed";
        result += filename.substring(filename.lastIndexOf('.'));
        return result;
    }

}
