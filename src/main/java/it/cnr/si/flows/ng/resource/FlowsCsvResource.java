package it.cnr.si.flows.ng.resource;


import com.codahale.metrics.annotation.Timed;
import com.opencsv.CSVWriter;
import it.cnr.si.flows.ng.service.FlowsCsvService;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;



@Controller
@RequestMapping("api")
public class FlowsCsvResource {


	public static final String BYTES = "bytes";

	@Inject
	private FlowsCsvService flowsCsvService;



	/**
	 * Export csv: esporta il result-set di una search sulle Process Instances in un file Csv
	 *
	 * @param res                  the res
	 * @param processDefinitionKey La process definition key della ricerca (oppurer "all")
	 * @param params               i "parametri della ricerca
	 * @throws IOException the io exception
	 */
	@RequestMapping(value = "/makeStatisticCsv", headers = "Accept=application/vnd.ms-excel", method = RequestMethod.GET, produces = "application/vnd.ms-excel")
	@ResponseBody
	@Timed
	@Secured(AuthoritiesConstants.USER)
	public void makeStatisticCsv(
			HttpServletResponse res,
			@RequestParam("processDefinitionKey") String processDefinitionKey,
			@RequestParam("startDateGreat") String startDateGreat,
			@RequestParam("startDateLess") String startDateLess) throws ParseException, IOException {

		boolean activeFlag = true;
		// Lista processi attivi
		List<HistoricProcessInstanceResponse> processvariables = flowsCsvService.getProcessesStatistics(processDefinitionKey, startDateGreat, startDateLess, activeFlag);
		//creo il pdf corrispondente
		String fileName = processDefinitionKey + "-Statistics";
		flowsCsvService.makeCsv(processvariables, res.getWriter(), processDefinitionKey);
	}

}