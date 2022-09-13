package it.cnr.si.flows.ng.service;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Firma;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Firmato;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.cnr.si.firmadigitale.firma.arss.ArubaSignServiceException;
import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;
import it.cnr.si.firmadigitale.firma.arss.stub.SignReturnV2;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.service.SecurityService;



@Service
public class FirmaDocumentoService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FirmaDocumentoService.class);

	@Autowired(required = false)
	private FlowsFirmaService flowsFirmaService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private SecurityService securityService;

	public void eseguiFirma(DelegateExecution execution, String nomeVariabileFile, PdfSignApparence apparence) {

		String currentUser = securityService.getCurrentUserLogin();
		LOGGER.info("L'utente {} sta firmando il file {}", currentUser, nomeVariabileFile);

		List<String> nomiVariabiliFile = new ArrayList<String>();
		nomiVariabiliFile.add(nomeVariabileFile);
		eseguiFirma(execution, nomiVariabiliFile, null);

	}

	public void eseguiFirma(DelegateExecution execution, List<String> nomiVariabiliFile, PdfSignApparence apparence) {

		TaskService taskService = execution.getEngineServices().getTaskService();

		if (nomiVariabiliFile == null)
			throw new IllegalStateException("Questo Listener ha bisogno di almeno un campo 'nomiVariabiliFile' nella process definition (nel Task Listener - Fields).");
		if (execution.getVariable("sceltaUtente") != null &&
				!"Firma Multipla".equals(execution.getVariable("sceltaUtente")) &&
				"Firma".equals(execution.getVariable("sceltaUtente")) ) {

			String stringaOscurante = "******";
			// TODO: validare presenza di queste tre variabili
			String username = (String) execution.getVariable("username");
			String password = (String) execution.getVariable("password");
			String otp = (String) execution.getVariable("otp");
			String textMessage = "";

			for (int i = 0; i < nomiVariabiliFile.size(); i++) {
				String nomeVariabileFile = nomiVariabiliFile.get(i);
				FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
				byte[] bytes = flowsAttachmentService.getAttachmentContentBytes(att);

				try {
					byte[] bytesfirmati = flowsFirmaService.firma(username, password, otp, bytes, apparence);
					att.setFilename(getSignedFilename(att.getFilename()));
					att.setAzione(Firma);
					att.addStato(Firmato);
					//setto l`username dell`utente che sta eseguendo la firma e la data
					att.setUsername(securityService.getCurrentUserLogin());
					att.setTime(new Date());

					flowsAttachmentService.saveAttachment(execution, nomeVariabileFile, att, bytesfirmati);

					String taskId = execution.getVariable("taskId", String.class);
					taskService.setVariable(taskId, "otp", stringaOscurante);
					taskService.setVariable(taskId, "password", stringaOscurante);
					execution.setVariable("otp", stringaOscurante);
					execution.setVariable("password", stringaOscurante);
				} catch (ArubaSignServiceException e) {
					LOGGER.error("FIRMA NON ESEGUITA", e);
					if (e.getMessage().indexOf("error code 0001") != -1) {
						textMessage = "-- errore generico --"
								+ "<br>- veirificare che l'estensione del file sia di tipo PDF"
								+ "<br>- veirificare la corretta digitazione del codice OTP"
								+ "<br>se il problema persiste"
								+ "<br>provare a risincronizzare il dispositivo OTP"
								+ "<br>seguendo le istruzioni presenti nella pagina"
								+ "<br>Manualistica&Faq<br>";
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
	}


	public void eseguiFirmaMultipla(DelegateExecution execution, List<String> nomiVariabiliFile, PdfSignApparence apparence) {

		TaskService taskService = execution.getEngineServices().getTaskService();

		if (nomiVariabiliFile == null)
			throw new IllegalStateException("Questo Listener ha bisogno di almeno un campo 'nomiVariabiliFile' nella process definition (nel Task Listener - Fields).");
		if (execution.getVariable("sceltaUtente") != null &&
				!"Firma Multipla".equals(execution.getVariable("sceltaUtente")) &&
				"Firma".equals(execution.getVariable("sceltaUtente")) ) {

			String stringaOscurante = "******";
			// TODO: validare presenza di queste tre variabili
			String username = (String) execution.getVariable("username");
			String password = (String) execution.getVariable("password");
			String otp = (String) execution.getVariable("otp");
			String textMessage = "";
			List<byte[]> bytes = new ArrayList();
			List<FlowsAttachment> att = new ArrayList();

			for (int i = 0; i < nomiVariabiliFile.size(); i++) {
				String nomeVariabileFile = nomiVariabiliFile.get(i);
				att.add((FlowsAttachment) execution.getVariable(nomeVariabileFile));
				//Check se il fil è psf
				if (!att.get(i).getMimetype().toString().equals("application/pdf")) {
					textMessage = "-- errore formato del file --"
							+ "<br>Almeno uno dei file per la firma"
							+ "<br>non risulta essere in formato PDF<br>";
					throw new BpmnError("500", "<b>FIRMA NON ESEGUITA<br>" + textMessage + "</b>");
				}
				//fine check
				byte[] byteSingle = flowsAttachmentService.getAttachmentContentBytes(att.get(i));
				bytes.add(flowsAttachmentService.getAttachmentContentBytes(att.get(i)));
			}
			try {
				List<SignReturnV2> bytesMultiplifirmati = flowsFirmaService.firmaMultipla(username, password, otp, bytes, apparence);
				for (int i = 0; i < bytesMultiplifirmati.size(); i++) {

					att.get(i).setFilename(getSignedFilename(att.get(i).getFilename()));
					att.get(i).setAzione(Firma);
					att.get(i).addStato(Firmato);
					//setto l`username dell`utente che sta eseguendo la firma e la data
					att.get(i).setUsername(securityService.getCurrentUserLogin());
					att.get(i).setTime(new Date());

					flowsAttachmentService.saveAttachment(execution, nomiVariabiliFile.get(i), att.get(i), bytesMultiplifirmati.get(i).getBinaryoutput());

					String taskId = execution.getVariable("taskId", String.class);
					taskService.setVariable(taskId, "otp", stringaOscurante);
					taskService.setVariable(taskId, "password", stringaOscurante);
					execution.setVariable("otp", stringaOscurante);
					execution.setVariable("password", stringaOscurante);
				}
			} catch (ArubaSignServiceException e) {
				LOGGER.error("FIRMA NON ESEGUITA", e);
				if (e.getMessage().indexOf("error code 0001") != -1) {
					textMessage = "-- errore generico --"
							+ "<br>- veirificare che l'estensione del file sia di tipo PDF"
							+ "<br>- veirificare la corretta digitazione del codice OTP"
							+ "<br>se il problema persiste"
							+ "<br>provare a risincronizzare il dispositivo OTP"
							+ "<br>seguendo le istruzioni presenti nella pagina"
							+ "<br>Manualistica&Faq<br>";
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



