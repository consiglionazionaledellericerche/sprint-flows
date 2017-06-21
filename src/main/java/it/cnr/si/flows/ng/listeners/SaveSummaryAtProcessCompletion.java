package it.cnr.si.flows.ng.listeners;

import com.google.common.net.MediaType;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.service.SummaryPdfService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

@Component
public class SaveSummaryAtProcessCompletion implements ActivitiEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveSummaryAtProcessCompletion.class);

    @Inject
    private SummaryPdfService summaryPdfService;

    @Override
    public void onEvent(ActivitiEvent event) {
        if ( event.getType() == ActivitiEventType.PROCESS_COMPLETED ) {
            RuntimeService runtimeService = event.getEngineServices().getRuntimeService();
            LOGGER.info("Processo {} con nome {} completato. Salvo il summary.",
                        event.getExecutionId(),
                        runtimeService.getVariable(event.getExecutionId(), "title"));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String fileName = null;
            try {
                fileName = summaryPdfService.createPdf(event.getProcessInstanceId(), outputStream);
            } catch (IOException | ParseException e) {
                LOGGER.error("Errore nella creazione del summary pdf FINALE per la Process Instance {}. \n" +
                                     "Errore: {}", event.getProcessInstanceId(), e.getMessage());
            }

            FlowsAttachment pdfToDB = new FlowsAttachment();
            pdfToDB.setBytes(outputStream.toByteArray());
            pdfToDB.setAzione(FlowsAttachment.Azione.Caricamento);
            pdfToDB.setTaskId(null);
            pdfToDB.setTaskName(null);
            pdfToDB.setTime(new Date());
            pdfToDB.setName(fileName);
            pdfToDB.setFilename(fileName);
            pdfToDB.setMimetype(MediaType.PDF.toString());

            runtimeService.setVariable(event.getExecutionId(), fileName, pdfToDB);
        }
    }

    /**
     * Se per caso la creazione del summary non riesca, non e' un problema bloccante
     * Si puo' ricreare in un secondo momento
     */
    @Override
    public boolean isFailOnException() {
        return false;
    }


}
