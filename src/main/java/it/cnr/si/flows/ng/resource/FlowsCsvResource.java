package it.cnr.si.flows.ng.resource;


import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsCsvService;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


@Controller
@RequestMapping("api")
public class FlowsCsvResource {


	public static final String BYTES = "bytes";

	@Inject
	private FlowsCsvService flowsCsvService;


//    todo: TEST

	/**
	 * Export csv: esporta il result-set di una search sulle Process Instances in un file Csv
	 *
	 * @param res                  the res
	 * @param processDefinitionKey La process definition key della ricerca (oppurer "all")
	 * @param startDateGreat       the start date great
	 * @param idStruttura          idStruttura
	 * @param startDateLess        the start date less
	 * @throws IOException the io exception
	 */
	@RequestMapping(value = "/makeStatisticCsv", headers = "Accept=application/vnd.ms-excel", method = RequestMethod.GET, produces = "application/vnd.ms-excel")
	@ResponseBody
	@Timed
	@Secured(AuthoritiesConstants.USER)
	public void makeStatisticCsv(
			HttpServletResponse res,
			@RequestParam("processDefinitionKey") String processDefinitionKey,
			@RequestParam("idStruttura") String idStruttura,
			@RequestParam("startDateGreat") String startDateGreat,
			@RequestParam("startDateLess") String startDateLess) throws IOException {

		// Lista processi attivi
		List<HistoricProcessInstanceResponse> activeProcessInstances = flowsCsvService.getProcessesStatistics(processDefinitionKey, idStruttura, startDateGreat, startDateLess, true);

		// Lista processi terminati
		List<HistoricProcessInstanceResponse> terminatedProcessInstances = flowsCsvService.getProcessesStatistics(processDefinitionKey, idStruttura, startDateGreat, startDateLess, false);

		// Lista processi totali
		activeProcessInstances.addAll(terminatedProcessInstances);

		//creo il csv corrispondente
		String fileName = "statistiche_" + processDefinitionKey + ".csv";
		res.setContentType("application/vnd.ms-excel");
		res.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

		flowsCsvService.makeCsv(activeProcessInstances, res.getWriter(), processDefinitionKey);
	}
}