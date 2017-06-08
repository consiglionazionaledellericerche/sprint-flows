package it.cnr.si.flows.ng.listeners.acquistitrasparenza;

import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.cnr.si.flows.ng.service.CounterService;

public class FirmaDecisione implements ExecutionListener {

	private static final long serialVersionUID = -56001764662303256L;

	@Inject
	private CounterService counterService;

	private static final Logger LOGGER = LoggerFactory.getLogger(FirmaDecisione.class);

	@Override
	public void notify(DelegateExecution execution) throws Exception {
		if (!execution.getEventName().equals(ExecutionListener.EVENTNAME_TAKE))
			throw new IllegalStateException("Questo Listener accetta solo eventi 'take'.");

		Date d = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		int annoDetermina = c.get(Calendar.YEAR);
		execution.setVariable("annoDetermina", annoDetermina);

		String counterId = "counterDetermina";
		String key =  String.valueOf(counterService.getNext(counterId));
		String nrDetermina = (String) execution.getVariable("nrDetermina");
		String nrDeterminaSoppressi = (String) execution.getVariable("nrDeterminaSoppressi");
		if (nrDetermina != null) {
			if (nrDeterminaSoppressi != null) {
				execution.setVariable("nrDeterminaSoppressi", nrDeterminaSoppressi + " - " + nrDetermina);
			} else {
				execution.setVariable("nrDeterminaSoppressi", nrDetermina);
			}
		}
		execution.setVariable("nrDetermina", key);

	}

}
