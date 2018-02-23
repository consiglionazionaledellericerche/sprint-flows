package it.cnr.si.flows.ng.listeners.oiv.service;

import java.io.IOException;
import java.text.ParseException;


import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ManageControlli {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageControlli.class);

	public void valutazioneEsperienze(DelegateExecution execution) throws IOException, ParseException {
		String numeroValutazioniPositive = execution.getVariable("numeroValutazioniPositive").toString();
		String numeroValutazioniNegative = execution.getVariable("numeroValutazioniNegative").toString();
		String valutazioneIstruttore = execution.getVariable("valutazioneIstruttore").toString();
		if((numeroValutazioniPositive.equals("0")) && valutazioneIstruttore.equals("domanda_da_approvare")){
			LOGGER.info("-- numeroValutazioniPositive: " + numeroValutazioniPositive );
			throw new BpmnError("412", "La scelta 'domanda_da_approvare' non risulta congruente<br>con la valutazione negativa di tutte le esperienze<br>");
		}
		if((numeroValutazioniNegative.equals("0")) && valutazioneIstruttore.equals("domanda_da_respingere")){
			LOGGER.info("-- numeroValutazioniNegative: " + numeroValutazioniNegative );
			throw new BpmnError("412", "La scelta 'domanda_da_respingere' non risulta congruente<br>con la valutazione positiva di tutte le esperienze<br>");
		}
	}
}
