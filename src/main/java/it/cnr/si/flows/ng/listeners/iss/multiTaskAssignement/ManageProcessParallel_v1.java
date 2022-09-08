package it.cnr.si.flows.ng.listeners.iss.multiTaskAssignement;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.EngineServices;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.utils.SecurityUtils;

@Component
@Profile("iss")
public class ManageProcessParallel_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessParallel_v1.class);
	private Expression faseEsecuzione;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		
		String currentUser = SecurityUtils.getCurrentUserLogin();
		String processInstanceId =  execution.getProcessInstanceId();
		String executionId =  execution.getId();
		String stato =  execution.getCurrentActivityName();
		
		String faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		
		LOGGER.info("ProcessInstanceId: "+processInstanceId+" Current User: "+currentUser+" executionId: "+executionId+" stato: "+stato+" fase esecuzione: "+faseEsecuzioneValue);
		
		switch(faseEsecuzioneValue){  
			// prepare list at the end of the previously task
			case "process-prepareVariableNextTask": {
				LOGGER.info("process-prepareVariableNextTask phase started at the END of the task");
				
				//convert json assignee list in Arraylist for assing task for every element of the list		
				String assigneeListJson = (String)execution.getVariable("assigneeList_json");
				List<String> assigneeList = assembleAssigneeList(assigneeListJson);
		        execution.setVariable("assigneeList", assigneeList);
				
				LOGGER.info("proposeListCandidate: "+ execution.getVariable("assigneeList"));

			};
			break;
			default: 
				LOGGER.info("fase esecuzione non gestita: "+faseEsecuzioneValue);				
			break;
		} 
	}

	private List<String> assembleAssigneeList(String jsonList){
		List<String> assigneeList= new ArrayList<String>();
		if(jsonList != null) {
			JSONArray assignees = new JSONArray(jsonList);
			for ( int i = 0; i < assignees.length(); i++) {
				JSONObject assigne = assignees.getJSONObject(i);
				try {
					assigneeList.add( assigne.getString("assegna"));			            	
				} catch (JSONException e) {
					LOGGER.error("Formato Non Valido");			               
				}
			}
		}
		return assigneeList;
	}
}
