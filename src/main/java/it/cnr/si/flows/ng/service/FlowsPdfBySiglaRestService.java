package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.PdfType;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.ViewRepository;
import it.cnr.si.service.RestPdfSiglaService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Enum.Azione.Aggiornamento;
import static it.cnr.si.flows.ng.utils.Enum.Azione.Caricamento;

@Service
@Profile({"cnr","iss"})
public class FlowsPdfBySiglaRestService {

    public static final String TITLE = "title";
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsPdfBySiglaRestService.class);

    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private FlowsProcessDiagramService flowsProcessDiagramService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private ViewRepository viewRepository;
    @Inject
    private Utils utils;
    @Inject
    private Environment env;
    @Inject
    private TaskService taskService;
    @Inject
    private HistoryService historyService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private RestPdfSiglaService restPdfSiglaService;

    public Pair<String, byte[]> makePdf(DelegateExecution execution, String nomeFile, String labelFile, String report, String valoreParam, String tipologiaDoc, String processInstanceId, String utenteFile) {
        JSONObject variabliStampa = new JSONObject();

        variabliStampa.put("nomeFile", nomeFile);
        variabliStampa.put("report", report);

        JSONArray array = new JSONArray();
        JSONObject arrayParams = new JSONObject();

        JSONObject arrayParamsKey = new JSONObject();
        JSONObject nomeParams = new JSONObject();
        arrayParamsKey.put("paramType", "java.lang.String");
        arrayParamsKey.put("valoreParam", valoreParam);
        nomeParams.put("nomeParam", "REPORT_DATA_SOURCE");
        arrayParamsKey.put("key", nomeParams);

        array.put(arrayParamsKey);
        variabliStampa.put("params", array);

        // RICHIESTA DEL PDF
        byte[] pdfByteArray = null;
        pdfByteArray = restPdfSiglaService.getSiglaPdf(variabliStampa.toString());

        //"Allego" il file nel flusso
        Map<String, FlowsAttachment> attachments = flowsAttachmentService.getCurrentAttachments(execution);
        PdfType pdfType = Enum.PdfType.valueOf(tipologiaDoc);

        FlowsAttachment attachment = attachments.get(pdfType.name());
        if (attachment != null) {
            //aggiorno il pdf
            attachment.setFilename(nomeFile);
            attachment.setLabel(labelFile);
			attachment.setName(pdfType.name());
            attachment.setAzione(Aggiornamento);
            attachment.setUsername(utenteFile);
        } else {
            //salvo il pdf nel flusso
            attachment = new FlowsAttachment();
            attachment.setAzione(Caricamento);
            attachment.setPath(runtimeService.getVariable(processInstanceId, "pathFascicoloDocumenti", String.class));
			attachment.setTaskId(null);
            attachment.setTaskName(null);
            attachment.setTime(new Date());
            attachment.setName(pdfType.name());
            attachment.setLabel(labelFile);
			attachment.setFilename(nomeFile);
            attachment.setMimetype(com.google.common.net.MediaType.PDF.toString());
            attachment.setUsername(utenteFile);
        }

        flowsAttachmentService.saveAttachment(execution, pdfType.name(), attachment, pdfByteArray);

        return Pair.of(nomeFile, pdfByteArray);
    }

    private String formatDate(Date date) {
        return date != null ? utils.formattaDataOra(date) : "";
    }

}