package it.cnr.si.flows.ng.listeners;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.FlowsFirmaService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;

@Component
public class FirmaDocumento implements ExecutionListener {

	private static final long serialVersionUID = -56001764662303256L;

	private static final Logger LOGGER = LoggerFactory.getLogger(FirmaDocumento.class);

	@Autowired
	private FlowsFirmaService firmaService;
	@Autowired
	private FlowsAttachmentService attachmentService;


	private Expression nomeFileDaFirmare;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		if (!execution.getEventName().equals(ExecutionListener.EVENTNAME_TAKE))
			throw new IllegalStateException("Questo Listener accetta solo eventi 'take'.");
		if (nomeFileDaFirmare.getValue(execution) == null)
			throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaFirmare' nella process definition (nel Task Listener - Fields).");

		String nomeVariabileFile = (String) nomeFileDaFirmare.getValue(execution);
		String stringaOscurante = "******";
		// TODO: validare presenza di queste tre variabili
		String username = (String) execution.getVariable("username");
		String password = (String) execution.getVariable("password");
		String otp = (String) execution.getVariable("otp");
		String textMessage = "";

		FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
		byte[] bytes = att.getBytes();

		try {
			byte[] bytesfirmati = firmaService.firma(username, password, otp, bytes);
			att.setBytes(bytesfirmati);
			att.setFilename(getSignedFilename(att.getFilename()));
			att.setAzione(FlowsAttachment.Azione.Firma);
			att.addStato(FlowsAttachment.Stato.Firmato);

			attachmentService.saveAttachment(execution, nomeVariabileFile, att);
			execution.setVariable("otp", stringaOscurante);
			execution.setVariable("password", stringaOscurante);
		} catch (ArubaSignServiceException e) {
			LOGGER.error("firma non riuscita", e);
			if (e.getMessage().indexOf("error code 0001") != -1) {
				textMessage = "controlla il formato del file sottopsto alla firma";
			} else if(e.getMessage().indexOf("error code 0003") != -1) {
				textMessage = "Errore in fase di verifica delle credenziali";
			} else if(e.getMessage().indexOf("error code 0004") != -1) {
				textMessage = "Errore nel PIN";
			} else {
				textMessage = "errore generico";
			}
			throw new BpmnError("500", "firma non riuscita - " + textMessage);
		}

	}


	private static String getSignedFilename(String filename) {
		String result = filename.substring(0, filename.lastIndexOf('.'));
		result += ".signed";
		result += filename.substring(filename.lastIndexOf('.'));
		return result;
	}

}
