package it.cnr.si.flows.ng.service;

import com.opencsv.CSVWriter;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;

@Service
public class FlowsCsvService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsCsvService.class);

	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private FlowsCsvDispatcherService flowsCsvDispatcherService;
	@Inject
	private ViewRepository viewRepository;
	@Inject
	private Utils utils;


	public List<HistoricProcessInstanceResponse> getProcessesStatistics(String processDefinitionKey, String idStruttura,String startDateGreat, String startDateLess, boolean activeFlag) {
		Map<String, String> req = new HashMap<>();
		req.put("startDateGreat", startDateGreat);
		req.put("startDateLess", startDateLess);
		if (idStruttura != null && !idStruttura.equals("0000")) {
			req.put("idStruttura", "text="+idStruttura);
		}
		//req.put(processDefinitionKey, processDefinitionKey);
		String order = "ASC";
		Integer firstResult = -1;
		Integer maxResults = -1;

		DataResponse flussi = flowsProcessInstanceService.search(req, processDefinitionKey, activeFlag, order, firstResult, maxResults, false);

		//VALORIZZAZIONE PARAMETRI STATISTICHE
		Integer domandeAttive = (int) flussi.getTotal();

		LOGGER.debug("nr. domande: {} con activeFlag: {}", domandeAttive, activeFlag);
		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI ATTIVI
		return (List<HistoricProcessInstanceResponse>) flussi.getData();
	}



	public void  makeCsv(List<HistoricProcessInstanceResponse> processInstances, PrintWriter printWriter, String processDefinitionKey) throws IOException {
		// vista (campi e variabili) da inserire nel csv in base alla tipologia di flusso selezionato
		View view = null;
		CSVWriter writer = new CSVWriter(printWriter, ',');
		if (!processDefinitionKey.equals(ALL_PROCESS_INSTANCES)) {
			view = viewRepository.getViewByProcessidType(processDefinitionKey, "export-csv");
			LOGGER.debug("view: {}", view);
		}
		List entriesIterable = new ArrayList<>();

		if (view != null) {
			List<Object> fields = new JSONArray(view.getView()).toList();

			//popolo gli headers del csv (intestazioni delle colonne)
			List<String> headers = fields.stream()
					.map(el -> (String) ((HashMap) el).get("label"))
					.collect(Collectors.toList());
			headers.add(0, "Stato Istanza");
			headers.add(1, "Business Key");
			headers.add(2, "Start Date");
			headers.add(3, "End Date");
			headers.add(4, "Duration In Millis");
			headers.add(5, "Identificativo ProcessInstance");
			headers.add(6, "Fase");
			headers.add(7, "Titolo");
			headers.add(8, "Descrizione");
			headers.add(9, "initiator");
			entriesIterable.add(0, utils.getArray(headers));

			for (HistoricProcessInstanceResponse pi : processInstances) {
				String processInstanceId = pi.getId();
				Map<String, Object> processInstanceDetails = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId, false);
				HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) processInstanceDetails.get("entity");
				// LISTA DEI PARAMETRI BASATI SULLE VARIABILI DELL'ISTANZA DI PROCESSO
				List<RestVariable> variables = processInstance.getVariables();
				// LISTA DEI PARAMETRI CALCOLATI PER STATISTICHE AVANZATE
				List<RestVariable> processInstanceMetadata = flowsCsvDispatcherService.getProcessInstanceMetadatas(processDefinitionKey, processInstance);
				List<RestVariable> totalStatisticMetadata = new ArrayList<>();
				totalStatisticMetadata.addAll(variables);
				totalStatisticMetadata.addAll(processInstanceMetadata);

				List<String> tupla = new ArrayList<>();
				//field comuni a tutte le Process Instances (Business Key, Start date)
				if (pi.getEndTime() == null){
					tupla.add("ATTIVO");
				} else {
					tupla.add("TERMINATO");
				}
				tupla.add(pi.getBusinessKey());
				tupla.add(utils.formattaDataOra(pi.getStartTime()));
				if (pi.getEndTime() != null) {
					tupla.add(utils.formattaDataOra(pi.getEndTime()));
				} else {
					tupla.add("");
				}
				if (pi.getDurationInMillis() != null) {
					tupla.add(pi.getDurationInMillis().toString());
				} else {
					tupla.add("");
				}
				tupla.add(pi.getId());
				String nameVariables = pi.getName();
				JSONObject jsonObj = new JSONObject(nameVariables);
				tupla.add(jsonObj.get("stato").toString());
				tupla.add(jsonObj.get("titolo").toString());
				tupla.add(jsonObj.get("descrizione").toString());
				tupla.add(jsonObj.get("initiator").toString());

				//field specifici per ogni procesDefinition (aggiungo alla tupla i valori delle totalStatisticMetadata)
				tupla.addAll(fields.stream()
						.map(el -> Utils.filterProperties(totalStatisticMetadata,
								(String) ((HashMap) el).get("varName")))
						.collect(Collectors.toList()));

				entriesIterable.add(utils.getArray(tupla));
			}
		}

		writer.writeAll(entriesIterable);
		writer.close();
	}
}