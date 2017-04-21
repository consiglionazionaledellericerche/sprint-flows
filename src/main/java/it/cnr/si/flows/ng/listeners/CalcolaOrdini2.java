package it.cnr.si.flows.ng.listeners;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.si.flows.ng.resource.FlowsProcessInstanceResource;

public class CalcolaOrdini2 implements ExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		LOGGER.info("processId: " + execution.getId());
		Integer currentValue = (Integer) execution.getVariable("valore2");
		Integer nextValue = new Integer(currentValue.intValue() - 1);
		LOGGER.info("valore corrente: " + nextValue);
		execution.setVariable("valore2", nextValue);
	}

}
