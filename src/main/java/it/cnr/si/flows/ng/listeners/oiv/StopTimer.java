package it.cnr.si.flows.ng.listeners.oiv;

import java.util.List;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.activiti.engine.impl.jobexecutor.TimerDeclarationImpl;



@Component
public class StopTimer implements ExecutionListener {
    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StopTimer.class);


    @Override
    public void notify(DelegateExecution execution) throws Exception {

        String processInstanceId = execution.getProcessInstanceId();
        List<Job> timerJobs = execution.getEngineServices().getManagementService().createJobQuery()
             	.processInstanceId(processInstanceId)
             	.timers()
             	.list();
        
        for(Job job : timerJobs)
        {
        	Execution jobExecution = execution.getEngineServices().getRuntimeService().createExecutionQuery()
       	 	.executionId(job.getExecutionId()).singleResult();
        	String timerDeclarationImplVal = ((TimerDeclarationImpl)job).getJobHandlerConfiguration().toString();
            LOGGER.debug("getDuedate {}, getId {}, TimerDeclarationImpl {}", job.getDuedate(), job.getId(), timerDeclarationImplVal);
            if (timerDeclarationImplVal.contains("boundarytimer4")) {
                LOGGER.debug("--- trovatogetDuedate {}, getId {}, timerDeclarationImplVal{}", job.getDuedate(), job.getId(), timerDeclarationImplVal);
            	job.wait();
            }
        }
        JobQuery jobQuery =  execution.getEngineServices().getManagementService().createJobQuery().processInstanceId(processInstanceId);
      
//        execution.getEngineServices().getRuntimeService().Job.getExecutionId().
//        execution.getEngineServices().getDynamicBpmnService().;
//        execution.getEngineServices().getRepositoryService().;
//        execution.getEngineServices().getRuntimeService().);
//        execution.getEngineServices().getManagementService().;

    }
}
