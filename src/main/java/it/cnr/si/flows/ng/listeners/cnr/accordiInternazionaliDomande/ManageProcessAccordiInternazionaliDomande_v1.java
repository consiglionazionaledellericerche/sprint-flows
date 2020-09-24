package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliDomande;


import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeAccordiInternazionaliEnum;
import it.cnr.si.service.ExternalMessageService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")
public class ManageProcessAccordiInternazionaliDomande_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAccordiInternazionaliDomande_v1.class);


	@Value("${cnr.abil.url}")
	private String urlAccordiBilaterali;
	@Value("${cnr.abil.domandePath}")
	private String pathDomandeAccordiBilaterali;

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartAccordiInternazionaliDomandeSetGroupsAndVisibility startAccordiInternazionaliDomandeSetGroupsAndVisibility;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private ExternalMessageService externalMessageService;	
	@Inject
	private TaskService taskService;
	@Inject
	private ManagementService managementService;


	private Expression faseEsecuzione;

	public void restToApplicazioneAccordiBilaterali(DelegateExecution execution, StatoDomandeAccordiInternazionaliEnum statoDomanda) {

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

		String url = urlAccordiBilaterali + pathDomandeAccordiBilaterali;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, abilPayload, ExternalApplication.ABIL);
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
		//CHECK PER ANNULLO FLUSSO 
		if (execution.getVariableInstance("motivazioneEliminazione") == null) {
			switch(faseEsecuzioneValue){  
			// START
			case "process-start": {
				startAccordiInternazionaliDomandeSetGroupsAndVisibility.configuraVariabiliStart(execution);
				// GENERO LA DOMANDA
				String nomeFile="domandaAccordiBilaterali";
				flowsPdfService.makePdfBeforeStart(nomeFile, processInstanceId);
			};break;
			// START
			case "valutazione-scientifica-end": {
				LOGGER.info("-- valutazione-scientifica: valutazione-scientifica");
				if(execution.getVariable("sceltaUtente").equals("CambiaDipartimento")) {
					String idDipartimento = execution.getVariable("dipartimentoId").toString();
					String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@" + idDipartimento;
					runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
					execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
					LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoDipartimento);
				} else {
					String nomeFile="valutazioneProgettoAccordiBilaterali";
					String labelFile="Scheda Valutazione Domanda";
					execution.setVariable("punteggio_totale", (Double.parseDouble(execution.getVariable("punteggio_pianoDiLavoro").toString().replaceAll(",", ".")) + Double.parseDouble(execution.getVariable("punteggio_qualitaProgetto").toString().replaceAll(",", "."))+ Double.parseDouble(execution.getVariable("punteggio_valoreAggiunto").toString().replaceAll(",", "."))+ Double.parseDouble(execution.getVariable("punteggio_qualitaGruppoDiRicerca").toString().replaceAll(",", "."))));
					flowsPdfService.makePdf(nomeFile, processInstanceId);
					FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
					documentoGenerato.setLabel(labelFile);
					flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);
				}
			};break;  
			case "validazione-end": {
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
				String idDipartimento = execution.getVariable("dipartimentoId").toString();
				String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@" + idDipartimento;
				runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
				execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
				LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoDipartimento);
				// GENERO LA DOMANDA
//				String nomeFile="domandaAccordiBilaterali";
//				String labelFile="Domanda";
//				flowsPdfService.makePdf(nomeFile, processInstanceId);
//				FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
//				documentoGenerato.setLabel(labelFile);
//				flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);
			};break;  			
			// START
			case "validazione-start": {
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
			};break;  
			case "valutazione-domande-bando-start": {
				restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.VALUTATA_SCIENTIFICAMENTE);
			};break;    
			case "endevent-respinta-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA RESPINTA");
				restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "RESPINTA");
			};break;    	
			case "endevent-non-autorizzata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA NON AUTORIZZATA");
				if(execution.getVariable("sceltaUtente") != "Respingi") {
					execution.setVariable("notaDomandaRespinta", "Scadenza termini temporali Valutazione Dirigente");
				}
				restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "NON AUTORIZZATA");
			};break;  
			case "endevent-annullata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA ANNULLATA");
				restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "ANNULLATA");
			};break;  
			case "endevent-non-finanziata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA NON FINANZIATA");
				restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "NON FINANZIATA");
			};break;  	
			case "endevent-approvata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA APPROVATA");
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA.toString());
				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "APPROVATA");
				restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.ACCETATA);
			};break;   	
			case "notificatask-start": {
				LOGGER.debug("**** notificatask-start");
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
		} else {
			restToApplicazioneAccordiBilaterali(execution, Enum.StatoDomandeAccordiInternazionaliEnum.CANCELLATA);
			List<Job> timerAttivi = managementService.createJobQuery().timers().processInstanceId(processInstanceId).list();
			timerAttivi.forEach(singoloTimer -> {
				if (singoloTimer.getId() != null) {
					LOGGER.debug("cancello il timer: {}", singoloTimer.getId());
					managementService.deleteJob(singoloTimer.getId());
				}
			});
		}
	}
}
