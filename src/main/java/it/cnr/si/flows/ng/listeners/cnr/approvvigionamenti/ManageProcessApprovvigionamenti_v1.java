package it.cnr.si.flows.ng.listeners.cnr.approvvigionamenti;



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
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeMissioniEnum;
import it.cnr.si.flows.ng.utils.Enum.TipologieeMissioniEnum;
import it.cnr.si.flows.ng.utils.SecurityUtils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.service.SecurityService;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
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
public class ManageProcessApprovvigionamenti_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessApprovvigionamenti_v1.class);


	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartApprovvigionamentiSetGroupsAndVisibility startApprovvigionamentiSetGroupsAndVisibility;
	@Inject
	private ExternalMessageService externalMessageService;	
	@Inject
	private AceService aceService;
	@Inject
	private Utils utils;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;	
	@Inject
	private SecurityService securityService;

	private Expression faseEsecuzione;



	@Override
	public void notify(DelegateExecution execution) throws Exception {
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
			//code
		};break; 
		case "verifica-start": {
			startApprovvigionamentiSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    

		case "verifica-end": {
			if(execution.getVariable("sceltaUtente") != null && execution.getVariable("sceltaUtente").equals("ModificaTipologia-Valida")) {
				String gruppoLavorazione = "responsabileApprovvigionamenti@0000";
				if (execution.getVariable("tipologiaRichiesta").toString().startsWith("telefoniaFissa")){
					gruppoLavorazione = "gruppoLavorazioneTelefoniaFissa@0000";
				} 			
				if (execution.getVariable("tipologiaRichiesta").toString().startsWith("telefoniaMobile")){
					gruppoLavorazione = "gruppoLavorazioneTelefoniaMobile@0000";
				} 			
				if (execution.getVariable("tipologiaRichiesta").toString().startsWith("cablaggio")){
					gruppoLavorazione = "gruppoLavorazioneCablaggio@0000";
				} 			
				if (execution.getVariable("tipologiaRichiesta").toString().startsWith("desktop")){
					gruppoLavorazione = "gruppoLavorazioneDesktop@0000";
				}
				execution.setVariable("gruppoLavorazione", gruppoLavorazione);
			} 
		};break; 

		case "lavorazione-start": {
			//code
		};break; 

		case "lavorazione-end": {
			//code
		};break;



		case "endevent-annullata-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoApprovvigionamentiEnum.ANNULLATA);
			execution.setVariable("statoFinale", Enum.StatoApprovvigionamentiEnum.ANNULLATA.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoApprovvigionamentiEnum.ANNULLATA.toString());
		};break;    	


		case "endevent-evasa-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoApprovvigionamentiEnum.EVASA);
			execution.setVariable("statoFinale", Enum.StatoApprovvigionamentiEnum.EVASA.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoApprovvigionamentiEnum.EVASA.toString());
		};break;  

		case "endevent-inevasa-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoApprovvigionamentiEnum.INEVASA);
			execution.setVariable("statoFinale", Enum.StatoApprovvigionamentiEnum.INEVASA.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoApprovvigionamentiEnum.INEVASA.toString());
		};break;  

		case "process-end": {
			//code
		};break; 
		// DEFAULT  
		default: {
		};break;

		} 
	}


}
