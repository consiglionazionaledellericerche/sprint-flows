package it.cnr.si.flows.ng.service;

import com.opencsv.CSVWriter;
import it.cnr.si.domain.View;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.json.JSONArray;
import org.json.JSONException;
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

import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.parseInt;

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
    

	public List<HistoricProcessInstanceResponse> getProcessesStatistics(String processDefinitionKey, String startDateGreat, String startDateLess, boolean activeFlag) {
		Map<String, String> req = new HashMap<>();
		req.put("startDateGreat", startDateGreat);
		req.put("startDateLess", startDateLess);
		req.put(processDefinitionKey, processDefinitionKey);
		String order = "ASC";
		Integer firstResult = -1;
		Integer maxResults = -1;

		Map<String, Object>  flussi = flowsProcessInstanceService.search(req, processDefinitionKey, activeFlag, order, firstResult, maxResults);

		//VALORIZZAZIONE PARAMETRI STATISTICHE
		Integer domandeAttive = parseInt(flussi.get("totalItems").toString());

		LOGGER.debug("nr. domande: {} con activeFlag: {}", domandeAttive, activeFlag);
		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI ATTIVI
		return (List<HistoricProcessInstanceResponse>) flussi.get("processInstances");
	}



	public void  makeCsv(List<HistoricProcessInstanceResponse> processInstances, PrintWriter printWriter, String processDefinitionKey) throws IOException {
		// vista (campi e variabili) da inserire nel csv in base alla tipologia di flusso selezionato
		View view = null;
		CSVWriter writer = new CSVWriter(printWriter, '\t');
		if (!processDefinitionKey.equals(ALL_PROCESS_INSTANCES)) {
			view = viewRepository.getViewByProcessidType(processDefinitionKey, "export-csv");
			LOGGER.debug("view: {}", view);
		}
		ArrayList<String[]> entriesIterable = new ArrayList<>();
		boolean hasHeaders = false;
		ArrayList<String> headers = new ArrayList<>();
		headers.add("Stato Istanza");
		headers.add("Business Key");
		headers.add("Start Date");
		for (HistoricProcessInstanceResponse pi : processInstances) {
			String processInstanceId = pi.getId();
			Map<String, Object> processInstanceDetails = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
			HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) processInstanceDetails.get("entity");
			// LISTA DEI PARAMETRI BASATI SULLE VARIABILI DELL'ISTANZA DI PROCESSO
			List<RestVariable> variables = processInstance.getVariables();
			// LISTA DEI PARAMETRI CALCOLATI PER STATISTICHE AVANZATE
			List<RestVariable> processInstanceMetadata = flowsCsvDispatcherService.getProcessInstanceMetadatas(processDefinitionKey, processInstance);
			List<RestVariable> totalStatisticmetadata = new ArrayList<>();
			totalStatisticmetadata.addAll(variables);
			totalStatisticmetadata.addAll(processInstanceMetadata);

			ArrayList<String> tupla = new ArrayList<>();
			//field comuni a tutte le Process Instances (Business Key, Start date)
			if (pi.getEndTime() == null){
				tupla.add("ATTIVO");

			} else {
				tupla.add("TERMINATO");
			}
			tupla.add(pi.getBusinessKey());
			tupla.add(utils.formattaDataOra(pi.getStartTime()));

			//field specifici per ogni procesDefinition
			if (view != null) {
				try {
					JSONArray fields = new JSONArray(view.getView());
					for (int i = 0; i < fields.length(); i++) {
						JSONObject field = fields.getJSONObject(i);
						tupla.add(Utils.filterProperties(totalStatisticmetadata, field.getString("varName")));
						//solo per il primo ciclo, prendo le label dei field specifici
						if (!hasHeaders)
							headers.add(field.getString("label"));
					}
				} catch (JSONException e) {
					LOGGER.error("Errore nel processamento del JSON", e);
					throw new IOException(e);
				}
			}
			if (!hasHeaders) {
				//inserisco gli headers come intestazione dei field del csv
				entriesIterable.add(0, utils.getArray(headers));
				hasHeaders = true;
			}
			entriesIterable.add(utils.getArray(tupla));
		}
		writer.writeAll(entriesIterable);
		writer.close();
	}
}