package it.cnr.si.flows.ng.listeners.oiv;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
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
public class SetTimer implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SetTimer.class);

	private Expression boundarytimerName;
	private Expression yearAddValue;
	private Expression dayAddValue;
	private Expression monthAddValue;
	private Expression hourAddValue;
	private Expression minuteAddValue;


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
			if ((boundarytimerName.getValue(execution) != null) && (timerName.equals(boundarytimerName.getValue(execution)))) {
				LOGGER.debug("--- CAMBIO DATA Duedate: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
				//job.wait().
				int yearAddValueInt = 0;   
				int monthAddValueInt = 0;   
				int hourAddValueInt = 0;   
				int dayAddValueInt = 0;   
				int minuteAddValueInt = 0;   
				if (yearAddValue != null) {
					try {
						yearAddValueInt = Integer.parseInt(yearAddValue.getValue(execution).toString());
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}

				if (monthAddValue != null) {
					try {
						monthAddValueInt = Integer.parseInt(monthAddValue.getValue(execution).toString());
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}
				if (dayAddValue != null) {
					try {
						dayAddValueInt = Integer.parseInt(dayAddValue.getValue(execution).toString());
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}

				if (hourAddValue != null) {
					try {
						hourAddValueInt = Integer.parseInt(hourAddValue.getValue(execution).toString());
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}
				if (minuteAddValue != null) {
					try {
						minuteAddValueInt = Integer.parseInt(minuteAddValue.getValue(execution).toString());
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}
				LOGGER.debug("--- boundarytimerName: {}, yearAddValue: {}, monthAddValue: {}, monthAddValue: {}, minuteAddValue: {}", boundarytimerName.getValue(execution), yearAddValueInt, monthAddValueInt, hourAddValueInt, minuteAddValueInt);
				Calendar newDate = Calendar.getInstance();
				newDate.add(Calendar.YEAR, yearAddValueInt);
				newDate.add(Calendar.MONTH, monthAddValueInt);
				newDate.add(Calendar.DAY_OF_YEAR, dayAddValueInt);
				newDate.add(Calendar.HOUR, hourAddValueInt);
				newDate.add(Calendar.MINUTE, minuteAddValueInt);
				Date newTimerDate = newDate.getTime();
				((TimerEntity) job).setDuedate(newTimerDate);
				LOGGER.debug("--- NUOVO Duedate: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
				int giorni = ((job.getDuedate().getDate()) - newDate.getTime().getDate()) ;
				LOGGER.debug("--- La differenza di giorni sarà: {}", job.getDuedate(), job.getId(), timerName);
			}
		}
		//JobQuery jobQuery =  execution.getEngineServices().getManagementService().createJobQuery().processInstanceId(processInstanceId);

		//        execution.getEngineServices().getRuntimeService().Job.getExecutionId().
		//        execution.getEngineServices().getDynamicBpmnService().;
		//        execution.getEngineServices().getRepositoryService().;
		//        execution.getEngineServices().getRuntimeService().);
		//        execution.getEngineServices().getManagementService().;

	}
}
