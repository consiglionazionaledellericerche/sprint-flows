package it.cnr.si.flows.ng.listeners.oiv;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;



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
            String timerName = ((TimerEntity) job).getJobHandlerConfiguration()
                    .split(":")[1]
                    .replace("\"", "")
                    .replace("}", "");
            LOGGER.debug("getDuedate {}, getId {}, TimerDeclarationImpl {}", job.getDuedate(), job.getId(), timerName);
            if (timerName.equals("boundarytimer3")) {
                LOGGER.debug("--- CAMBIO DATA Duedate: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
            	//job.wait().
                Date date3000 = new GregorianCalendar(3000, Calendar.FEBRUARY, 11).getTime();
            	((TimerEntity) job).setDuedate(date3000);
                LOGGER.debug("--- NUOVO Duedate: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
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
