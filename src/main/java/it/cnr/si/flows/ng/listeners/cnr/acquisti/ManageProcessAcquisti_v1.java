package it.cnr.si.flows.ng.listeners.cnr.acquisti;



import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.service.ProtocolloDocumentoService;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import it.cnr.si.spring.storage.StorageObject;
import it.cnr.si.spring.storage.StoreService;
import it.cnr.si.flows.ng.listeners.cnr.acquisti.service.AcquistiService;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Inject;

@Component
@Profile("cnr")
public class ManageProcessAcquisti_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAcquisti_v1.class);
	public static final String STATO_FINALE_DOMANDA = "statoFinaleDomanda";

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsAttachmentService attachmentService;
	@Inject
	private StartAcquistiSetGroupsAndVisibility startAcquistiSetGroupsAndVisibility;
	@Inject
	private DittaCandidata dittaCandidata;	
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private AcquistiService acquistiService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private ExternalMessageService externalMessageService;
	@Inject
	AceService aceService;
	@Inject
	private StoreService storeService;

	private Expression faseEsecuzione;

	public void pubblicaFilePubblicabili(DelegateExecution execution, Boolean pubblicaFlag) {
		for (int i = 0; i < 1000; i++) {
			String nomeFile = "allegati" + i;
			if(execution.getVariable(nomeFile) != null) {
				FlowsAttachment documentoCorrente = (FlowsAttachment) execution.getVariable(nomeFile);
				LOGGER.info("-- documentoCorrente: " + documentoCorrente.getLabel() );
				if(documentoCorrente.isPubblicazioneTrasparenza()) {
					attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), pubblicaFlag);
				}
				if(documentoCorrente.isPubblicazioneUrp()) {
					attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), pubblicaFlag);
				}
			} else {
				break;
			}
		}
	}



	public void pubblicaTuttiFilePubblicabili(DelegateExecution execution) {

		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);

			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente);
			if(documentoCorrente.isPubblicazioneUrp()) {
				attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), true);					
			}
			if(documentoCorrente.isPubblicazioneTrasparenza()) {
				attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), true);					
			}
		}
	}
	public void pubblicaFilePubblicabiliURP(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);
			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente);
			if(documentoCorrente.isPubblicazioneUrp()) {
				attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), true);					
			}
		}
	}

	public void pubblicaFilePubblicabiliTrasparenza(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);
			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente);
			if(documentoCorrente.isPubblicazioneTrasparenza()) {
				attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), true);					
			}
		}
	}
	public void pubblicaFileMultipliPubblicabili(DelegateExecution execution, String nomeDocumento, Boolean pubblicaFlag) {
		for (int i = 0; i < 1000; i++) {
			if(execution.getVariable(nomeDocumento +"[" + i + "]") != null) {
				FlowsAttachment documentoCorrente = (FlowsAttachment) execution.getVariable(nomeDocumento +"[" + i + "]");
				LOGGER.info("-- documentoCorrente: " + documentoCorrente );
				if(documentoCorrente.isPubblicazioneTrasparenza()) {
					attachmentService.setPubblicabileTrasparenza(execution, documentoCorrente.getName(), pubblicaFlag);
				}
				if(documentoCorrente.isPubblicazioneUrp()) {
					attachmentService.setPubblicabileUrp(execution, documentoCorrente.getName(), pubblicaFlag);
				}
			} else {
				break;
			}
		}
	}
	public void CalcolaTotaleImpegni(DelegateExecution execution) {
		double importoTotaleNetto = 0.0;
		double importoTotaleLordo = 0.0;

		String impegniString = (String) execution.getVariable("impegni_json");
		JSONArray impegni = new JSONArray(impegniString);

		for ( int i = 0; i < impegni.length(); i++) {

			JSONObject impegno = impegni.getJSONObject(i);

			try {
				importoTotaleNetto += impegno.getDouble("importoNetto");
			} catch (JSONException e) {
				LOGGER.error("Formato Impegno Non Valido {} nel flusso {} - {}", impegno.getString("importoNetto"), execution.getId(), execution.getVariable("title"));
				throw new BpmnError("400", "Formato Impegno Non Valido: " + impegno.getString("importoNetto"));
			}
			impegno.put("importoLordo", (double) Math.round(100*((impegno.getDouble("importoNetto")) * (1+(impegno.getDouble("percentualeIva"))/100)))/100);
			try {
				importoTotaleLordo += impegno.getDouble("importoLordo");
			} catch (JSONException e) {
				LOGGER.error("Formato Impegno Non Valido {} nel flusso {} - {}", impegno.getString("importoLordo"), execution.getId(), execution.getVariable("title"));
				throw new BpmnError("400", "Formato Impegno Non Valido: " + impegno.getString("importoLordo"));
			}			
			impegno.put("uo_label", aceBridgeService.getUoById(Integer.parseInt(impegno.get("uo").toString())).getDenominazione());
		}

		execution.setVariable("impegni_json", impegni.toString());
		execution.setVariable("importoTotaleNetto", importoTotaleNetto);
		execution.setVariable("importoTotaleLordo", importoTotaleLordo);
	}
	// FUNZIONE CHE CONTROLLA LA LISTA DEI SOCUMENTI CHE DEVONO ESSERE PUBBLICATI IN TRASPARENZA (SE PRESENTI DEVONO ESSERE PUBBLICATI ALTRIMENTI IL FLUSSO SI BLOCCA)
	public void controllaFilePubblicabiliTrasparenza(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		String errorMessage = "<b>il flusso non può essere terminato perché<br>i seguenti file devono risulare pubblicati in trasparenza:<br>";
		int nrFilesMancanti = 0;
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);
			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente.getFilename());
			if((documentoCorrente.getName().equals("decisioneContrattare")  
					|| documentoCorrente.getName().equals("modificheVariantiArt106")
					|| documentoCorrente.getName().equals("bandoAvvisi")
					|| documentoCorrente.getName().equals("letteraInvito")
					|| documentoCorrente.getName().equals("provvedimentoAmmessiEsclusi")
					|| documentoCorrente.getName().equals("provvedimentoNominaCommissione")
					|| documentoCorrente.getName().equals("provvedimentoAggiudicazione")
					|| documentoCorrente.getName().equals("elencoVerbali")
					|| documentoCorrente.getName().equals("modificheVariantiArt106")
					|| documentoCorrente.getName().equals("stipula")
					|| documentoCorrente.getName().equals("avvisoPostInformazione"))
					& (!documentoCorrente.getStati().toString().contains("PubblicatoTrasparenza"))) {
				nrFilesMancanti = nrFilesMancanti +1;
				errorMessage = errorMessage + " - " + documentoCorrente.getName();					
			}
		}
		if (nrFilesMancanti>0) {
			throw new BpmnError("500", errorMessage+"</b><br>");
		}
	}
	// FUNZIONE CHE CONTROLLA LA LISTA DEI SOCUMENTI CHE DEVONO ESSERE PUBBLICATI IN TRASPARENZA (SE PRESENTI DEVONO ESSERE PUBBLICATI ALTRIMENTI IL FLUSSO SI BLOCCA)
	public void prepareFilesToSigla(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getCurrentAttachments(execution);
		for (String key : attachmentList.keySet()) {
			FlowsAttachment documentoCorrente = attachmentList.get(key);
			String aspectSiglaDecisioneContrattare = "P:sigla_contratti_aspect:doc_flusso_decisione_contrattare";
			String aspectSiglaCommonsPubblicazione = "P:sigla_commons_aspect:flusso_pubblicazione";
			LOGGER.info("Key = " + key + ", documentoCorrente = " + documentoCorrente.getFilename());
			if(documentoCorrente.getName().equals("decisioneContrattare")){
				StorageObject fileKey = storeService.getStorageObjectBykey(documentoCorrente.getUrl());
				if(!storeService.hasAspect(fileKey, aspectSiglaDecisioneContrattare)) {
					storeService.addAspect(fileKey, aspectSiglaDecisioneContrattare);					
				}
				if(!storeService.hasAspect(fileKey, aspectSiglaCommonsPubblicazione)) {
					storeService.addAspect(fileKey, aspectSiglaCommonsPubblicazione);			 
				}
				Map<String, Object> metadataPropertiesAspectSiglaDecisioneContrattare = new HashMap<String, Object>();
				metadataPropertiesAspectSiglaDecisioneContrattare.put("sigla_contratti_aspect_decisione_contrattare:name", documentoCorrente.getName());
				metadataPropertiesAspectSiglaDecisioneContrattare.put("sigla_contratti_aspect_decisione_contrattare:label", documentoCorrente.getLabel());
				metadataPropertiesAspectSiglaDecisioneContrattare.put("sigla_contratti_aspect_decisione_contrattare:filename", documentoCorrente.getFilename());
				Map<String, Object> metadataPropertiesAspectSiglaCommonsPubblicazione = new HashMap<String, Object>();
				metadataPropertiesAspectSiglaCommonsPubblicazione.put("sigla_commons_aspect:pubblicazione_trasparenza", documentoCorrente.isPubblicazioneTrasparenza());
				storeService.updateProperties(metadataPropertiesAspectSiglaDecisioneContrattare  , fileKey);
				storeService.updateProperties(metadataPropertiesAspectSiglaCommonsPubblicazione  , fileKey);
			}
		}
	}

	public Map<String, Object> createSiglaPayload(DelegateExecution execution) throws ParseException {
		Map<String, Object> metadatiAcquisto = new HashMap<String, Object>()
		{
			{
				DateFormat format = new SimpleDateFormat("yyyy");
				// ESERCIZIO 
				if(execution.getVariable("startDate") != null){
					String strDate = format.format(execution.getVariable("startDate"));  
					put("esercizio", Integer.parseInt(strDate));
				}
				// CD_UNITA_ORGANIZZATIVA 
				if(execution.getVariable("idStruttura") != null){
					put("cd_unita_organizzativa", aceBridgeService.getUoById(Integer.parseInt(execution.getVariable("idStruttura").toString())).getCdsuo().toString());
				}
				// DT_REGISTRAZIONE 
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
				dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Vatican"));
				Date endDate = new Date();
				String endStrDate = dateFormat.format(endDate);  
				put("dt_registrazione", endStrDate);
				// CD_TERZO_RESP 
				if(execution.getVariable("rup") != null){
					PersonaWebDto rupUser = aceService.getPersonaByUsername(execution.getVariable("rup").toString());
					put("cd_terzo_resp", rupUser.getCodiceFiscale());
				}
				// CD_TERZO_FIRMATARIO 
				if(execution.getVariable("usernameFirmatarioContratto") != null){
					PersonaWebDto firmatarioUser = aceService.getPersonaByUsername(execution.getVariable("usernameFirmatarioContratto").toString());
					put("cd_terzo_firmatario", firmatarioUser.getCodiceFiscale());
				}
				// FIG_GIUR_EST 
				if(execution.getVariable("pIvaCodiceFiscaleDittaAggiudicataria") != null){
					put("fig_giur_est", execution.getVariable("pIvaCodiceFiscaleDittaAggiudicataria").toString());
				}
				// NATURA_CONTABILE 
				put("natura_contabile", "P");
				if(execution.getVariable("tipologiaAcquisizioneId") != null){
					String tipologiaAcquisizioneId = execution.getVariable("tipologiaAcquisizioneId").toString();
					// CD_PROC_AMM 
					if(tipologiaAcquisizioneId.equals("11")) {
						put("cd_proc_amm", "PA55");
					}
					if(tipologiaAcquisizioneId.equals("12")) {
						put("cd_proc_amm", "PR55");
					}	
					if(tipologiaAcquisizioneId.equals("13")) {
						put("cd_proc_amm", "PN56");
					}	
					if(tipologiaAcquisizioneId.equals("14")) {
						put("cd_proc_amm", "PN57");
					}	
					if(tipologiaAcquisizioneId.equals("15")) {
						put("cd_proc_amm", "DC58");
					}	
					if(tipologiaAcquisizioneId.equals("23")) {
						put("cd_proc_amm", "PNS");
					}	
					if(tipologiaAcquisizioneId.equals("22")) {
						put("cd_proc_amm", "PNSS");
					}	
					// FL_MEPA 
					if(tipologiaAcquisizioneId.equals("11") || tipologiaAcquisizioneId.equals("21") ) {
						put("fl_mepa", "Y");
					} else {
						put("fl_mepa", "N");
					}
				}
				// CD_PROC_AMM 
				if(execution.getVariable("strumentoAcquisizioneId") != null){
					String strumentoAcquisizioneId = execution.getVariable("strumentoAcquisizioneId").toString();
					if(strumentoAcquisizioneId.equals("12")) {
						put("cd_proc_amm", "PNSS");
					}	
				}
				// OGGETTO 
				if(execution.getVariable("descrizione") != null){
					put("oggetto", execution.getVariable("descrizione").toString());
				}
				// CD_PROTOCOLLO 				
				put("cd_protocollo", execution.getProcessBusinessKey().toString());
				DateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				// DT_STIPULA 
				if(execution.getVariable("dataStipulaContratto") != null){
					put("dt_stipula", dateFormat.format(inputDateFormat.parse(execution.getVariable("dataStipulaContratto").toString())));		        	
				}
				// DT_INIZIO_VALIDITA 
				if(execution.getVariable("dataInizioValiditaContratto") != null){
					put("dt_inizio_validita", dateFormat.format(inputDateFormat.parse(execution.getVariable("dataInizioValiditaContratto").toString())));		        	
				}
				// DT_FINE_VALIDITA 
				if(execution.getVariable("dataFineValiditaContratto") != null){
					put("dt_fine_validita", dateFormat.format(inputDateFormat.parse(execution.getVariable("dataFineValiditaContratto").toString())));		        	
				}
				// IM_CONTRATTO_PASSIVO 
				put("im_contratto_passivo", new BigDecimal(execution.getVariable("importoTotaleLordo").toString()));
				// CD_TIPO_ATTO 
				put("cd_tipo_atto", "DET");
				if(runtimeService.getVariable(execution.getProcessInstanceId(), "decisioneContrattare", FlowsAttachment.class) != null) {
					FlowsAttachment determina = runtimeService.getVariable(execution.getProcessInstanceId(), "decisioneContrattare", FlowsAttachment.class);
					// DS_ATTO 
					put("ds_atto", determina.getLabel() + " Prot." + determina.getMetadati().get("numeroProtocollo") + " del " + dateFormat.format(inputDateFormat.parse(determina.getMetadati().get("dataProtocollo").toString())));
				}
				// CD_PROTOCOLLO_GENERALE 
				if(runtimeService.getVariable(execution.getProcessInstanceId(), "contratto", FlowsAttachment.class) != null) {
					FlowsAttachment contratto = runtimeService.getVariable(execution.getProcessInstanceId(), "contratto", FlowsAttachment.class);
					put("cd_protocollo_generale", contratto.getLabel() + " Prot." + contratto.getMetadati().get("numeroProtocollo") + " del " + dateFormat.format(inputDateFormat.parse(contratto.getMetadati().get("dataProtocollo").toString())));
					// FL_PUBBLICA_CONTRATTO 
					if(contratto.isPubblicazioneTrasparenza()) {
						put("fl_pubblica_contratto", Boolean.valueOf("true"));
					} else {
						put("fl_pubblica_contratto", Boolean.valueOf("false"));
					}
				}
				if(runtimeService.getVariable(execution.getProcessInstanceId(), "stipula", FlowsAttachment.class) != null) {
					FlowsAttachment stipula = runtimeService.getVariable(execution.getProcessInstanceId(), "stipula", FlowsAttachment.class);
					// CD_PROTOCOLLO_GENERALE 
					String cdProtocolloGenerale = stipula.getLabel();
					// ESERCIZIO_PROTOCOLLO 
					if(stipula.getDataProtocollo() != null && stipula.getNumeroProtocollo() != null){
						put("esercizio_protocollo", Integer.parseInt(format.format(inputDateFormat.parse(stipula.getDataProtocollo()))));	
						cdProtocolloGenerale = stipula.getLabel() + " Prot." + stipula.getNumeroProtocollo() + " del " + dateFormat.format(inputDateFormat.parse(stipula.getDataProtocollo()));
					}
					put("cd_protocollo_generale", cdProtocolloGenerale);
					// FL_PUBBLICA_CONTRATTO 
					if(stipula.isPubblicazioneTrasparenza()) {
						put("fl_pubblica_contratto", Boolean.valueOf("true"));
					} else {
						put("fl_pubblica_contratto", Boolean.valueOf("false"));
					}
				}
				// FL_ART82 
				put("fl_art82", "N");
				// CD_CIG 
				if(execution.getVariable("cig") != null){
					put("cd_cig", execution.getVariable("cig").toString());
				}
				// CD_CUP 
				if(execution.getVariable("cup") != null){
					put("cd_cup", execution.getVariable("cup").toString());
				}
				// IM_CONTRATTO_PASSIVO_NETTO 
				if(execution.getVariable("importoTotaleNetto") != null){
					put("im_contratto_passivo_netto", new BigDecimal(execution.getVariable("importoTotaleNetto").toString()));
				}
			}
		};	

		return metadatiAcquisto;
	}
	public void releaseDocumentInSigla(DelegateExecution execution) {
		Map<String, FlowsAttachment> attachmentList = attachmentService.getAttachementsForProcessInstance(execution.getProcessInstanceId());
		for (String key : attachmentList.keySet()) {
			FlowsAttachment value = attachmentList.get(key);
			LOGGER.info("Key = " + key + ", Value = " + value);
		}	
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
			startAcquistiSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;
		case "pre-determina-start": {
			pubblicaTuttiFilePubblicabili(execution);
		};break;
		case "pre-determina-end": {
			pubblicaTuttiFilePubblicabili(execution);
		};break;     
		case "end-annullato-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "ANNULLATO");
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "ANNULLATO");
		};break;     
		// START DECISIONE-CONTRATTARE
		case "DECISIONE-CONTRATTARE-start": {
			String rup = execution.getVariable("rup", String.class);
			runtimeService.addUserIdentityLink(execution.getProcessInstanceId(), rup, PROCESS_VISUALIZER);
			CalcolaTotaleImpegni(execution);
			prepareFilesToSigla(execution);
		};break;  
		case "modifica-decisione-end": {
			CalcolaTotaleImpegni(execution);
		};break;		
		case "verifica-decisione-start": {
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
		};break;  
		case "firma-decisione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "decisioneContrattare");
			}
		};break; 
		case "protocollo-decisione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "decisioneContrattare");
			}
		};break;  
		case "endevent-decisione-contrattare-annulla-start": {
		};break;     
		case "endevent-decisione-contrattare-annulla-end": {
			execution.setVariable("direzioneFlusso", "Annulla");
		};break;   
		case "endevent-decisione-contrattare-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break;
		// END DECISIONE-CONTRATTARE 

		case "espletamento-procedura-end": {	
			if (execution.getVariable("strumentoAcquisizioneId") != null && (execution.getVariable("strumentoAcquisizioneId").equals("21") || execution.getVariable("strumentoAcquisizioneId").equals("23"))) {
				acquistiService.OrdinaElencoDitteCandidate(execution);
			}
			if (execution.getVariable("tipologiaAffidamentoDiretto") != null && (execution.getVariable("tipologiaAffidamentoDiretto").toString().equals("semplificata"))) {
				execution.setVariable("statoImpegni", "definitivi"); 	
			} else {
				execution.setVariable("tipologiaAffidamentoDiretto", "normale"); 
			}
			if(sceltaUtente != null && !sceltaUtente.equals("Revoca") && !execution.getVariable("tempiCompletamentoProceduraFine").equals("null")) {
				DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				Date dateStart = format.parse(execution.getVariable("tempiCompletamentoProceduraInizio").toString());
				Date dateEnd = format.parse(execution.getVariable("tempiCompletamentoProceduraFine").toString());
				if(dateStart.after(dateEnd)) {
					throw new BpmnError("500", "<b>Data Completamento Procedura Inizio posteriore alla data di Fine<br></b>");
				}
			}

			pubblicaTuttiFilePubblicabili(execution);
		};break;
		// START PROVVEDIMENTO-AGGIUDICAZIONE  
		case "predisposizione-provvedimento-aggiudicazione-start": {
			if (execution.getVariable("strumentoAcquisizioneId") != null && (execution.getVariable("strumentoAcquisizioneId").equals("21") || execution.getVariable("strumentoAcquisizioneId").equals("23"))) {

				if (execution.getVariable("nrElencoDitteInit") != null) {
					//				acquistiService.SostituisciDocumento(execution, "provvedimentoAggiudicazione");
					acquistiService.ScorriElencoDitteCandidate(execution);	
				}
				dittaCandidata.evidenzia(execution);
			}
		};break;
		case "predisposizione-provvedimento-aggiudicazione-end": {
			execution.setVariable("statoImpegni", "definitivi"); 
			CalcolaTotaleImpegni(execution);
		};break; 
		case "firma-provvedimento-aggiudicazione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "provvedimentoAggiudicazione");
			}
		};break;  
		case "protocollo-provvedimento-aggiudicazione-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "provvedimentoAggiudicazione");
			}
		};break;
		case "endevent-provvedimento-aggiudicazione-revoca-end": {
			execution.setVariable("direzioneFlusso", "RevocaConProvvedimento");
		};break;    
		case "endevent-provvedimento-aggiudicazione-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break;     
		case "endevent-provvedimento-aggiudicazione-altro-candidato-end": {
			execution.setVariable("direzioneFlusso", "SelezionaAltroCandidato");
		};break;
		case "modifica-provvedimento-aggiudicazione-end": {
			CalcolaTotaleImpegni(execution);
		};break;  
		// END PROVVEDIMENTO-AGGIUDICAZIONE

		// START CONTRATTO FUORI MEPA  
		case "predisposizione-contratto-start": {
			if ((execution.getVariable("gestioneRTIDittaAggiudicataria") != null) && (execution.getVariable("gestioneRTIDittaAggiudicataria").toString().equals("SI"))) {
				//dittaCandidata.aggiornaDittaRTIInvitata(execution);
			}
		};break;
		case "firma-contratto-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				firmaDocumentoService.eseguiFirma(execution, "contratto");
				execution.setVariable("usernameFirmatarioContratto", SecurityUtils.getCurrentUserLogin());
				Date dataStipulaContratto = new Date();
				execution.setVariable("dataStipulaContratto", dataStipulaContratto);	
			}
		};break; 
		case "protocollo-contratto-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "contratto");
			}
		};break;  
		case "endevent-contratto-fuori-mepa-revoca-end": {
			execution.setVariable("direzioneFlusso", "RevocaConProvvedimento");
		};break; 
		case "endevent-contratto-fuori-mepa-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break;     
		case "endevent-contratto-fuori-mepa-altro-candidato-end": {
			execution.setVariable("direzioneFlusso", "SelezionaAltroCandidato");
		};break;
		// END CONTRATTO FUORI MEPA

		// START CONSUNTIVO  
		case "consuntivo-start": {
			String nomeFile="avvisoPostInformazione";
			String labelFile="Avviso di Post-Informazione";
			acquistiService.ProponiDittaAggiudicataria(execution);
			flowsPdfService.makePdf(nomeFile, processInstanceId);
			FlowsAttachment documentoGenerato = runtimeService.getVariable(processInstanceId, nomeFile, FlowsAttachment.class);
			documentoGenerato.setLabel(labelFile);
			documentoGenerato.setPubblicazioneTrasparenza(true);
			flowsAttachmentService.saveAttachmentFuoriTask(processInstanceId, nomeFile, documentoGenerato, null);

		};break;
		case "consuntivo-end": {
			if(sceltaUtente != null && !sceltaUtente.equals("RevocaConProvvedimento")) {
				pubblicaTuttiFilePubblicabili(execution);
				attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
				//attachmentService.setPubblicabileTrasparenza(execution.getId(), "avvisoPostInformazione", true);
				//attachmentService.setPubblicabileTrasparenza(execution.getId(), "modificheVariantiArt106", true);
				if ( execution.getVariable("importoTotaleNetto") != null && Double.compare(Double.parseDouble(execution.getVariable("importoTotaleNetto").toString()), 1000000) > 0) {
					attachmentService.setPubblicabileTrasparenza(execution, "stipula", true);
				}
				if(execution.getVariable("numeroProtocollo_stipula") != null) {
					protocolloDocumentoService.protocollaDocumento(execution, "stipula", execution.getVariable("numeroProtocollo_stipula").toString(), execution.getVariable("dataProtocollo_stipula").toString());
				}
				if(execution.getVariable("numeroProtocollo_contratto") != null) {
					protocolloDocumentoService.protocollaDocumento(execution, "contratto", execution.getVariable("numeroProtocollo_contratto").toString(), execution.getVariable("dataProtocollo_contratto").toString());
				}
			}
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				attachmentService.setPubblicabileTrasparenza(execution, "ProvvedimentoDiRevoca", true);
			}

		};break; 
		case "end-stipulato-start": {
			pubblicaTuttiFilePubblicabili(execution);
			controllaFilePubblicabiliTrasparenza(execution);
			execution.setVariable(STATO_FINALE_DOMANDA, "STIPULATO");
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "STIPULATO");
			//TODO implementare le url a seconda del contesto
			String urlSigla = "www.google.it";
			Map<String, Object> siglaPayload = createSiglaPayload(execution);
			externalMessageService.createExternalMessage(urlSigla, ExternalMessageVerb.POST, siglaPayload);
			prepareFilesToSigla(execution);
		};break;     
		case "end-stipulato-end": {
		};break;
		// END CONSUNTIVO  

		// START STIPULA MEPA  
		//		case "stipula-mepa-consip-start": {
		//			if (execution.getVariable("strumentoAcquisizioneId").toString().equals("21")) {
		//				dittaCandidata.evidenzia(execution);
		//			} else {
		//				if ((execution.getVariable("gestioneRTIDittaAggiudicataria") != null) && (execution.getVariable("gestioneRTIDittaAggiudicataria").toString().equals("SI"))) {
		//					dittaCandidata.aggiornaDittaRTIInvitata(execution);
		//				}
		//			}			
		//		};break; 
		case "endevent-stipula-mepa-consip-revoca-end": {
			execution.setVariable("direzioneFlusso", "RevocaConProvvedimento");
		};break;
		case "endevent-stipula-mepa-consip-protocollo-end": {
			execution.setVariable("direzioneFlusso", "Stipula");
		};break; 
		// END STIPULA MEPA  

		// START REVOCA

		case "firma-revoca-end": {
			firmaDocumentoService.eseguiFirma(execution, "ProvvedimentoDiRevoca");
		};break; 
		case "protocollo-revoca-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
				protocolloDocumentoService.protocolla(execution, "ProvvedimentoDiRevoca");
			}
		};break;  
		// END REVOCA  

		// FINE ACQUISTI  

		case "end-revocato-start": {
			execution.setVariable(STATO_FINALE_DOMANDA, "REVOCATO");
			flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "REVOCATO");
		};break;

		// FINE FLUSSO  
		case "process-end": {
			//			if(execution.getVariable(STATO_FINALE_DOMANDA).toString().equals("STIPULATO")){
			//				pubblicaTuttiFilePubblicabili(execution);
			//			}
		};break;  

		//SUBFLUSSI
		case "DECISIONE-CONTRATTARE-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaSemplice")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					attachmentService.setPubblicabileTrasparenza(execution, value.getName(), false);					
				}
			} else {					
				//attachmentService.setPubblicabileTrasparenza(execution.getId(), "decisioneContrattare", true);
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;

		case "PROVVEDIMENTO-AGGIUDICAZIONE-start": {
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "giustificazioniAnomalie", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "provvedimentoNominaCommissione", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "provvedimentoAmmessiEsclusi", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "esitoValutazioneAnomalie", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "elencoDitteInvitate", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "elencoVerbali", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "bandoAvvisi", true);
			//			attachmentService.setPubblicabileTrasparenza(execution.getId(), "letteraInvito", true);
			pubblicaTuttiFilePubblicabili(execution);
		};break;	

		case "PROVVEDIMENTO-AGGIUDICAZIONE-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}
			} else {					
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;	


		case "CONTRATTO-FUORI-MEPA-start": {
			//			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			//			pubblicaFileMultipliPubblicabili(execution, "bandoAvvisi", true);
			//			pubblicaFileMultipliPubblicabili(execution, "letteraInvito", true);
			pubblicaTuttiFilePubblicabili(execution);

		};break;	


		case "CONTRATTO-FUORI-MEPA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}
			} else {	
				if ( execution.getVariable("importoTotaleNetto") != null && Double.parseDouble(execution.getVariable("importoTotaleNetto").toString()) > 1000000) {
					attachmentService.setPubblicabileTrasparenza(execution, "contratto", true);
				}
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;



		case "STIPULA-MEPA-start": {
			//			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);		
			//			pubblicaFileMultipliPubblicabili(execution, "bandoAvvisi", true);
			//			pubblicaFileMultipliPubblicabili(execution, "letteraInvito", true);
			pubblicaTuttiFilePubblicabili(execution);

		};break;	

		case "STIPULA-MEPA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			if(sceltaUtente != null && sceltaUtente.equals("RevocaConProvvedimento")) {
				for (String key : attachmentList.keySet()) {
					FlowsAttachment value = attachmentList.get(key);
					LOGGER.info("Key = " + key + ", Value = " + value);
					//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
				}
			} else {					
				pubblicaTuttiFilePubblicabili(execution);
			}
		};break;		
		case "REVOCA-end": {
			attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
			for (String key : attachmentList.keySet()) {
				FlowsAttachment value = attachmentList.get(key);
				LOGGER.info("Key = " + key + ", Value = " + value);
				//attachmentService.setPubblicabile(execution.getId(), value.getName(), false);					
			}
			attachmentService.setPubblicabileTrasparenza(execution, "ProvvedimentoDiRevoca", true);
			pubblicaTuttiFilePubblicabili(execution);
		};break;	
		case "end-revocato": {
		};break;

		// DEFAULT  
		default:  {
		};break;    

		} 
	}
}
