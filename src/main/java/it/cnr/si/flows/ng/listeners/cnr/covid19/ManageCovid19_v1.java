package it.cnr.si.flows.ng.listeners.cnr.covid19;


import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.CNRPdfSignApparence;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.rest.common.api.DataResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")
public class ManageCovid19_v1 implements ExecutionListener {
	public static final String STATO_FINALE_GRADUATORIA = "statoFinaleDomanda";
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageCovid19_v1.class);
	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartCovid19SetGroupsAndVisibility_v1 startCovid19SetGroupsAndVisibility_v1;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private TaskService taskService;
	@Inject
	private FlowsCsvService flowsCsvService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private FlowsTaskService flowsTaskService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private FlowsPdfBySiglaRestService flowsPdfBySiglaRestService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private AceService aceService;
	@Inject
	private PdfSignApparence monitoraggioAttivitaCovid19;
	@Inject
	private Utils utils;

	private Expression faseEsecuzione;


	@Override
	public void notify(DelegateExecution execution) throws Exception {

		String processInstanceId = execution.getProcessInstanceId();
		String executionId = execution.getId();
		String stato = execution.getCurrentActivityName();
		String sceltaUtente = "start";

		if (execution.getVariable("sceltaUtente") != null) {
			sceltaUtente = (String) execution.getVariable("sceltaUtente");
		}

		LOGGER.info("ProcessInstanceId: " + processInstanceId);
		String faseEsecuzioneValue = "noValue";
		faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		LOGGER.info("-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);

		switch (faseEsecuzioneValue) {
		// START
		case "process-start": {
			// CONTROLLO UNICITA' SCHEDA -MESE ANNO TIPOLOGIA UTENTE
			controlloFlussoEsistente(execution);
			calcoloMeseNumerico(execution);
			startCovid19SetGroupsAndVisibility_v1.configuraVariabiliStart(execution);
		}
		break;
		case "firma-start": {
			
			String profiloDomanda = "indefinito";
			if (execution.getVariable("profiloDomanda") != null) {
				profiloDomanda = execution.getVariable("profiloDomanda").toString();
			}
			if (!profiloDomanda.equals("ricercatore-tecnologo")){
				LOGGER.info("L'utente {} ha  {} come profilo [{}] ", execution.getVariable("nomeCognomeUtente").toString(),  profiloDomanda);
				// TODO INSERIRE DOPO SETTEMBRE 
				//throw new BpmnError("412", "Livello ["+ execution.getVariable("livelloRichiedente").toString() +"] associato all'utenza: " + execution.getVariable("userNameUtente").toString() + " risulta non valido per l'inserimento del monitoraggio<br>");
			}
			
			// INSERIMENTO VARIABILI FLUSSO
			execution.setVariable("titolo", "Scheda " + execution.getVariable("tipoAttivita") + " - " + execution.getVariable("initiator"));
			execution.setVariable("descrizione", "Scheda Attività - " + execution.getVariable("mese") + " " + execution.getVariable("anno"));

			// CONTROLLO DATA AVVIO SMART WORKING
			if (execution.getVariable("tipoAttivita").toString().equals("programmazione")){
				if (execution.getVariable("dataAvvioSmartWorking") == null || execution.getVariable("dataAvvioSmartWorking").equals("null")) {
					String data01MeseCorrente = execution.getVariable("anno").toString() + "-" + execution.getVariable("meseNumerico").toString() + "-01T00:00:00.000Z";
					execution.setVariable("dataAvvioSmartWorking", data01MeseCorrente);
				}
			}

			//PARAMETRI GENERAZIONE PDF
			String tipoAttivita = "rendicontazione";
			if (execution.getVariable("tipoAttivita") != null) {
				tipoAttivita = execution.getVariable("tipoAttivita").toString();
			}
			String nomeFile = tipoAttivita + ".pdf";
			String labelFile = "Monitoraggio Attività Personale";
			String report = "/scrivaniadigitale/smart_working.jrxml";
			//tipologiaDoc è la tipologia del file
			String tipologiaDoc = Enum.PdfType.valueOf("monitoraggioAttivitaCovid19").name();
			String utenteFile = execution.getVariable("initiator").toString();

			//valoreParam per il json che racchiude i dati della stampa
			JSONObject valoreParamJson = new JSONObject();
			valoreParamJson.put("matricola", execution.getVariable("matricola"));
			valoreParamJson.put("nomeCognomeUtente", execution.getVariable("nomeCognomeUtente"));
			valoreParamJson.put("tipoContratto", execution.getVariable("tipoContratto"));
			valoreParamJson.put("cds", execution.getVariable("cds"));
			valoreParamJson.put("direttore", execution.getVariable("direttore"));
			valoreParamJson.put("mese", execution.getVariable("mese").toString());
			valoreParamJson.put("anno", execution.getVariable("anno").toString());
			valoreParamJson.put("attivita_svolta",Utils.sanitizeHtml(execution.getVariable("attivita")));
			valoreParamJson.put("tipoAttivita", execution.getVariable("tipoAttivita").toString());
			if (execution.getVariable("tipoAttivita").equals("programmazione")) {
				valoreParamJson.put("modalita", Utils.sanitizeHtml(execution.getVariable("modalita")));
				valoreParamJson.put("dataAvvioSmartWorking", execution.getVariable("dataAvvioSmartWorking"));
			}

			String valoreParam = valoreParamJson.toString();

			if (tipoAttivita.equals("rendicontazione")) {
				labelFile = "Rendicontazione Attività Personale";
				execution.setVariable("modalita", null);
				execution.setVariable("dataAvvioSmartWorking", null);
			} else {
				labelFile = "Programmazione Attività Personale";
			}
			// UPDATE VARIABILI FLUSSO
			utils.updateJsonSearchTerms(executionId, processInstanceId, stato);
			// GENERAZIONE PDF
			flowsPdfBySiglaRestService.makePdf(execution, nomeFile, labelFile, report, valoreParam, tipologiaDoc, processInstanceId, utenteFile);
		}
		break;
		case "firma-end": {
			if (sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, Enum.PdfType.valueOf("monitoraggioAttivitaCovid19").name(), monitoraggioAttivitaCovid19);
			}
		}
		break;
		case "modifica-start": {
			//AGGIORNA PARAMETRI
			String gruppoResponsabileProponenteOld = execution.getVariable("gruppoResponsabileProponente").toString();
			startCovid19SetGroupsAndVisibility_v1.configuraVariabiliStart(execution);
			String gruppoResponsabileProponente = execution.getVariable("gruppoResponsabileProponente").toString();
			// RIMOZIONE VISIBILITA' IN CASO CAMBIO DIRETTORE
			if (!gruppoResponsabileProponenteOld.equals(gruppoResponsabileProponente)) {
				runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileProponente, PROCESS_VISUALIZER);
				runtimeService.deleteGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileProponenteOld, PROCESS_VISUALIZER);
			}
		}
		case "protocollo-end": {
			if (sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, execution.getVariable("tipoAttivita").toString());
			}
		}
		break;
		case "endevent-covid19-start": {
			if((execution.getVariable("sceltaUtente").toString().equalsIgnoreCase("Firma")) || (execution.getVariable("sceltaUtente").toString().equalsIgnoreCase("Firma Multipla"))) {
				SimpleUtenteWebDto utente = aceService.getUtente(execution.getVariable("userNameUtente").toString());
				String tipoProfilo = utente.getPersona().getProfilo();
				String statoFinale = "AUTORIZZATO";
				if (tipoProfilo != null && (tipoProfilo.equals("1") || tipoProfilo.equals("2"))) {
					statoFinale = "PRESA D'ATTO";
				}  
				execution.setVariable(statoFinaleDomanda.name(), statoFinale);
				execution.setVariable("statoFinale", statoFinale);					
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			}
		}
		break;


		case "process-end": {
			//

		}
		break;
		// DEFAULT
		default: {
		}
		break;

		}
	}

	@Bean(name = {"monitoraggioAttivitaCovid19"})
	@ConfigurationProperties(prefix = "cnr.firma.covid19")
	public CNRPdfSignApparence create() throws IOException {
		return new CNRPdfSignApparence();
	}

	private void calcoloMeseNumerico(DelegateExecution execution) {

		//IMPOSTARE LA DATA COME 1 GIORNO DEL MESE CORRENTE
		String anno = execution.getVariable("anno").toString() ;
		String meseLettere = execution.getVariable("mese").toString() ;
		String meseNumerico; 
		switch(meseLettere){
		case "gennaio":
			meseNumerico = "01";
			break;
		case "febbraio":
			meseNumerico = "02";
			break;
		case "marzo":
			meseNumerico = "03";
			break;
		case "aprile":
			meseNumerico = "04";
			break;
		case "maggio":
			meseNumerico = "05";
			break;
		case "giugno":
			meseNumerico = "06";
			break;
		case "luglio":
			meseNumerico = "07";
			break;
		case "agosto":
			meseNumerico = "08";
			break;
		case "settembre":
			meseNumerico = "09";
			break;
		case "ottobre":
			meseNumerico = "10";
			break;
		case "novembre":
			meseNumerico = "11";
			break;
		case "dicembre":
			meseNumerico = "12";
			break;
		case "primo_semestre":
				meseNumerico = "01";
				break;
		case "secondo_semestre":
			meseNumerico = "07";
			break;
		default:
			meseNumerico = "Invalid month";
			break;
		} 
		execution.setVariable("meseNumerico", meseNumerico);

	}

	private void controlloFlussoEsistente(DelegateExecution execution) {
		// CONTROLLO UNICITA' SCHEDA -MESE ANNO TIPOLOGIA UTENTE
		String userNameUtente = execution.getVariable("initiator").toString();
		SimpleUtenteWebDto utente = aceService.getUtente(execution.getVariable("initiator").toString());
		String nomeCognomeUtente = utente.getPersona().getNome() + " " + utente.getPersona().getCognome();
		String mese = execution.getVariable("mese").toString();
		String anno = execution.getVariable("anno").toString();
		String tipoAttivita = execution.getVariable("tipoAttivita").toString();
		if (utente != null && mese != null && anno != null && tipoAttivita != null) {
			Map<String, String> req = new HashMap<>();
			req.put("mese", "text="+mese);
			req.put("anno", "text="+anno);
			req.put("tipoAttivita", "text="+tipoAttivita);
			req.put("processDefinitionKey", "covid19");
			req.put("nomeCognomeUtente", "text="+nomeCognomeUtente);
			String order = "ASC";
			Integer firstResult = -1;
			Integer maxResults = -1;
			String processDefinitionKey = "covid19";
			Boolean activeFlag = true;
			DataResponse flussiAttiviPerBando = flowsProcessInstanceService.search(req, processDefinitionKey, activeFlag, order, firstResult, maxResults, false);
			if (flussiAttiviPerBando.getSize() > 0) {
				throw new BpmnError("413", "Risulta già presente un flusso associato all'utenza: " + userNameUtente + 
						"<br>per il mese: " + mese + " - anno: " + anno + 
						"<br>per la tipologia: " + tipoAttivita + "<br>");
			} else {
				activeFlag = false;
				DataResponse flussiTerminatiPerBando = flowsProcessInstanceService.search(req, processDefinitionKey, activeFlag, order, firstResult, maxResults, false);
				req.put("statoFinaleDomanda", "text=ELIMINATO");
				DataResponse flussiEliminatiPerBando = flowsProcessInstanceService.search(req, processDefinitionKey, activeFlag, order, firstResult, maxResults, false);				
				if (flussiTerminatiPerBando.getSize() - flussiEliminatiPerBando.getSize() > 0) {
					throw new BpmnError("413", "Risulta già presente un flusso associato all'utenza: " + userNameUtente + 
							"<br>per il mese: " + mese + " - anno: " + anno + 
							"<br>per la tipologia: " + tipoAttivita + "<br>");
				}
			}

		}
	}
}
