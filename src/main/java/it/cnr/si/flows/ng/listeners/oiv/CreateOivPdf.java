package it.cnr.si.flows.ng.listeners.oiv;

import it.cnr.si.flows.ng.dto.FlowsAttachment;


import it.cnr.si.service.OivPdfService;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.net.MediaType;

import javax.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;

import java.util.Date;


import static it.cnr.si.flows.ng.utils.Enum.Azione.Caricamento;



@Component
public class CreateOivPdf implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateOivPdf.class);

	@Inject
	private OivPdfService oivPdfService;
	
	@Override
	public void notify(DelegateExecution execution) throws Exception {
		//(OivPdfService oivPdfService = new OivPdfService();
		
		
		String processInstanceId =  execution.getProcessInstanceId();		
		LOGGER.info("ProcessInstanceId: " + processInstanceId);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String fileName = null;
        try {
            fileName = oivPdfService.createPdf(processInstanceId, outputStream);
        } catch (IOException | ParseException e) {
            LOGGER.error("Errore nella creazione del file pdf  per la Process Instance {}. \n" +
                                 "Errore: {}", processInstanceId, e.getMessage());
        }
        String variableFileName = fileName.replaceAll(" ", "_");
        variableFileName = variableFileName.replace(".pdf", "");
		LOGGER.info("avvio la generazione del pdf: " + fileName + " con variabile: " + variableFileName);
        FlowsAttachment pdfToDB = new FlowsAttachment();
        pdfToDB.setBytes(outputStream.toByteArray());
        pdfToDB.setAzione(Caricamento);
        pdfToDB.setTaskId(null);
        pdfToDB.setTaskName(null);
        pdfToDB.setTime(new Date());
        pdfToDB.setName(variableFileName);
        pdfToDB.setFilename(fileName);
        pdfToDB.setMimetype(MediaType.PDF.toString());

        execution.setVariable(variableFileName, pdfToDB);

	}}
