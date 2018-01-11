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

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;



@Component
public class CalcolaTempi implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CalcolaTempi.class);

	private Expression timeVariableStart;
	private Expression timeVariableStop;
	private Expression timeVariableRecord;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		String timeVariableStartValue = timeVariableStart.getValue(execution).toString();
		Date timeVariableStartDate = (Date) execution.getVariable(timeVariableStartValue);
		String timeVariableStopValue = timeVariableStop.getValue(execution).toString();
		Date timeVariableStopDate =  (Date) execution.getVariable(timeVariableStopValue);
		String timeVariableRecordValue = timeVariableRecord.getValue(execution).toString();
		//Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
		//Date timeVariableStartDateFormat = new Date(timeVariableStartDate);
		// Date timeVariableStopDateFormat = new Date(timeVariableStopDate);
		int timeVariableRecordDateValue =(int) (timeVariableStopDate.getTime() - timeVariableStartDate.getTime());
		int timeVariableRecordDateDays = timeVariableRecordDateValue/ (1000 * 60 * 60 * 24);
		int timeVariableRecordDateHours = timeVariableRecordDateValue/ (1000 * 60 * 60);
		int timeVariableRecordDateMinutes = timeVariableRecordDateValue/ (1000 * 60);

		execution.setVariable(timeVariableRecordValue, timeVariableRecordDateDays);

		LOGGER.debug("--- set time: {} to: {}", timeVariableRecordValue, timeVariableRecordDateDays);
		LOGGER.debug("--- timeVariableRecordDateHours: {} timeVariableRecordDateMinutes: {}", timeVariableRecordDateHours, timeVariableRecordDateMinutes);
	}

}
