package it.cnr.si.flows.ng.listeners.oiv.service;


import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Utils;

import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.inject.Inject;

import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.endDate;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.gruppoRA;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.initiator;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.startDate;
import static it.cnr.si.flows.ng.utils.Utils.*;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;


/**
 * Created by massimo on 04/04/2018.
 */
@Service
public class StatisticOivService {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticOivService.class);

	@Inject
	FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private Utils utils;



	public void getOivStatistics (String processDefinitionKey, String startDateGreat, String startDateLess) throws ParseException {
		Map<String, String> req = new HashMap<>();
		req.put("startDateGreat", startDateGreat);
		req.put("startDateLess", startDateLess);
		req.put(processDefinitionKey, processDefinitionKey);
		String order = "ASC";
		Integer firstResult = -1;
		Integer maxResults = -1;
		Boolean active = true;
		Boolean finished = false;
		// ELENCO PARAMETRI STATISTICHE
		Integer domandeTotali = 0;
		Integer domandeInEsame = 0;
		Integer domandeEsaminate = 0;
		Integer nrUominiFascia1 = 0;
		Integer nrUominiFascia2 = 0;
		Integer nrUominiFascia3 = 0;
		Integer nrDonneFascia1 = 0;
		Integer nrDonneFascia2 = 0;
		Integer nrDonneFascia3 = 0;

		Map<String, Object>  flussiAttivi = flowsProcessInstanceService.search(req, processDefinitionKey, active, order, firstResult, maxResults);
		Map<String, Object>  flussiTerminati = flowsProcessInstanceService.search(req, processDefinitionKey, finished, order, firstResult, maxResults);

		//VALORIZZAZIONE PARAMETRI STATISTICHE
		domandeInEsame = parseInt(flussiAttivi.get("totalItems").toString());
		domandeEsaminate = parseInt(flussiTerminati.get("totalItems").toString());
		domandeTotali = domandeInEsame + domandeEsaminate;


		LOGGER.debug("nr. domandeInEsame: {} - nr. domandeEsaminate: {} - nr. domandeTotali: {}", domandeInEsame, domandeEsaminate, domandeTotali);

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

			String sessoRichiedente = "";
			String fasciaAppartenenzaAttribuita = "";
			String tipologiaRichiesta = "";
			String dataInvioDomanda = "";
			String faseUltima = "";

			//GESTIONE DEI PARAMETRI DA VISUALIZZARE
			for (RestVariable var : variables) {
				String variableName = var.getName().toString();
				switch(variableName){  
				case "sessoRichiedente": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					sessoRichiedente = var.getValue().toString();
				};break; 
				case "fasciaAppartenenzaAttribuita": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					fasciaAppartenenzaAttribuita = var.getValue().toString();
				};break; 
				case "tipologiaRichiesta": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					tipologiaRichiesta = var.getValue().toString();
				};break; 
				case "faseUltima": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					faseUltima = var.getValue().toString();
				};break;
				case "dataInvioDomanda": {
					LOGGER.info("-- " + var.getName() + ": " + formatDate(utils.parsaData((String) var.getValue())));
					dataInvioDomanda = formatDate(utils.parsaData((String) var.getValue()));
				};break;
				default:  {
					//LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				};break;  

				}
			}

			if (sessoRichiedente.equals("M")){
				switch(fasciaAppartenenzaAttribuita){  
				case "1": {
					nrUominiFascia1 = nrUominiFascia1 +1;
				};break;   
				case "2": {
					nrUominiFascia2 = nrUominiFascia2 +1;
				};break;   
				case "3": {
					nrUominiFascia3 = nrUominiFascia3 +1;
				};break; 
				} 
			}else {
				switch(fasciaAppartenenzaAttribuita){  
				case "1": {
					nrDonneFascia1 = nrDonneFascia1 +1;
				};break;   
				case "2": {
					nrDonneFascia2 = nrDonneFascia2 +1;
				};break;   
				case "3": {
					nrDonneFascia3 = nrDonneFascia3 +1;
				};break; 
				}
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

			String sessoRichiedente = "";
			String fasciaAppartenenzaAttribuita = "";
			String tipologiaRichiesta = "";
			String dataInvioDomanda = "";
			String faseUltima = "";

			//GESTIONE DEI PARAMETRI DA VISUALIZZARE
			for (RestVariable var : variables) {
				String variableName = var.getName().toString();
				switch(variableName){  
				case "sessoRichiedente": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					sessoRichiedente = var.getValue().toString();
				};break; 
				case "fasciaAppartenenzaAttribuita": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					fasciaAppartenenzaAttribuita = var.getValue().toString();
				};break; 
				case "tipologiaRichiesta": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					tipologiaRichiesta = var.getValue().toString();
				};break; 
				case "faseUltima": {
					LOGGER.info("-- " + var.getName() + ": " + var.getValue());
					faseUltima = var.getValue().toString();
				};break;
				case "dataInvioDomanda": {
					LOGGER.info("-- " + var.getName() + ": " + formatDate(utils.parsaData((String) var.getValue())));
					dataInvioDomanda = formatDate(utils.parsaData((String) var.getValue()));
				};break;
				default:  {
					//LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				};break;  

				}
			}

			if (sessoRichiedente.equals("M")){
				switch(fasciaAppartenenzaAttribuita){  
				case "1": {
					nrUominiFascia1 = nrUominiFascia1 +1;
				};break;   
				case "2": {
					nrUominiFascia2 = nrUominiFascia2 +1;
				};break;   
				case "3": {
					nrUominiFascia3 = nrUominiFascia3 +1;
				};break; 
				} 
			}else {
				switch(fasciaAppartenenzaAttribuita){  
				case "1": {
					nrDonneFascia1 = nrDonneFascia1 +1;
				};break;   
				case "2": {
					nrDonneFascia2 = nrDonneFascia2 +1;
				};break;   
				case "3": {
					nrDonneFascia3 = nrDonneFascia3 +1;
				};break; 
				}
			}


		}
		
		LOGGER.info("-- nrUominiFascia1: {} - nrUominiFascia2: {} - nrUominiFascia3: {} - nrUominiTotale: {} ",  nrUominiFascia1, nrUominiFascia2, nrUominiFascia3, nrUominiFascia1 + nrUominiFascia2 + nrUominiFascia3);
		LOGGER.info("-- nrDonneFascia1: {} - nrDonneFascia2: {} - nrDonneFascia3: {} - nrDonneTotale: {} ",  nrDonneFascia1, nrDonneFascia2, nrDonneFascia3, nrDonneFascia1 + nrDonneFascia2 + nrDonneFascia3);

	}


	private String formatDate(Date date) {
		return date != null ? utils.formattaDataOra(date) : "";
	}
}
