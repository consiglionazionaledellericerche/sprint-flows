package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliDomande;



import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeAccordiInternazionaliEnum;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

@Component
@Profile("cnr")
public class ManageProcessAccordiInternazionaliDomande_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAccordiInternazionaliDomande_v1.class);
	public static final String STATO_FINALE_DOMANDA = "statoFinaleDomanda";

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartAccordiInternazionaliDomandeSetGroupsAndVisibility startAccordiInternazionaliSetGroupsAndVisibility;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private ExternalMessageService externalMessageService;	

	private Expression faseEsecuzione;
	
	

	public void restToApplicazioneAccordiBilaterali(DelegateExecution execution, StatoDomandeAccordiInternazionaliEnum statoDomanda) {

		String urlAccordiBilaterali = "www.google.it";
		// @Value("${cnr.accordi-bilaterali.url}")
		// private String urlAccordiBilaterali;
		// @Value("${cnr.accordi-bilaterali.usr}")
		// private String usrAccordiBilaterali;	
		// @Value("${cnr.accordi-bilaterali.psw}")
		// private String pswAccordiBilaterali;
		Double idDomanda = Double.parseDouble(execution.getVariable("idDomanda").toString());
		Map<String, Object> abilPayload = new HashMap<String, Object>()
		{
			{
				put("idDomanda", idDomanda);
				put("stato", statoDomanda.name().toString());
			}	
		};	
		externalMessageService.createExternalMessage(urlAccordiBilaterali, ExternalMessageVerb.POST, abilPayload);

	}


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
		case "valutazione-scientifica-end": {
			LOGGER.info("-- valutazione-scientifica: valutazione-scientifica");
			String nomeFile="valutazioneProgettoAccordiBilaterali";
			String labelFile="Scheda Valutazione Domanda";
			execution.setVariable("punteggio_totale", (Integer.parseInt(execution.getVariable("punteggio_pianoDiLavoro").toString()) + Integer.parseInt(execution.getVariable("punteggio_qualitaProgetto").toString())+ Integer.parseInt(execution.getVariable("punteggio_valoreAggiunto").toString())+ Integer.parseInt(execution.getVariable("punteggio_qualitaGruppoDiRicerca").toString())));
			flowsPdfService.makePdf(nomeFile, processInstanceId);
			FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
			documentoGenerato.setLabel(labelFile);
			flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);
			
		};break;  
		case "validazione-end": {
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
			String idDipartimento = execution.getVariable("dipartimentoId").toString();
			String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@" + idDipartimento;
			runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
			execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
			LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoDipartimento);
		};break;  			
		// START
		case "validazione-start": {
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
		};break;  
		case "valutazione-domande-bando-start": {
			restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.VALUTATA_SCIENTIFICAMENTE);
		};break;    
		case "endevent-respinta-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA RESPINTA");
			restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
		};break;    	
		case "endevent-non-autorizzata-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA NON AUTORIZZATA");
			restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
		};break;  
		case "endevent-annullata-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA ANNULLATA");
			restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
		};break;  
		case "endevent-non-finanziata-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA NON FINANZIATA");
			restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
		};break;  	
		case "endevent-approvata-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "DOMANDA APPROVATA");
			restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.ACCETATA);
		};break;  
		//TIMERS
		case "timer2-end": {
			int nrNotifiche = 1;
			if(execution.getVariable("numeroNotificheTimer2") != null) {
				nrNotifiche = (Integer.parseInt(execution.getVariable("numeroNotificheTimer2").toString()) + 1);
			} 
			execution.setVariable("numeroNotificheTimer2", nrNotifiche);
			LOGGER.debug("Timer2 nrNotifiche: {}", nrNotifiche);
		};break;  
		case "timer2-end-script": {
			int nrNotifiche = 1;
			if(execution.getVariable("numeroNotificheTimer2") != null) {
				nrNotifiche = (Integer.parseInt(execution.getVariable("numeroNotificheTimer2").toString()) + 1);
			} 
			execution.setVariable("numeroNotificheTimer2", nrNotifiche);
			LOGGER.debug("Timer2 nrNotifiche: {}", nrNotifiche);
		};break;  
		
		// DEFAULT  
		default:  {
		};break;    

		} 
	}
}
