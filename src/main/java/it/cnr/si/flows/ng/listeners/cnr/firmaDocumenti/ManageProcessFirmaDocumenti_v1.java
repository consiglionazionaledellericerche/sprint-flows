package it.cnr.si.flows.ng.listeners.cnr.firmaDocumenti;



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
public class ManageProcessFirmaDocumenti_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessFirmaDocumenti_v1.class);


	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartFirmaDocumentiSetGroupsAndVisibility startFirmaDocumentiSetGroupsAndVisibility;
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
			startFirmaDocumentiSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    

		case "firma-start": {
			//utils.updateJsonSearchTerms(executionId, processInstanceId, "FIRMA");
		};break; 

		case "firma-end": {
			// FIRMA MULTIPLA TUTTI I DOCUMENTI DI UN CERTO TIPO
			if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
				List<String> nomiVariabiliFile = new ArrayList<String>();
				List<FlowsAttachment> attachments = flowsAttachmentService.getAttachmentArray(processInstanceId, "documentoDaFirmare");
				if (attachments.size() == 0)
					throw new TaskFailedException("Attachment non opzionali mancanti: " + "allegati");               
				attachments.forEach(att -> nomiVariabiliFile.add(att.getName()));
				firmaDocumentoService.eseguiFirmaMultipla(execution, nomiVariabiliFile, null);
			}
		};break; 

		case "modifica-end": {
			//Check se il gruppo VALIDATORI ha membri
			if(sceltaUtente != null && !sceltaUtente.equals("Annulla")) {

				List<SimpleUtenteWebDto> members = aceService.getUtentiInRuoloEo("validatoreFirmaDocumenti", Integer.parseInt(execution.getVariable("idStruttura").toString()));
				if (members.isEmpty()) {
					execution.setVariable("organizzazioneStruttura", "Semplice");
				} else {
					execution.setVariable("organizzazioneStruttura", "Complessa");
				}
			}
		};break;



		case "endevent-annullato-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoFirmaDocumentiEnum.ANNULLATO);
			execution.setVariable("statoFinale", Enum.StatoFirmaDocumentiEnum.ANNULLATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoFirmaDocumentiEnum.ANNULLATO.toString());
		};break;    	


		case "endevent-firmato-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoFirmaDocumentiEnum.FIRMATO);
			execution.setVariable("statoFinale", Enum.StatoFirmaDocumentiEnum.FIRMATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoFirmaDocumentiEnum.FIRMATO.toString());
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
