package it.cnr.si.flows.ng.listeners.cnr.laboratoriCongiuntiDomande;


import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeLABEnum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeLABEnum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
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

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.initiator;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")
public class ManageProcessLaboratoriCongiuntiDomande_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessLaboratoriCongiuntiDomande_v1.class);


	@Value("${cnr.labcon.url}")
	private String urlLaboratoriCongiunti;
	@Value("${cnr.labcon.domandePath}")
	private String pathDomandeLaboratoriCongiunti;

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartLaboratoriCongiuntiDomandeSetGroupsAndVisibility startShortTermMobilityDomandeSetGroupsAndVisibility;
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
	private FlowsTaskService flowsTaskService;
	@Inject
	private ManagementService managementService;
	@Inject
	private RepositoryService repositoryService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private AceService aceService;
	@Inject
	private Utils utils;

	private Expression faseEsecuzione;

	public void restToApplicazioneLabConn(DelegateExecution execution, StatoDomandeLABEnum statoDomanda) {

		// @Value("${cnr.accordi-bilaterali.url}")
		// private String urlLaboratoriCongiunti;
		// @Value("${cnr.accordi-bilaterali.usr}")
		// private String usrAccordiBilaterali;	
		// @Value("${cnr.accordi-bilaterali.psw}")
		// private String pswAccordiBilaterali;
		//Double idDomanda = Double.parseDouble(execution.getVariable("idDomanda").toString());
		String idDomanda = execution.getVariable("idDomanda").toString();
		Map<String, Object> stmPayload = new HashMap<String, Object>()
		{
			{
				put("idDomanda", idDomanda);
				put("stato", statoDomanda.name().toString());
			}	
		};

		String url = urlLaboratoriCongiunti + pathDomandeLaboratoriCongiunti;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, stmPayload, ExternalApplication.LABCON);
	}


	@Override
	public void notify(DelegateExecution execution) throws Exception {

		Map<String, FlowsAttachment> attachmentList;
		String processInstanceId =  execution.getProcessInstanceId();
		String executionId =  execution.getId();
		String stato =  execution.getCurrentActivityName();
		String sceltaUtente = "start";
		if(execution.getVariable("sceltaUtente") != null) {
			sceltaUtente =  (String) execution.getVariable("sceltaUtente");	
		}
		String faseEsecuzioneValue = "noValue";
		// la variabile tipologiaRespinta tiene conto delle varie possibilità di come la domanda è staata respinta
		faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		LOGGER.info("ProcessInstanceId: " + processInstanceId + "-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);
		//CHECK PER ANNULLO FLUSSO 
		if (execution.getVariableInstance("motivazioneEliminazione") == null) {
			switch(faseEsecuzioneValue){  
			// START
			case "process-start": {
				startShortTermMobilityDomandeSetGroupsAndVisibility.configuraVariabiliStart(execution);
			};break;    	
			// START
			case "validazione-start": {
				utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
				execution.setVariable("tipologiaRespinta", "scadenzaTimer");
			};break;  
			case "validazione-end": {
				//flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
				String idDipartimento = execution.getVariable("dipartimentoId").toString();
				String gruppoValutatoreScientificoLABDipartimento = "valutatoreScientificoLABDipartimento@" + idDipartimento;
				runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoLABDipartimento, PROCESS_VISUALIZER);
				execution.setVariable("gruppoValutatoreScientificoLABDipartimento", gruppoValutatoreScientificoLABDipartimento);
				LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoLABDipartimento);
				// INPUT DEVE PREVEDERE LA DOMANDA PDF - NON GENERO LA DOMANDA
				//				String nomeFile="domandaShortTermMobility";
				//				String labelFile="Domanda";
				//				flowsPdfService.makePdf(nomeFile, processInstanceId);
				//				FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
				//				documentoGenerato.setLabel(labelFile);
				//				flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);
				if(sceltaUtente.equals("Respingi")) {
					execution.setVariable("tipologiaRespinta", "respintaResponsabile");
				}
			};break;  	 
			case "modifica-start": {
				restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.APERTA);
			};break;

			case "pre-accettazione-start": {
				if(sceltaUtente.equals("Respingi")) {
					execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.RESPINTA.toString());
					execution.setVariable("tipologiaRespinta", "respintaResponsabile");
					//restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.RESPINTA);
					utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.RESPINTA.toString());
				} else {
					if(sceltaUtente.equals("Annulla")) {
						execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.ANNULLATA.toString());
						//restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.ANNULLATA);
						utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.ANNULLATA.toString());
					}
					else{
						execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.VALIDATA.toString());
						//restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.VALIDATA);
						utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.VALIDATA.toString());
					}
				}
			};break; 			

			//			case "endevent-non-validata-start": {
			//				execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.RESPINTA);
			//				restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.RESPINTA);
			//				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.RESPINTA.toString());
			//			};break;    	
			//			case "endevent-validata-start": {
			//				execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.VALIDATA);
			//				restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.VALIDATA);
			//				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.VALIDATA.toString());
			//			};break;  
			//			case "endevent-annullata-start": {
			//				execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.ANNULLATA);
			//				restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.ANNULLATA);
			//				flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.ANNULLATA.toString());
			//			};break;
			// SUBFLUSSO VALIDAZIONE DIRIGENTE
			case "validazioneDirigente-end": {
				LOGGER.debug("**** validazioneDirigente-end");
			};break; 			
			case "endevent-respinta-start": {
				execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.RESPINTA.toString());
				restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.RESPINTA);
				execution.setVariable("statoFinale", Enum.StatoDomandeLABEnum.RESPINTA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
				if(execution.getVariable("tipologiaRespinta").toString().equals("scadenzaTimer")) {
					execution.setVariable("notaDomandaRespinta", "La Domanda è stata respinta per scadenza termini temporali Valutazione Dirigente");
				}
				if(execution.getVariable("tipologiaRespinta").toString().equals("respintaResponsabile")) {
					execution.setVariable("notaDomandaRespinta", "La Domanda è stata respinta dal Direttore");
				}
			};break;					
			case "endevent-autorizzata-start": {
				execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.AUTORIZZATA.toString());
				restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.AUTORIZZATA);
				execution.setVariable("statoFinale", Enum.StatoDomandeLABEnum.AUTORIZZATA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			};break;						
			case "accettazione-start": {
				LOGGER.debug("**** accettazione-start");
				execution.setVariable("tipologiaRespinta", "respintaFormale");
			};break; 			
			case "accettazione-end": {
				LOGGER.debug("**** accettazione-end");
				execution.setVariable("domandaCorrenteAccettataFlag", "false");
				execution.setVariable("tutteDomandeAccettateFlag", "false");
				//				int domandaCorrenteAccettata = 0;
				if(!sceltaUtente.equals("Respingi")) {
					//	domandaCorrenteAccettata = 1;
					execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.ACCETTATA.toString());
					execution.setVariable("domandaCorrenteAccettataFlag", "true");
					//restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.ACCETTATA);
					utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.ACCETTATA.toString());
				}
				List<ProcessInstance> processinstancesListaDomandeAccettatePerBando = runtimeService.createProcessInstanceQuery()
						.processDefinitionKey("laboratori-congiunti-domande")
						.variableValueEquals(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.ACCETTATA.toString())
						.variableValueEquals("idBando", execution.getVariable("idBando"))
						.list();
				List<ProcessInstance> processinstancesListaDomandeAttivePerBando = runtimeService.createProcessInstanceQuery()
						.processDefinitionKey("laboratori-congiunti-domande")
						.variableValueEquals("idBando", execution.getVariable("idBando"))
						.list();
				LOGGER.debug("**** domande bando: {}, nr attive = {} - nr accettate= {} - domanda corrente accettata: {}", execution.getVariable("idBando"), processinstancesListaDomandeAttivePerBando.size(), processinstancesListaDomandeAccettatePerBando.size(), execution.getVariable("domandaCorrenteAccettataFlag").toString());

				if ((processinstancesListaDomandeAttivePerBando.size()  == processinstancesListaDomandeAccettatePerBando.size() + 1) &&  (execution.getVariable("domandaCorrenteAccettataFlag").toString().equals("true"))) {
					execution.setVariable("tutteDomandeAccettateFlag", "true");
					processinstancesListaDomandeAccettatePerBando.forEach(( processInstance) -> {
						if (flowsProcessInstanceService.getProcessInstance(processInstance.getId()).getName().contains(Enum.StatoDomandeLABEnum.ACCETTATA.toString())) {
							runtimeService.signal(processInstance.getId());
							LOGGER.info("-- sblocco la processInstance: " + processInstance.getName() + " (" + processInstance.getId() + ") ");			
						}
					});

				}
			};break; 


			case "pre-valutazione-start": {
				LOGGER.debug("**** pre-valutazione-start");
				//				if(execution.getVariable("domandaCorrenteAccettataFlag").equals("true")) {
				//					runtimeService.signal(execution.getProcessInstanceId());
				//					LOGGER.info("-- sblocco la Corrente processInstance: " + execution.getProcessBusinessKey() + " (" + execution.getProcessInstanceId() + ") ");
				//				}
			};break; 

			case "valutazione-scientifica-start": {
				LOGGER.debug("**** valutazione-scientifica-start");
			};break; 			

			case "valutazione-scientifica-end": {
				LOGGER.info("-- valutazione-scientifica: valutazione-scientifica");
				execution.setVariable("domandaCorrenteValutataFlag", "false");
				if(execution.getVariable("sceltaUtente").equals("CambiaDipartimento")) {
					String idDipartimento = execution.getVariable("dipartimentoId").toString();
					String gruppoValutatoreScientificoLABDipartimento = "valutatoreScientificoLABDipartimento@" + idDipartimento;
					runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoLABDipartimento, PROCESS_VISUALIZER);
					execution.setVariable("gruppoValutatoreScientificoLABDipartimento", gruppoValutatoreScientificoLABDipartimento);
					LOGGER.debug("Imposto i gruppi dipartimento : {} - del flusso {}", idDipartimento, gruppoValutatoreScientificoLABDipartimento);
				} else {
					execution.setVariable("domandaCorrenteValutataFlag", "true");
					execution.setVariable(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.VALUTATA_SCIENTIFICAMENTE.toString());
					//restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.VALUTATA_SCIENTIFICAMENTE);
					utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeLABEnum.VALUTATA_SCIENTIFICAMENTE.toString());	
					Double punteggioTotale= 
							Double.parseDouble(execution.getVariable("punteggio_originalita_scientifica").toString().replaceAll(",", ".")) 
							+ Double.parseDouble(execution.getVariable("punteggio_qualificazione_proponenti").toString().replaceAll(",", "."))
							+ Double.parseDouble(execution.getVariable("punteggio_documentazione_presentazione_progetto").toString().replaceAll(",", "."))
							+ Double.parseDouble(execution.getVariable("punteggio_utilita_necessita_collaborazione").toString().replaceAll(",", "."))
							+ Double.parseDouble(execution.getVariable("punteggio_potenzialita_ricerca_sviluppo_CNR").toString().replaceAll(",", "."))
							+ Double.parseDouble(execution.getVariable("punteggio_potenzialita_investimenti_privati").toString().replaceAll(",", "."))
							+ Double.parseDouble(execution.getVariable("punteggio_sfruttamento_diffusione_risultati").toString().replaceAll(",", "."))
							+ Double.parseDouble(execution.getVariable("punteggio_congruita_economica_progetto").toString().replaceAll(",", "."));
					execution.setVariable("punteggio_totale", punteggioTotale.toString());
					//CREAZIONE PDF VALUTAZIONE
					//PARAMETRI GENERAZIONE PDF x SIGLA PRINT
					String nomeFile="valutazioneLaboratoriCongiunti";
					String labelFile="Scheda Valutazione Domanda";
					String report = "/scrivaniadigitale/valutazioneLaboratoriCongiunti.jrxml";
					//tipologiaDoc è la tipologia del file
					String tipologiaDoc = Enum.PdfType.valueOf("valutazioneLaboratoriCongiunti").name();
					String utenteFile = execution.getVariable("initiator").toString();

					// UPDATE VARIABILI FLUSSO
					utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
					// GENERAZIONE PDF
					List<String> listaVariabiliHtml = new ArrayList<String>();
					listaVariabiliHtml.add("commento");
					flowsPdfService.makePdfBySigla(tipologiaDoc, processInstanceId, listaVariabiliHtml, labelFile, report);
					//flowsPdfService.makePdf(nomeFile, processInstanceId);

					FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
					documentoGenerato.setLabel(labelFile);
					flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_originalita_scientifica", execution.getVariable("punteggio_originalita_scientifica"));
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_qualificazione_proponenti", execution.getVariable("punteggio_qualificazione_proponenti"));
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_documentazione_presentazione_progetto", execution.getVariable("punteggio_documentazione_presentazione_progetto"));
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_utilita_necessita_collaborazione", execution.getVariable("punteggio_utilita_necessita_collaborazione"));
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_potenzialita_ricerca_sviluppo_CNR", execution.getVariable("punteggio_potenzialita_ricerca_sviluppo_CNR"));
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_potenzialita_investimenti_privati", execution.getVariable("punteggio_potenzialita_investimenti_privati"));
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_sfruttamento_diffusione_risultati", execution.getVariable("punteggio_sfruttamento_diffusione_risultati"));
					runtimeService.setVariable(execution.getProcessInstanceId(), "punteggio_congruita_economica_progetto", execution.getVariable("punteggio_congruita_economica_progetto"));
				}
			};break;	

			case "graduatoria-start": {
				LOGGER.debug("**** graduatoria-start");
				// VERIFICA TUTTE LE DOMANDE DI FLUSSI ATTIVI PER QUEL BANDO e PER QUEL DIPARTIMENTO
				List<ProcessInstance> processinstancesListaPerBandoDipartimento = runtimeService.createProcessInstanceQuery()
						.processDefinitionKey("laboratori-congiunti-domande")
						.variableValueEquals("idBando", execution.getVariable("idBando"))
						.variableValueEquals("dipartimentoId", execution.getVariable("dipartimentoId"))
						.list();
				List<ProcessInstance> processinstancesListaDomandeValutatePerBandoDipartimento = runtimeService.createProcessInstanceQuery()
						.processDefinitionKey("laboratori-congiunti-domande")
						.variableValueEquals("idBando", execution.getVariable("idBando"))
						.variableValueEquals("dipartimentoId", execution.getVariable("dipartimentoId"))
						.variableValueEquals(statoFinaleDomanda.name(), Enum.StatoDomandeLABEnum.VALUTATA_SCIENTIFICAMENTE.toString())
						.list();

				if ((processinstancesListaPerBandoDipartimento.size() == processinstancesListaDomandeValutatePerBandoDipartimento.size() + 1) &&  (execution.getVariable("domandaCorrenteValutataFlag").toString().equals("true"))) {
					//START FLUSSO BANDI
					// Creazione flusso bando se non presente la presenza del flusso bando 

					List<ProcessInstance> processinstancesBandiPerBandoDipartimento = runtimeService.createProcessInstanceQuery()
							.processDefinitionKey("laboratori-congiunti-bando-dipartimento")
							.variableValueEquals("idBando", execution.getVariable("idBando"))
							.variableValueEquals("dipartimentoId", execution.getVariable("dipartimentoId"))
							.list();
					if (processinstancesBandiPerBandoDipartimento.size() == 0)
					{
						String processDefinitionId = repositoryService.createProcessDefinitionQuery().processDefinitionKeyLike("laboratori-congiunti-bando-dipartimento").orderByProcessDefinitionVersion().desc().list().get(0).getId();
						//START del flusso bando
						String siglaDipartimento = aceBridgeService.getUoById(Integer.parseInt(execution.getVariable("dipartimentoId").toString())).getSigla();
						Map<String, Object> data = new HashMap<>();
						String titoloBando = "Verbale Graduatoria Bando " + execution.getVariable("idBando") + "-" + siglaDipartimento;
						data.put(Enum.VariableEnum.titolo.name(), titoloBando);
						data.put(Enum.VariableEnum.descrizione.name(), "Flusso per la gestione del " + titoloBando);
						data.put(Enum.VariableEnum.initiator.name(), "app.scrivaniadigitale");
						data.put("idBando", execution.getVariable("idBando"));
						data.put("dipartimentoId", execution.getVariable("dipartimentoId"));
						data.put("processDefinitionId", processDefinitionId);
						data.put(initiator.name(), "app.scrivaniadigitale");

						LOGGER.info("-- EFFETTUO START FLUSSO laboratori-congiunti-bando-dipartimento CON titolo: " + data.get("titolo") + " descrizione" + data.get("descrizione")  + " initiator " + data.get("initiator")  + " idBando" + data.get("idBando") );

						//flowsTaskService.startProcessInstance(processDefinitionId, data);
						flowsTaskService.startProcessInstanceAsApplication(processDefinitionId, data,"app.scrivaniadigitale");
					} else {
						// Aziona il receive task "elenco-domande"
						LOGGER.info("-- Bando già avviato: " + processinstancesBandiPerBandoDipartimento.get(0).getBusinessKey() );
					}
				}

			};break; 			

			//TIMERS
			case "timer-chiusura-bando-end": {
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
			restToApplicazioneLabConn(execution, Enum.StatoDomandeLABEnum.CANCELLATA);
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
