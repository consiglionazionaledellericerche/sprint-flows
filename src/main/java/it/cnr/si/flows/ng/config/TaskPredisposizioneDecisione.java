package it.cnr.si.flows.ng.config;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.resource.FlowsProcessInstanceResource;

public class TaskPredisposizioneDecisione implements ExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		String nomeVariabileScelta = "sceltaUtente";
		LOGGER.info(" - processId: " + execution.getId());
		LOGGER.info("TaskPredisposizioneDecisione - sceltaUtente: " + execution.getVariable(nomeVariabileScelta));
		LOGGER.info("TaskPredisposizioneDecisione - valore: " + execution.getVariable("valore"));
		LOGGER.info("TaskPredisposizioneDecisione - oggettoAcquisizione: " + execution.getVariable("oggettoAcquisizione"));
		LOGGER.info("TaskPredisposizioneDecisione - descrizioneAcquisizione: " + execution.getVariable("descrizioneAcquisizione"));
		LOGGER.info("TaskPredisposizioneDecisione - titolo: " + execution.getVariable("titolo"));
		LOGGER.info("TaskPredisposizioneDecisione - priorita: " + execution.getVariable("priorita"));
		LOGGER.info("TaskPredisposizioneDecisione - commento: " + execution.getVariable("commento"));
		Integer currentValue = (Integer.parseInt(execution.getVariable("valore").toString()));
		Integer nextValue = new Integer(currentValue.intValue() + 1);
		execution.setVariable("valore: ", nextValue);
		LOGGER.info("valore corrente: " + nextValue);
		
		FlowsAttachment decisioneAContrattare = (FlowsAttachment) execution.getVariable("decisioneContrattare[0]");
		decisioneAContrattare.setMetadato(FlowsAttachment.PUBBLICAZIONE_FLAG, true);
		
	}

}