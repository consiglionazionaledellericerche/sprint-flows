package it.cnr.si.flows.ng.listeners;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.resource.FlowsProcessInstanceResource;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskPredisposizioneDecisione implements ExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		String nomeVariabileScelta = "sceltaUtente";
		LOGGER.info(" - processId: " + execution.getId());
		LOGGER.info("TaskPredisposizioneDecisione - sceltaUtente: " + execution.getVariable(nomeVariabileScelta));
		LOGGER.info("TaskPredisposizioneDecisione - valore: " + execution.getVariable("valore"));
        LOGGER.info("TaskPredisposizioneDecisione - descrizione: " + execution.getVariable("descrizione"));
		LOGGER.info("TaskPredisposizioneDecisione - priorita: " + execution.getVariable("priorita"));
		LOGGER.info("TaskPredisposizioneDecisione - commento: " + execution.getVariable("commento"));
        Integer currentValue = (Integer.valueOf(execution.getVariable("valore").toString()));
        Integer nextValue = currentValue + 1;
		execution.setVariable("valore: ", nextValue);
        LOGGER.info("valore corrente: {}", nextValue);

        FlowsAttachment decisioneAContrattare = (FlowsAttachment) execution.getVariable("decisioneContrattare[0]");
		decisioneAContrattare.setMetadato(FlowsAttachment.PUBBLICAZIONE_FLAG, true);
    }
}