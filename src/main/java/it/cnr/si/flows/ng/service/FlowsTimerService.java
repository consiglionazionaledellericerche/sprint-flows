package it.cnr.si.flows.ng.service;


import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;

import org.activiti.engine.impl.persistence.entity.TimerEntity;

import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


import javax.inject.Inject;

import org.activiti.rest.service.api.RestResponseFactory;

import org.springframework.beans.factory.annotation.Autowired;








import 	it.cnr.si.flows.ng.repository.SetTimerDuedateCmd;


@Service
public class FlowsTimerService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTimerService.class);

	@Autowired
	protected ManagementService managementService;
	@Autowired
	private RestResponseFactory restResponseFactory;


	@Inject
	private RepositoryService repositoryService;

	@Inject
	private FlowsTimerService flowsTimerService;


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


	public void setTimer(String processInstanceId, String timerId, Date date) throws IOException, ParseException {

		//      TIMER
		LOGGER.debug("setTimer {}",  date);
		// SimpleDateFormat formatter=new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
		//Date newTimerDate = formatter.parse(date);
		Date newTimerDate = date;

		List<Job> jobTimer = flowsTimerService.getTimer(processInstanceId,timerId);
		if(jobTimer.size() > 0){
			LOGGER.info("------ DATA: {} per timer: {} " + jobTimer.get(0).getDuedate(), timerId);
		} else {
			LOGGER.info("------ " + timerId + ": TIMER SCADUTO: ");
		}

		managementService.executeCommand(new SetTimerDuedateCmd(jobTimer.get(0).getId(), newTimerDate));

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

				((TimerEntity) job).setDuedate(newTimerDate);
				//SetTimerDuedateCmd(job.getId(),newTimerDate);
				managementService.executeCommand(new SetTimerDuedateCmd(job.getId(), newTimerDate));
				// (ESEGUE IL TIMER) managementService.executeJob(job.getId());

				LOGGER.debug("--- NUOVO Duedate: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
				long diffTime = job.getDuedate().getTime() - newDate.getTime().getTime();
				long diffDays = diffTime / (1000 * 60 * 60 * 24);
				LOGGER.debug("--- La differenza di giorni sarà: {}", diffDays);
			}
		}
		LOGGER.debug("--- processInstanceId: {} timerId: {}", processInstanceId, timerId);
	}


}
