package it.cnr.si.flows.ng.listeners.oiv.service;


import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.cfg.TransactionContext;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.impl.jobexecutor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.resource.FlowsUserResource.SearchResult;

import static it.cnr.si.flows.ng.utils.Utils.TASK_EXECUTOR;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;



@Service
public class GestioneTimerService {
	private static final Logger LOGGER = LoggerFactory.getLogger(GestioneTimerService.class);

	@Autowired
	protected ManagementService managementService;
	@Autowired
	private RestResponseFactory restResponseFactory;

	@Inject
	private RepositoryService repositoryService;

	public List getTimers(String processInstanceId) throws IOException, ParseException {

		//      TIMER
		List<Job> timerJobs = managementService.createJobQuery()
				.processInstanceId(processInstanceId)
				.timers()
				.list();
		LOGGER.info("TIMERS" + timerJobs);
		for(Job job : timerJobs)
		{
			String timerName = ((TimerEntity) job).getJobHandlerConfiguration()
					.split(":")[1]
							.replace("\"", "")
							.replace("}", "");
			LOGGER.info("getDuedate {}, getId {}, TimerDeclarationImpl {}", job.getDuedate(), job.getId(), timerName);
		}
		return timerJobs;
	}

	public List getTimer(String processInstanceId, String timerId) throws IOException, ParseException {

		//      TIMER
		List<Job> selectedTimerJob = new ArrayList<>();
		List<Job> timerJobs = managementService.createJobQuery()
				.processInstanceId(processInstanceId)
				.timers()
				.list();
		LOGGER.info("TIMERS" + timerJobs);
		for(Job job : timerJobs)
		{
			String timerName = ((TimerEntity) job).getJobHandlerConfiguration()
					.split(":")[1]
							.replace("\"", "")
							.replace("}", "");
			LOGGER.info("getDuedate {}, getId {}, TimerDeclarationImpl {}", job.getDuedate(), job.getId(), timerName);
			if (timerName.equals(timerId)) {
				LOGGER.info("--- DATA FINE PROCEDURA: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
				selectedTimerJob.add(0, job);
				LOGGER.info("--- selectedTimerJob.size(): {} ", selectedTimerJob.size());

			}
		}
		return selectedTimerJob;
	}


	public void setTimerValuesFromNow(String processInstanceId, String timerId, int yearAddValue, int monthAddValue, int dayAddValue, int hourAddValue, int minuteAddValue) throws IOException, ParseException  {

		List<Job> timerJobs = managementService.createJobQuery()
				.processInstanceId(processInstanceId)
				.timers()
				.list();

		for(Job job : timerJobs)
		{
			String timerName = ((TimerEntity) job).getJobHandlerConfiguration()
					.split(":")[1]
							.replace("\"", "")
							.replace("}", "");
			LOGGER.debug("getDuedate {}, getId {}, TimerDeclarationImpl {}", job.getDuedate(), job.getId(), timerName);
			if (timerName.equals(timerId)) {
				LOGGER.debug("--- CAMBIO DATA Duedate: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
				//job.wait().
				int yearAddValueInt = 0;   
				int monthAddValueInt = 0;   
				int hourAddValueInt = 0;   
				int dayAddValueInt = 0;   
				int minuteAddValueInt = 0;   
				if (yearAddValue != 0) {
					try {
						yearAddValueInt = yearAddValue;
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}

				if (monthAddValue != 0) {
					try {
						monthAddValueInt = monthAddValue;
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}
				if (dayAddValue != 0) {
					try {
						dayAddValueInt = dayAddValue;
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}

				if (hourAddValue != 0) {
					try {
						hourAddValueInt = hourAddValue;
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}
				if (minuteAddValue != 0) {
					try {
						minuteAddValueInt = minuteAddValue;
					} catch (NumberFormatException e) {
						//Will Throw exception!
						//do something! anything to handle the exception.
					}
				}
				LOGGER.debug("--- boundarytimerName: {}, yearAddValue: {}, monthAddValue: {}, monthAddValue: {}, minuteAddValue: {}", timerName, yearAddValueInt, monthAddValueInt, hourAddValueInt, minuteAddValueInt);
				Calendar newDate = Calendar.getInstance();
				newDate.add(Calendar.YEAR, yearAddValueInt);
				newDate.add(Calendar.MONTH, monthAddValueInt);
				newDate.add(Calendar.DAY_OF_YEAR, dayAddValueInt);
				newDate.add(Calendar.HOUR, hourAddValueInt);
				newDate.add(Calendar.MINUTE, minuteAddValueInt);
				Date newTimerDate = newDate.getTime();
				//TimerEntity newTimerDate = new TimerEntity();
				//newTimerDate.getId().setDuedate(newTimerDate);
				((TimerEntity) job).setDuedate(newTimerDate);
				
				//managementService.executeCommand(Context.getCommandContext().getCommand());


//				JobExecutor jobExecutor = Context.getProcessEngineConfiguration().getJobExecutor();
//				JobAddedNotification messageAddedNotification = new JobAddedNotification(jobExecutor);
//				TransactionContext transactionContext = Context.getCommandContext().getTransactionContext();
//				transactionContext.addTransactionListener(TransactionState.COMMITTED, messageAddedNotification);

				//managementService.setDuedate(newTimerDate);
				// (ESEGUE IL TIMER) managementService.executeJob(job.getId());
				//((TimerEntity) job).execute(Context.getCommandContext());
				//Context.getCommandContext().getJobEntityManager().schedule((TimerEntity) job);
				//Context.getCommandContext().getJobEntityManager().schedule((TimerEntity) job);
				LOGGER.debug("--- NUOVO Duedate: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
				int giorni = ((job.getDuedate().getDate()) - newDate.getTime().getDate()) ;
				LOGGER.debug("--- La differenza di giorni sarà: {}", job.getDuedate(), job.getId(), timerName);
			}
		}
		LOGGER.debug("--- processInstanceId: {} timerId: {}", processInstanceId, timerId);
	}

}
