package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionali;



import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;
import it.cnr.si.flows.ng.listeners.cnr.acquisti.service.AcquistiService;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

import java.util.Map;

import javax.inject.Inject;

@Component
@Profile("cnr")
public class ManageProcessAccordiInternazionali_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAccordiInternazionali_v1.class);
	public static final String STATO_FINALE_DOMANDA = "statoFinaleDomanda";

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsAttachmentService attachmentService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private AcquistiService acquistiService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private StartAccordiInternazionaliSetGroupsAndVisibility startAccordiInternazionaliSetGroupsAndVisibility;
	@Inject
	private RuntimeService runtimeService;



	private Expression faseEsecuzione;


	@Override
	public void notify(DelegateExecution execution) throws Exception {
		//(OivPdfService oivPdfService = new OivPdfService();

		Map<String, FlowsAttachment> attachmentList;
		String processInstanceId =  execution.getProcessInstanceId();
		String executionId =  execution.getId();
		String stato =  execution.getCurrentActivityName();
		String sceltaUtente = "start";
		if(execution.getVariable("sceltaUtente") != null) {
			sceltaUtente =  (String) execution.getVariable("sceltaUtente");	
		}

		LOGGER.info("ProcessInstanceId: " + processInstanceId);
		String faseEsecuzioneValue = "noValue";
		faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		LOGGER.info("-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);

		switch(faseEsecuzioneValue){  
		// START
		case "process-start": {
			startAccordiInternazionaliSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    
		// START
		case "validazione-start": {
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
		};break;  
		case "validazione-end": {
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
			String idDipartimento = execution.getVariable("idDipartimento").toString();
			String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@" + idDipartimento;
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
			execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
			LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoDipartimento);
		};break;  			

		case "firma-verbale-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "verbale");
			}
		};break; 
		case "protocollo-verbale-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "verbale");
			}
		};break;  	
		case "endevent-annullata-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA ANNULLATA");
		};break;  	
		case "endevent-negativa-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA NEGATIVA");
		};break;  
		case "endevent-non-finanziata-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA NON FINANZIATA");
		};break;  	
		case "endevent-approvata-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA APPROVATA");
		};break;  

		// DEFAULT  
		default:  {
		};break;    

		} 
	}
}
