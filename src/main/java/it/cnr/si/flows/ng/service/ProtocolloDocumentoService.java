package it.cnr.si.flows.ng.service;


import it.cnr.si.flows.ng.dto.FlowsAttachment;

import org.activiti.engine.delegate.DelegateExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Protocollo;
import static it.cnr.si.flows.ng.utils.Enum.StatoAcquisti.Protocollato;

import java.io.IOException;
import java.text.ParseException;

import javax.inject.Inject;

@Service
public class ProtocolloDocumentoService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolloDocumentoService.class);

    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    
	public void protocolla(DelegateExecution execution, String nomeVariabileFile)  throws IOException, ParseException  {

		if (nomeVariabileFile == null)
			throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaProtocollare' nella process definition (nel Task Listener - Fields).");

		String valoreNumeroProtocollo = (String) execution.getVariable("numeroProtocolloInput");
		String valoreDataProtocollo = (String) execution.getVariable("dataProtocolloInput");


		FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
		att.setAzione(Protocollo);
		att.addStato(Protocollato);
		att.setMetadato("numeroProtocollo", valoreNumeroProtocollo);
		att.setMetadato("dataProtocollo", valoreDataProtocollo);
		execution.setVariable(nomeVariabileFile, att);
		//execution.setVariable("numeroProtocollo_" + nomeVariabileFile, valoreNumeroProtocollo);
		//execution.setVariable("dataProtocollo_" + nomeVariabileFile, valoreDataProtocollo);
		att.getMetadati().put("numeroProtocollo", valoreNumeroProtocollo);
        att.getMetadati().put("dataProtocollo", valoreDataProtocollo);
        execution.setVariable("numeriProtocollo", flowsAttachmentService.addProtocollo(execution.getVariable("numeriProtocollo").toString(), valoreNumeroProtocollo));
	}

	// se il numero e data di protocollo sono già variabili con la sintassi  <numeroProtocollo_ + nomeVariabileFile>
	public void protocollaDocumento(DelegateExecution execution, String nomeVariabileFile, String valoreNumeroProtocollo, String valoreDataProtocollo)  throws IOException, ParseException  {

		if (nomeVariabileFile == null)
			throw new IllegalStateException("Questo Listener ha bisogno del campo 'nomeFileDaProtocollare' nella process definition (nel Task Listener - Fields).");

		FlowsAttachment att = (FlowsAttachment) execution.getVariable(nomeVariabileFile);
		att.setAzione(Protocollo);
		att.addStato(Protocollato);
		att.setMetadato("numeroProtocollo", valoreNumeroProtocollo);
		att.setMetadato("dataProtocollo", valoreDataProtocollo);
		execution.setVariable(nomeVariabileFile, att);
	}
}
