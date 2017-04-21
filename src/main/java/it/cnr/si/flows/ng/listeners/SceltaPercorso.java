package it.cnr.si.flows.ng.listeners;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.si.flows.ng.resource.FlowsProcessInstanceResource;

public class SceltaPercorso implements ExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		String nomeVariabileScelta = "sceltaPercorso";
		LOGGER.info("SceltaPercorso - processId: " + execution.getId());
		LOGGER.info("SceltaPercorso - sceltaPercorso: " + execution.getVariable(nomeVariabileScelta));
		execution.setVariable(nomeVariabileScelta, "Revoca");
		LOGGER.info("SceltaPercorso - sceltaPercorso: " + execution.getVariable(nomeVariabileScelta));
	}

}