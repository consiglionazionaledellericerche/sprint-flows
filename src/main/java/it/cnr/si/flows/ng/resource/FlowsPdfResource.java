package it.cnr.si.flows.ng.resource;


import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.security.AuthoritiesConstants;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.Map;


@Controller
@RequestMapping("api")
public class FlowsPdfResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsPdfResource.class);
    private static final String VALUTAZIONE_ESPERIENZE_JSON = "valutazioneEsperienze_json";
    @Inject
    private FlowsPdfService pdfService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private HistoryService historyService;

    /**
     * Crea e restituisce il summary pdf del flusso.
     *
     * @param processInstanceId : processInstanceId del workflow di cui si vuole generare il summary
     * @return the response entity
     * @throws Exception the exception
     * @return: restituisce il pdf generato
     */
    @RequestMapping(value = "/summaryPdf", headers = "Accept=application/pdf", method = RequestMethod.GET, produces = "application/pdf")
    @ResponseBody
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<byte[]> makeSummaryPdf(
            @RequestParam("processInstanceId") String processInstanceId,
            HttpServletRequest req) {

        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String fileName = pdfService.makeSummaryPdf(processInstanceId, outputStream);

            HttpHeaders headers = new HttpHeaders();
            ResponseEntity<byte[]> resp;
            headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            headers.setContentType(MediaType.parseMediaType("application/pdf"));
            headers.setContentLength(outputStream.toByteArray().length);
            resp = new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);

            return resp;
        } catch (Exception e) {
            LOGGER.error("Errore nella creazione del Summary.pdf per il flusso {}: ", processInstanceId, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Crea e restituisce il un pdf del tipo specificato.
     *
     * @param processInstanceId processInstanceId del flusso
     * @param tipologiaDoc      la "tipologia" di pdf da create (ad es.: "rigetto")

     * @return the response entity
     * @throws Exception the exception
     * @return: restituisce il pdf generato
     */
    @RequestMapping(value = "/makePdf", headers = "Accept=application/pdf", method = RequestMethod.GET, produces = "application/pdf")
    @ResponseBody
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<byte[]> makePdf(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("tipologiaDoc") String tipologiaDoc) {
        //carico le processVariablwes e rimappo in formato json il campo stringa "valutazioneEsperienze_json"
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processInstanceId(processInstanceId)
                .singleResult();
        JSONObject processvariables = mappingValutazioniEsperienze(historicProcessInstance.getProcessVariables());
        //creo il pdf corrispondente
        String utenteRichiedente = processvariables.getString("nomeRichiedente");
        String fileName = tipologiaDoc + "-" + utenteRichiedente + ".pdf";
        byte[] pdfByteArray = pdfService.makePdf(Enum.PdfType.valueOf(tipologiaDoc), processvariables, fileName, utenteRichiedente, processInstanceId);
        //popolo gli headers della response
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<byte[]> resp;
        headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        headers.setContentType(MediaType.parseMediaType("application/pdf"));
        headers.setContentLength(pdfByteArray.length);
        resp = new ResponseEntity<>(pdfByteArray, headers, HttpStatus.OK);

        return resp;
    }

    private JSONObject mappingValutazioniEsperienze(Map<String, Object> processVariables) {
        JSONObject variables = new JSONObject(processVariables);

        if (variables.has(VALUTAZIONE_ESPERIENZE_JSON)) {
            JSONArray esperienze = new JSONArray(variables.getString(VALUTAZIONE_ESPERIENZE_JSON));
            variables.put(VALUTAZIONE_ESPERIENZE_JSON, esperienze);
        }
        return variables;
    }
}