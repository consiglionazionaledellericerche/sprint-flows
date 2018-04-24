package it.cnr.si.flows.ng.listeners.oiv.service;


import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.service.FlowsTimerService;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;


@Service
public class OperazioniTimer {
	private static final Logger LOGGER = LoggerFactory.getLogger(OperazioniTimer.class);

	@Inject
	private FlowsTimerService flowsTimerService;


	public void printTimer(DelegateExecution execution, String timerId) throws IOException, ParseException {
		String processInstanceId = execution.getProcessInstanceId();
		List<Job> timerJobs = flowsTimerService.getTimer(processInstanceId, timerId);
		if(timerJobs.size() > 0){
			LOGGER.info("------ DATA: {} per timer: {} " + timerJobs.get(0).getDuedate(), timerId);
		} else {
			LOGGER.info("------ " + timerId + ": TIMER SCADUTO: ");	
		}
	}

	public void determinaTimerScadenzaTermini(DelegateExecution execution, String timerId) throws IOException, ParseException {
		String processInstanceId = execution.getProcessInstanceId();
		List<Job> timerJobs = flowsTimerService.getTimer(processInstanceId, timerId);
		if(timerJobs.size() > 0){
			timerJobs.get(0).getDuedate();
			execution.setVariable("dataScadenzaTerminiDomanda", timerJobs.get(0).getDuedate());
			LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));	
		} else {
			LOGGER.info("------ " + timerId + ": TIMER SCADUTO: ");	
		}
	}

	public void setTimer(DelegateExecution execution, String timerId, int years, int months, int days, int hours, int minutes) throws IOException, ParseException {

		String processInstanceId = execution.getProcessInstanceId();
		// Estende  il timer 
		flowsTimerService.setTimerValuesFromNow(processInstanceId, timerId, years, months, days, hours, minutes);
		LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));

	}
	public void setTimerScadenzaTermini(DelegateExecution execution, String timerId, int years, int months, int days, int hours, int minutes) throws IOException, ParseException {

		String processInstanceId = execution.getProcessInstanceId();
		// Estende  il timer 
		flowsTimerService.setTimerValuesFromNow(processInstanceId, timerId, years, months, days, hours, minutes);
		determinaTimerScadenzaTermini(execution, timerId);
		LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));
	}


	public void sospendiTimerTempiProceduramentali(DelegateExecution execution, String timerScadenzaTempiId, String timerAvvisoScadenzaId) throws IOException, ParseException {

		String processInstanceId = execution.getProcessInstanceId();
		List<Job> timerScadenzaTempiJobs = flowsTimerService.getTimer(processInstanceId, timerScadenzaTempiId);
		Calendar newDate = Calendar.getInstance();
		Date timerScadenzaTempi = newDate.getTime();
		Date timerAvvisoScadenza = newDate.getTime();
		if (!timerScadenzaTempiJobs.isEmpty()) {
			timerScadenzaTempi = timerScadenzaTempiJobs.get(0).getDuedate();
			List<Job> timerAvvisoScadenzaJobs = flowsTimerService.getTimer(processInstanceId, timerAvvisoScadenzaId);
			if (!timerAvvisoScadenzaJobs.isEmpty()) {
				timerAvvisoScadenza = timerAvvisoScadenzaJobs.get(0).getDuedate(); 
			}
			LOGGER.info("------ Si SOSPENDONO LE DATE: ScadenzaTempiProceduramentali: {} e AvvisoScadenzaTempiProceduramentali: {}" + timerScadenzaTempi, timerAvvisoScadenza);

			//Calcolo giorni mancanti alla scadenza vengono salvati nella variabile di processo "ggScadenzaTerminiDomanda"
			long diffTime = timerScadenzaTempi.getTime() - newDate.getTime().getTime();
			long diffDays = diffTime / (1000 * 60 * 60 * 24);
			LOGGER.debug("--- La differenza di giorni sarà: {}", diffDays);
			execution.setVariable("ggScadenzaTerminiDomanda", diffDays);
			LOGGER.info("------ gg Scadenza Termini Domanda: " + execution.getVariable("ggScadenzaTerminiDomanda"));

			// Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 1 anno
			flowsTimerService.setTimerValuesFromNow(processInstanceId, timerScadenzaTempiId, 1, 0, 0, 0, 0);
			determinaTimerScadenzaTermini(execution, "boundarytimer3");
			// Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 1 anno
			flowsTimerService.setTimerValuesFromNow(processInstanceId, timerAvvisoScadenzaId, 1, 0, 0, 0, 0);			
		}

	}

	public void riprendiTimerTempiProceduramentali(DelegateExecution execution, String timerScadenzaTempiId, String timerAvvisoScadenzaId) throws IOException, ParseException {

		String processInstanceId = execution.getProcessInstanceId();
		int diffDays = Integer.parseInt(execution.getVariable("ggScadenzaTerminiDomanda").toString());
		int diffDaysAvviso = 0;
		if (diffDays >5 ) {
			diffDaysAvviso = diffDays - 5;
		}
		// Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 1 anno
		flowsTimerService.setTimerValuesFromNow(processInstanceId, timerScadenzaTempiId, 0, 0, diffDays, 0, 0);
		// Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 1 anno
		flowsTimerService.setTimerValuesFromNow(processInstanceId, timerAvvisoScadenzaId, 0, 0, diffDaysAvviso, 0, 0);
		determinaTimerScadenzaTermini(execution, "boundarytimer3");
		LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));
	}

	public int calcolaGiorniTraDateString(String stringDateInf, String stringDateSup) throws ParseException {

		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		Date dateInf =  df.parse(stringDateInf);
		Date dateSup =  df.parse(stringDateSup);
		int timeVariableRecordDateValue =(int) (dateSup.getTime() - dateInf.getTime());
		int timeVariableRecordDateDays = timeVariableRecordDateValue/ (1000 * 60 * 60 * 24);
		int timeVariableRecordDateHours = timeVariableRecordDateValue/ (1000 * 60 * 60);
		int timeVariableRecordDateMinutes = timeVariableRecordDateValue/ (1000 * 60);

		LOGGER.debug("--- {} gg diff tra : {} e: {}", timeVariableRecordDateDays, dateInf, dateSup);
		return timeVariableRecordDateDays;
	}

	public int calcolaGiorniTraDate(Date dateInf, Date dateSup) throws ParseException {

		int timeVariableRecordDateValue =(int) (dateSup.getTime() - dateInf.getTime());
		int timeVariableRecordDateDays = timeVariableRecordDateValue/ (1000 * 60 * 60 * 24);
		int timeVariableRecordDateHours = timeVariableRecordDateValue/ (1000 * 60 * 60);
		int timeVariableRecordDateMinutes = timeVariableRecordDateValue/ (1000 * 60);

		LOGGER.debug("--- {} gg diff tra : {} e: {}", timeVariableRecordDateDays, dateInf, dateSup);
		return timeVariableRecordDateDays;
	}
}
