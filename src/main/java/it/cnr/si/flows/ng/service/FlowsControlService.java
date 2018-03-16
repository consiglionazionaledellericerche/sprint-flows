package it.cnr.si.flows.ng.service;

import java.io.IOException;
import java.text.ParseException;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;

@Service
public class FlowsControlService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsControlService.class);
	@Autowired
	private FlowsAttachmentService attachmentService;
	
	// Verifica che il file sia firmato p7m 
	public void verificaFileFirmato_Cades(DelegateExecution execution, String nomeFile) throws IOException, ParseException {
		String processInstanceId =  execution.getProcessInstanceId();
		//FlowsAttachment fileDaCaricare =  (FlowsAttachment) execution.getVariable(nomeFile);
		FlowsAttachment filePresente = attachmentService.getAttachementsForProcessInstance(processInstanceId).get(nomeFile);
		String estensioneFilePresente = filePresente.getMimetype();
		String nomeFilePresente = filePresente.getFilename();
		if((nomeFilePresente.indexOf("p7m") >= 0) || (estensioneFilePresente.equalsIgnoreCase("application/x-pkcs7-mime"))){
			LOGGER.debug("--- il file : {} è p7m con estensione {}", nomeFilePresente, estensioneFilePresente);
		} else {
			LOGGER.debug("--- il file : {} NON è p7m, ma con estensione {} ", nomeFilePresente, estensioneFilePresente);
			throw new BpmnError("412", "Il file " + nomeFilePresente + " non risulta firmato CAdES<br>il file dovrebbe avere estensione <b>p7m</b><br>");
		}
		
	}
	
	// Verifica che il file sia firmato p7m che .PAdES
	public void verificaFileFirmato_Cades_Pades(DelegateExecution execution, String nomeFile) throws IOException, ParseException {
		String processInstanceId =  execution.getProcessInstanceId();
		//FlowsAttachment fileDaCaricare =  (FlowsAttachment) execution.getVariable(nomeFile);
		FlowsAttachment filePresente = attachmentService.getAttachementsForProcessInstance(processInstanceId).get(nomeFile);
		String estensioneFilePresente = filePresente.getMimetype();
		String nomeFilePresente = filePresente.getFilename();
		if((nomeFilePresente.indexOf("p7m") >= 0) || (estensioneFilePresente.equalsIgnoreCase("application/x-pkcs7-mime") || nomeFilePresente.indexOf("signed.pdf") >= 0) ){
			LOGGER.debug("--- il file : {} risulta firmato con estensione {}", nomeFilePresente, estensioneFilePresente);
		} else {
			LOGGER.debug("--- il file : {} NON risulta firmato, ma con estensione {} ", nomeFilePresente, estensioneFilePresente);
			throw new BpmnError("412", "Il file " + nomeFilePresente + " non risulta firmato<br> - se firmato CAdES il file dovrebbe avere estensione <b>p7m</b><br> - se firmato PAdES il file dovrebbe avere estensione<b>.signed.pdf</b><br>");
		}
		
	}
	
	// Verifica che il file sia firmato PAdES 
	public void verificaFileFirmato_Pades(DelegateExecution execution, String nomeFile) throws IOException, ParseException {
		String processInstanceId =  execution.getProcessInstanceId();
		//FlowsAttachment fileDaCaricare =  (FlowsAttachment) execution.getVariable(nomeFile);
		FlowsAttachment filePresente = attachmentService.getAttachementsForProcessInstance(processInstanceId).get(nomeFile);
		String estensioneFilePresente = filePresente.getMimetype();
		String nomeFilePresente = filePresente.getFilename();
		if(nomeFilePresente.indexOf("signed.pdf") >= 0) {
			LOGGER.debug("--- il file : {} è .signed.pdf con estensione {}", nomeFilePresente, estensioneFilePresente);
		} else {
			LOGGER.debug("--- il file : {} NON è è idoneao alla firma, ma con estensione {} ", nomeFilePresente, estensioneFilePresente);
			throw new BpmnError("412", "Il file " + nomeFilePresente + " non risulta firmato PAdES<br>il file dovrebbe avere estensione <b>.signed.pdf</b><br>");
		}
		
	}
}

