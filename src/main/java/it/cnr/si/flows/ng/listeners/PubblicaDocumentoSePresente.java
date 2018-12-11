package it.cnr.si.flows.ng.listeners;

import java.util.Map;

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
public class PubblicaDocumentoSePresente implements ExecutionListener {

	private static final long serialVersionUID = -56001764662303256L;

	private static final Logger LOGGER = LoggerFactory.getLogger(PubblicaDocumentoSePresente.class);

	@Inject
	private FlowsAttachmentService attachmentService;
	private Expression nomeFileDaPubblicare;


	@Override
	public void notify(DelegateExecution execution) throws Exception {


		Map<String, Object> mappaVariabili = execution.getVariables();

		for (Map.Entry<String, Object> entry : mappaVariabili.entrySet()) {
			if (entry.getValue() != null){
				if (entry.getValue() instanceof FlowsAttachment) {
					FlowsAttachment documento = (FlowsAttachment) entry.getValue();
					if	(documento != null && documento.getName().startsWith((String) nomeFileDaPubblicare.getValue(execution))){
						LOGGER.info("pubblico: " + documento.getFilename() + "(" + entry.getKey() + ") per il flusso: " + execution.getId() );
						attachmentService.setPubblicabileTrasparenza(execution.getId(), entry.getKey(), true);
					}
				}				
			}
		}
	}
}

