package it.cnr.si.flows.ng.listeners.oiv.service;

import it.cnr.si.flows.ng.service.FlowsPdfService;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;


@Service
public class CreateOivPdf {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateOivPdf.class);

    @Inject
    private FlowsPdfService flowsPdfService;

    public Pair<String, byte[]> creaPdfOiv(DelegateExecution execution, String tipologiaDoc) throws IOException, ParseException {

        String processInstanceId = execution.getProcessInstanceId();


        LOGGER.info("ProcessInstanceId: " + processInstanceId);
        LOGGER.info("STAMPA la seguente tipologia di documento: " + tipologiaDoc);

        return flowsPdfService.makePdf(tipologiaDoc, processInstanceId);

    }
}
