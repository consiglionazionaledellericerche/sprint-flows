package it.cnr.si.flows.ng.listeners.cnr.firmaPresidente;



import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeMissioniEnum;
import it.cnr.si.flows.ng.utils.Enum.TipologieeMissioniEnum;

import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

@Component
@Profile("cnr")
public class ManageProcessFirmaPresidente_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessFirmaPresidente_v1.class);


	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartFirmaPresidenteSetGroupsAndVisibility startFirmaPresidenteSetGroupsAndVisibility;
	@Inject
	private ExternalMessageService externalMessageService;	
	@Inject
	private AceService aceService;
	@Inject
	private Utils utils;


	private Expression faseEsecuzione;



	@Override
	public void notify(DelegateExecution execution) throws Exception {
		//(OivPdfService oivPdfService = new OivPdfService();
		String currentUser = securityService.getCurrentUserLogin();
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
			startFirmaPresidenteSetGroupsAndVisibility.configuraVariabiliStart(execution);
			execution.setVariable("tutteDomandeAccettateFlag", "false");
		};break;    

		// START
		case "respinto-uo-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoDomandeMissioniEnum.RESPINTO_UO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeMissioniEnum.RESPINTO_UO.toString());
		};break;

		case "respinto-spesa-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoDomandeMissioniEnum.RESPINTO_UO_SPESA);
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeMissioniEnum.RESPINTO_UO_SPESA.toString());
		};break;


		case "firma-uo-start": {
			utils.updateJsonSearchTerms(executionId, processInstanceId, "FIRMA UO");
		};break; 

		case "firma-uo-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				List<String> nomiVariabiliFile = new ArrayList<String>();
				nomiVariabiliFile.add("missioni");
				if (execution.getVariable("tipologiaMissione").toString().equals("ordine")){
					if (execution.getVariable("missioneConAnticipoFlag") != null && execution.getVariable("missioneConAnticipoFlag").toString().equals("si")) {
						nomiVariabiliFile.add("anticipoMissione");
						firmaDocumentoService.eseguiFirmaMultipla(execution, nomiVariabiliFile, null);
					} else {
						firmaDocumentoService.eseguiFirma(execution, nomiVariabiliFile, null);
					}
				}
			}
			
			// FIRMA MULTIPLA TUTTI I DOCUMENTI DI UN CERTO TIPO
//			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
//				List<String> nomiVariabiliFile = new ArrayList<String>();
//				nomiVariabiliFile.add("decisioneContrattare");
//				List<FlowsAttachment> attachments = flowsAttachmentService.getAttachmentArray(processInstanceId, "allegati");
//				if (attachments.size() == 0)
//					throw new TaskFailedException("Attachment non opzionali mancanti: " + "allegati");               
//				attachments.forEach(att -> nomiVariabiliFile.add(att.getName()));
//				firmaDocumentoService.eseguiFirmaMultipla(execution, nomiVariabiliFile, null);
//			}
//			

			if(sceltaUtente != null && (sceltaUtente.equals("Firma") || sceltaUtente.equals("Firma Multipla"))) {

				//SE I DUE FIRMATARI SPESA E UO SONO LA STESSA PERSONA
				if (execution.getVariable("validazioneSpesaFlag").toString().equalsIgnoreCase("si")) {
					execution.setVariable("firmaSpesaFlag", "si");
					//				String gruppoFirmatarioUo = execution.getVariable("gruppoFirmatarioUo").toString();
					String gruppoFirmatarioSpesa = execution.getVariable("gruppoFirmatarioSpesa").toString();
					//				String gruppoFirmatarioUoSigla = gruppoFirmatarioUo.split("@")[0];
					//				int gruppoFirmatarioUoIdEO = Integer.parseInt(gruppoFirmatarioUo.split("@")[1].toString());
					String gruppoFirmatarioSpesaSigla = gruppoFirmatarioSpesa.split("@")[0];
					int gruppoFirmatarioSpesaIdEO = Integer.parseInt(gruppoFirmatarioSpesa.split("@")[1].toString());
					//List<SimpleUtenteWebDto> utentiGruppoFirmatarioUo =  aceService.getUtentiInRuoloEo(gruppoFirmatarioUoSigla, gruppoFirmatarioUoIdEO);
					List<SimpleUtenteWebDto> utentiGruppoFirmatarioSpesa =  aceService.getUtentiInRuoloEo(gruppoFirmatarioSpesaSigla, gruppoFirmatarioSpesaIdEO);
					// SE L'UTENTE CORRENTE FA PARTE DEL GRUPPO FIRMATARIO SPESA
					for(int i=0;i<utentiGruppoFirmatarioSpesa.size();i++) { 	
						LOGGER.info("l'utente {} nel gruppo è {} ",  i , utentiGruppoFirmatarioSpesa.get(i).getUsername());
						if(utentiGruppoFirmatarioSpesa.get(i).getUsername().equalsIgnoreCase(currentUser)) {
							execution.setVariable("firmaSpesaFlag", "no");
						}
					} 
				}
				if (execution.getVariable("firmaSpesaFlag").toString().equalsIgnoreCase("si")) {
					execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoDomandeMissioniEnum.FIRMATO_UO);
					utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeMissioniEnum.FIRMATO_UO.toString());
				}
			}

		};break; 

		case "firma-spesa-start": {
			utils.updateJsonSearchTerms(executionId, processInstanceId, "FIRMA SPESA");
		};break; 

		case "firma-spesa-end": {
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				List<String> nomiVariabiliFile = new ArrayList<String>();
				nomiVariabiliFile.add("missioni");
				if (execution.getVariable("tipologiaMissione").toString().equals("ordine")){
					if (execution.getVariable("missioneConAnticipoFlag") != null && execution.getVariable("missioneConAnticipoFlag").toString().equals("si")) {
						nomiVariabiliFile.add("anticipoMissione");
						firmaDocumentoService.eseguiFirmaMultipla(execution, nomiVariabiliFile, null);

					} else {
						firmaDocumentoService.eseguiFirma(execution, nomiVariabiliFile, null);
					}
				}
			}
		};break; 

		case "endevent-annulla": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoDomandeMissioniEnum.ANNULLATO);
			execution.setVariable("statoFinale", Enum.StatoDomandeMissioniEnum.ANNULLATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			//restToApplicazioneMissioni(execution, Enum.StatoDomandeMissioniEnum.ANNULLATO);
		};break;    	


		case "endevent-firmato-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoDomandeMissioniEnum.FIRMATO.toString());
			execution.setVariable("statoFinale", Enum.StatoDomandeMissioniEnum.FIRMATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
		};break;  

		case "process-end": {
			//sbloccaDomandeBando(execution);
		};break; 
		// DEFAULT  
		default: {
		};break;

		} 
	}


}
