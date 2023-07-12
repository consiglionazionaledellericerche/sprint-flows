package it.cnr.si.flows.ng.listeners.cnr.acquistiICT;


import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeAccordiInternazionaliEnum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.ExternalMessageService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.runtime.Job;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")
public class ManageProcessAcquistiICT_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAcquistiICT_v1.class);


	@Value("${cnr.abil.url}")
	private String urlAcquistiICT;
	@Value("${cnr.abil.domandePath}")
	private String pathDomandeAcquistiICT;

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartAcquistiICTSetGroupsAndVisibility startAccordiInternazionaliDomandeSetGroupsAndVisibility;
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
	@Inject
	private Utils utils;
	@Inject
	private FlowsPdfBySiglaRestService flowsPdfBySiglaRestService;

	private Expression faseEsecuzione;

	public void restToApplicazioneAcquistiICT(DelegateExecution execution, StatoDomandeAccordiInternazionaliEnum statoDomanda) {

		// @Value("${cnr.accordi-bilaterali.url}")
		// private String urlAcquistiICT;
		// @Value("${cnr.accordi-bilaterali.usr}")
		// private String usrAcquistiICT;
		// @Value("${cnr.accordi-bilaterali.psw}")
		// private String pswAcquistiICT;
		Double idDomanda = Double.parseDouble(execution.getVariable("idDomanda").toString());
		Map<String, Object> abilPayload = new HashMap<String, Object>()
		{
			{
				put("idDomanda", idDomanda);
				put("stato", statoDomanda.name().toString());
			}
		};

		String url = urlAcquistiICT + pathDomandeAcquistiICT;
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
			    String propostaHtml = String.valueOf(execution.getVariable("propostaDiRicerca"));
			    String propostaPulita = Utils.sanitizeHtml(propostaHtml);
			    execution.setVariable("propostaDiRicerca", propostaPulita);
			    			    
				startAccordiInternazionaliDomandeSetGroupsAndVisibility.configuraVariabiliStart(execution);
				// GENERO LA DOMANDA ---OLD
				//String nomeFile="domandaAcquistiICT";
				//flowsPdfService.makePdfBeforeStartPi(nomeFile, processInstanceId);

				//PARAMETRI GENERAZIONE PDF x SIGLA PRINT
				String tipoAttivita = "rendicontazione";
				if (execution.getVariable("tipoAttivita") != null) {
					tipoAttivita = execution.getVariable("tipoAttivita").toString();
				}
				String nomeFile="domandaAcquistiICT";
				String labelFile = "Domanda Accordi Bilaterali";
				String report = "/scrivaniadigitale/domandaAcquistiICT.jrxml";
				//tipologiaDoc è la tipologia del file
				String tipologiaDoc = Enum.PdfType.valueOf("domandaAcquistiICT").name();
				String utenteFile = execution.getVariable("initiator").toString();

				// UPDATE VARIABILI FLUSSO
				utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
				// GENERAZIONE PDF
				List<String> listaVariabiliHtml = new ArrayList<String>();
				listaVariabiliHtml.add("propostaDiRicerca");
				flowsPdfService.makePdfBySigla(tipologiaDoc, processInstanceId, listaVariabiliHtml, labelFile, report);

			};break;
			// START
			
			case "firma-determina-start": {
			
			};break;
			case "firma-determina-end": {
				
			};break;
			
			case "modifica-determina-start": {
				
			};break;
			case "modifica-determina-end": {
				
			};break;
			
			case "protocollo-determina-start": {
				
			};break;
			case "protocollo-determina-end": {
				
			};break;
			
			case "predisposizione-proposta-aggiudicazione-start": {
				
			};break;
			case "predisposizione-proposta-aggiudicazione-end": {
				
			};break;
			
			case "firma-proposta-aggiudicazione-start": {
				
			};break;
			case "firma-proposta-aggiudicazione-end": {
				
			};break;
			
			case "modifica-proposta-aggiudicazione-start": {
				
			};break;
			case "modifica-proposta-aggiudicazione-end": {
				
			};break;
			
			case "protocollo-proposta-aggiudicazione-start": {
				
			};break;
			case "protocollo-proposta-aggiudicazione-end": {
				
			};break;
			
			case "espletamento-procedura-start": {
				
			};break;
			case "espletamento-procedura-end": {
				
			};break;
			
			case "predisposizione-ordine-start": {
				
			};break;
			case "predisposizione-ordine-end": {
				
			};break;
			
			case "firma-ordine-start": {
				
			};break;
			case "firma-ordine-end": {
				
			};break;
			
			case "modifica-ordine-start": {
				
			};break;
			case "modifica-ordine-end": {
				
			};break;

			
			case "protocollo-ordine-start": {
				
			};break;
			case "protocollo-ordine-end": {
				
			};break;

			
			case "consuntivo-start": {
				
			};break;
			case "consuntivo-end": {
				
			};break;

			
			case "carica-ordine-mepa-start": {
				
			};break;
			case "carica-ordine-mepa-end": {
				
			};break;

			
			case "protocollo-ordine-mepa-start": {
				
			};break;
			case "protocollo-ordine-mepa-end": {
				
			};break;

			
			case "invio-ordine-mepa-start": {
				
			};break;
			case "invio-ordine-mepa-end": {
				
			};break;
			
			
			
			
			case "valutazione-scientifica-end": {
				LOGGER.info("-- valutazione-scientifica: valutazione-scientifica");
				if(execution.getVariable("sceltaUtente").equals("CambiaDipartimento")) {
					String idDipartimento = execution.getVariable("dipartimentoId").toString();
					String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@" + idDipartimento;
					runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
					execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
					LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoDipartimento);
				} else {
					String nomeFile="valutazioneProgettoAcquistiICT";
					String labelFile="Scheda Valutazione Domanda";
					execution.setVariable("punteggio_totale", (Double.parseDouble(execution.getVariable("punteggio_pianoDiLavoro").toString().replaceAll(",", ".")) + Double.parseDouble(execution.getVariable("punteggio_qualitaProgetto").toString().replaceAll(",", "."))+ Double.parseDouble(execution.getVariable("punteggio_valoreAggiunto").toString().replaceAll(",", "."))+ Double.parseDouble(execution.getVariable("punteggio_qualitaGruppoDiRicerca").toString().replaceAll(",", "."))));
					flowsPdfService.makePdf(nomeFile, processInstanceId);
					FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
					documentoGenerato.setLabel(labelFile);
					flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);
				}
			};break;
			case "validazione-end": {
				utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
				String idDipartimento = execution.getVariable("dipartimentoId").toString();
				String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@" + idDipartimento;
				runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
				execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
				LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoDipartimento);
				// GENERO LA DOMANDA
				//				String nomeFile="domandaAcquistiICT";
				//				String labelFile="Domanda";
				//				flowsPdfService.makePdf(nomeFile, processInstanceId);
				//				FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
				//				documentoGenerato.setLabel(labelFile);
				//				flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);
			};break;
			// START
			case "validazione-start": {
				utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
			};break;
			case "valutazione-domande-bando-start": {
				restToApplicazioneAcquistiICT(execution, Enum.StatoDomandeAccordiInternazionaliEnum.VALUTATA_SCIENTIFICAMENTE);
			};break;
			case "endevent-respinta-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA RESPINTA");
				restToApplicazioneAcquistiICT(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				execution.setVariable("statoFinale", Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			};break;
			case "endevent-non-autorizzata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA NON AUTORIZZATA");
				if(execution.getVariable("sceltaUtente") != "Respingi") {
					execution.setVariable("notaDomandaRespinta", "Scadenza termini temporali Valutazione Dirigente");
				}
				restToApplicazioneAcquistiICT(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				execution.setVariable("statoFinale", "NON AUTORIZZATA");
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			};break;
			case "endevent-annullata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA ANNULLATA");
				restToApplicazioneAcquistiICT(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				execution.setVariable("statoFinale", "ANNULLATA");
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			};break;
			case "endevent-non-finanziata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA NON FINANZIATA");
				restToApplicazioneAcquistiICT(execution, Enum.StatoDomandeAccordiInternazionaliEnum.RESPINTA);
				execution.setVariable("statoFinale", "NON FINANZIATA");
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			};break;
			case "endevent-approvata-start": {
				execution.setVariable(statoFinaleDomanda.name(), "DOMANDA APPROVATA");
				execution.setVariable("statoFinale", "APPROVATA");
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
				restToApplicazioneAcquistiICT(execution, Enum.StatoDomandeAccordiInternazionaliEnum.ACCETATA);
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
			restToApplicazioneAcquistiICT(execution, Enum.StatoDomandeAccordiInternazionaliEnum.CANCELLATA);
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
