package it.cnr.si.flows.ng.service;

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
import com.opencsv.CSVWriter;
import javax.inject.Inject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;

import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.parseInt;

@Service
public class FlowsCsvDispatcherService {

	public static final String TITLE = "title";
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsCsvDispatcherService.class);

	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private ViewRepository viewRepository;
	@Inject
	private Utils utils;


	public List<RestVariable>  getProcessInstanceMetadatas(String processDefinitionKey, HistoricProcessInstanceResponse processInstance) throws IOException {
		List<RestVariable> processInstanceMetadata = new ArrayList<>();

		if (processDefinitionKey != null){
			switch(processDefinitionKey){  
			case "iscrizione-elenco-oiv": {
				processInstanceMetadata = getOivMetadata(processInstanceMetadata, processInstance);
			};break;		
			default:  {
				LOGGER.info("-- statistiche per: " + processDefinitionKey);
			};break;    


			}

		}
		return processInstanceMetadata;

	}

	public List<RestVariable>  getOivMetadata(List<RestVariable> processInstanceMetadata, HistoricProcessInstanceResponse processInstance) throws IOException {
		Long durataComplessiva = processInstance.getDurationInMillis();
		RestVariable metadato = new RestVariable();
		metadato.setName("durataComplessiva");
		if (durataComplessiva != null) {
			metadato.setValue(durataComplessiva);
		} else {
			metadato.setValue(0);
		}

		processInstanceMetadata.add(metadato);
		return processInstanceMetadata;
	}
}