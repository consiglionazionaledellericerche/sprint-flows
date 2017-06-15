package it.cnr.si.flows.ng.listeners.acquistitrasparenza;

import java.util.List;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;



@Component
public class CalcolaTotaleImpegniAcquisti implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CalcolaTotaleImpegniAcquisti.class);

	@Override
	public void notify(DelegateExecution execution) throws Exception {

		double importoTotale = 0.0;	
		for(int i = 0; i<=100; i=i+1) {
			String Elemento = "impegni[" +  i + "][importo]";
			if(execution.getVariable(Elemento) != null){
				String importoImpegnoSingolo = (String) execution.getVariable(Elemento);
				try {
					double importo = Double.parseDouble(importoImpegnoSingolo);
					importoTotale = importoTotale + importo;
				} catch (NumberFormatException e ){
					LOGGER.error("Formato Impegno Non Valido {} nel flusso {} - {}", importoImpegnoSingolo, execution.getId(), execution.getVariable("title"));
					throw new BpmnError("400", "Formato Impegno Non Valido: " + importoImpegnoSingolo);
				}			
			} else {
				break;
			}
			execution.setVariable("importoTotale", importoTotale);
		}
	}
}
