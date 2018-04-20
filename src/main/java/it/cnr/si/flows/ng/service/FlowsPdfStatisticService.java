package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.utils.Utils;


import org.activiti.engine.ProcessEngine;
import org.activiti.engine.TaskService;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.inject.Inject;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import static it.cnr.si.flows.ng.utils.Utils.*;


/**
 * Created by massimo on 04/04/2018.
 */
@Service
public class FlowsPdfStatisticService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsPdfStatisticService.class);

	@Inject
	FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private Utils utils;
	@Inject
	ProcessEngine processEngine;
	@Inject
	TaskService taskService;
	

	// ELENCO PARAMETRI STATISTICHE
	int nrFlussiTotali = 0;
	int nrFlussiAttivi = 0;
	int nrFlussiTerminati = 0;
	int allTerminatedProcessInstancesDurationInMillis = 0;

	Calendar newDate = Calendar.getInstance();	
	Date dataOdierna = newDate.getTime();
	
	String sessoRichiedente = "";
	String fasciaAppartenenzaAttribuita = "";
	String tipologiaRichiesta = "";
	String faseUltima = "";
	String statoFinaleDomanda = "";


	public JSONObject getPdfStatistics (String processDefinitionKey, String startDateGreat, String startDateLess) throws ParseException {
		Map<String, String> req = new HashMap<>();
		req.put("startDateGreat", startDateGreat);
		req.put("startDateLess", startDateLess);
		req.put(processDefinitionKey, processDefinitionKey);
		String order = "ASC";
		Integer firstResult = -1;
		Integer maxResults = -1;
		Boolean active = true;
		Boolean finished = false;

		resetStatisticvariables();

		Map<String, Object>  flussiAttivi = flowsProcessInstanceService.search(req, processDefinitionKey, active, order, firstResult, maxResults);
		Map<String, Object>  flussiTerminati = flowsProcessInstanceService.search(req, processDefinitionKey, finished, order, firstResult, maxResults);
		Map<String, Integer> mapStatiFlussiAttivi = new HashMap<String, Integer>();

		//VALORIZZAZIONE PARAMETRI STATISTICHE
		nrFlussiAttivi = parseInt(flussiAttivi.get("totalItems").toString());
		nrFlussiTerminati  = parseInt(flussiTerminati.get("totalItems").toString());
		nrFlussiTotali = nrFlussiAttivi + nrFlussiTerminati ;


		LOGGER.debug("nr. nrFlussiAttivi: {} - nr. nrFlussiTerminati: {} - nr. nrFlussiTotali: {}", nrFlussiAttivi, nrFlussiTerminati, nrFlussiTotali);



		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI ATTIVI
		List<HistoricProcessInstanceResponse> activeProcessInstances = (List<HistoricProcessInstanceResponse>) flussiAttivi.get("processInstances");
		for (HistoricProcessInstanceResponse pi : activeProcessInstances) {
			LOGGER.debug(" getId= " + pi.getId());
			LOGGER.debug(" getDurationInMillis= " + pi.getDurationInMillis());
			LOGGER.debug(" elementi= " + pi.getName());
			String processInstanceId = pi.getId();
			Map<String, Object> processInstanceDetails = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
			HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) processInstanceDetails.get("entity");
			List<RestVariable> variables = processInstance.getVariables();

			
			// Calcolo gli stati nei flussi attivi)
			String currentTaskName = taskService.createTaskQuery().processInstanceId(processInstanceId).active().singleResult().getName();
			LOGGER.debug("--##  currentTaskName : {} ", currentTaskName);
			//TODO calcolo nr istanze
			if(mapStatiFlussiAttivi.containsKey(currentTaskName)) {
				mapStatiFlussiAttivi.put(currentTaskName, mapStatiFlussiAttivi.get(currentTaskName) + 1);
			} else {
				mapStatiFlussiAttivi.put(currentTaskName, 1);
			}
		

		}

		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI TERMINATI
		List<HistoricProcessInstanceResponse> terminatedProcessInstances = (List<HistoricProcessInstanceResponse>) flussiTerminati.get("processInstances");
		for (HistoricProcessInstanceResponse pi : terminatedProcessInstances) {
			LOGGER.debug(" getId= " + pi.getId());
			LOGGER.debug(" getDurationInMillis= " + pi.getDurationInMillis());
			LOGGER.debug(" elementi= " + pi.getName());
			String processInstanceId = pi.getId();
			Map<String, Object> processInstanceDetails = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
			HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) processInstanceDetails.get("entity");
			List<RestVariable> variables = processInstance.getVariables();
			allTerminatedProcessInstancesDurationInMillis = (int) (allTerminatedProcessInstancesDurationInMillis + pi.getDurationInMillis());

		}
		
		JSONObject variableStatisticsJson = new JSONObject();
		
		//LISTA VARIABILI COMUNI
		variableStatisticsJson.put("dataIn", startDateGreat);
		variableStatisticsJson.put("dataOut", startDateLess);
		variableStatisticsJson.put("nrFlussiAttivi", nrFlussiAttivi);
		variableStatisticsJson.put("nrFlussiTerminati", nrFlussiTerminati);
		variableStatisticsJson.put("nrFlussiTotali", nrFlussiTotali);
		
		//LISTA VARIABILI FLUSSI ATTIVI
		Map<String, String> listaStatiFlussiAttivi = new HashMap<String, String>();
		JSONArray arrayStatiFlussiAttivi = new JSONArray();
		for (Entry<String, Integer> pair : mapStatiFlussiAttivi.entrySet()) {
			listaStatiFlussiAttivi.put("Stato", pair.getKey());
			listaStatiFlussiAttivi.put("NrIstanze", pair.getValue().toString());
			arrayStatiFlussiAttivi.put(listaStatiFlussiAttivi);
		}
		variableStatisticsJson.put("StatiFlussiAttivi", arrayStatiFlussiAttivi);
		
		//LISTA VARIABILI FLUSSI TERMINATI
		Map<String, String> listaStatiFlussiTerminati = new HashMap<String, String>();
		JSONArray arrayStatiFlussiTerminati = new JSONArray();
		int mediaGiorniFlusso = allTerminatedProcessInstancesDurationInMillis/ (1000 * 60 * 60 * 24 * nrFlussiTerminati);
		listaStatiFlussiTerminati.put("mediaGiorniFlusso", String.valueOf(mediaGiorniFlusso));
		arrayStatiFlussiTerminati.put(listaStatiFlussiTerminati);
		variableStatisticsJson.put("StatiFlussiTerminati", arrayStatiFlussiTerminati);


		//flowsPdfResource.makeStatisticPdf(processDefinitionKey, variableStatisticsJson);

		return variableStatisticsJson;
	}


	private int calcolaGiorniTraDate(Date dateInf, Date dateSup) {
		int timeVariableRecordDateValue =(int) (dateSup.getTime() - dateInf.getTime());
		int timeVariableRecordDateDays = timeVariableRecordDateValue/ (1000 * 60 * 60 * 24);
		int timeVariableRecordDateHours = timeVariableRecordDateValue/ (1000 * 60 * 60);
		int timeVariableRecordDateMinutes = timeVariableRecordDateValue/ (1000 * 60);

		LOGGER.debug("--- {} gg diff tra : {} e: {}", timeVariableRecordDateDays, dateInf, dateSup);
		return timeVariableRecordDateDays;
	}

	private String formatDate(Date date) {
		return date != null ? utils.formattaDataOra(date) : "";
	}

	private void resetStatisticvariables() {

		nrFlussiTotali = 0;
		nrFlussiAttivi = 0;
		nrFlussiTerminati = 0;
		dataOdierna = newDate.getTime();
	}

}
