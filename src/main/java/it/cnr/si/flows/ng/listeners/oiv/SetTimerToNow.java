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
public class SetTimerToNow implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SetTimerToNow.class);

	private Expression timeVariable;

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		String timeVariableName = (String) timeVariable.getValue(execution);
		Calendar newDate = Calendar.getInstance();
		execution.setVariable(timeVariableName, newDate.getTime());

		LOGGER.debug("--- set timeVariable: {} to: {}", timeVariableName, execution.getVariable(timeVariableName));
	}

}
