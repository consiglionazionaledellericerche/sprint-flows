package it.cnr.si.flows.ng.listeners;

import java.util.Arrays;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.si.flows.ng.resource.FlowsProcessInstanceResource;

public class SettaMultiInstanceCollection implements ExecutionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessInstanceResource.class);
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		// TODO Auto-generated method stub
		LOGGER.info("processId: " + execution.getId());

		String collectionArray[] = new String[]{"User1","User2","User3"};

		List collectionList = Arrays.asList(collectionArray);

		// printing the list
		System.out.println("The list is:" + collectionList);

		LOGGER.info("collectionList: " + collectionList);
		execution.setVariable("listaCollection", collectionList);	
	}

}
