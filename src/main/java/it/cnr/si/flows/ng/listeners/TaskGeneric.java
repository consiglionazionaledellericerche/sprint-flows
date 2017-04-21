package it.cnr.si.flows.ng.listeners;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.si.flows.ng.resource.FlowsProcessInstanceResource;

public class TaskGeneric implements ExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		String nomeVariabileScelta = "sceltaUtente";
		LOGGER.info("TaskGeneric - processId: " + execution.getId());
		LOGGER.info("TaskGeneric - sceltaUtente: " + execution.getVariable(nomeVariabileScelta));
		LOGGER.info("TaskGeneric - valore: " + execution.getVariable("valore"));
		Integer currentValue = (Integer.parseInt(execution.getVariable("valore").toString()));
		Integer nextValue = new Integer(currentValue.intValue() + 1);
		execution.setVariable("valore: ", nextValue);
		LOGGER.info("valore corrente: " + nextValue);
	}

}