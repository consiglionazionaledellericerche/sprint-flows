package it.cnr.si.flows.ng.listeners.cnr.valida;



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
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeSmartWorkingEnum;
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
public class ManageProcessValida_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessValida_v1.class);

	@Value("${cnr.siper.url}")
	private String urlSiper;
	@Value("${cnr.siper.domandePath}")
	private String pathValida;
	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartValidaSetGroupsAndVisibility startValidaSetGroupsAndVisibility;
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


	public void restToApplicazioneSiper(DelegateExecution execution, Enum.StatoDomandeValidaEnum statoDomanda) {

		String idDomanda = execution.getVariable("idDomanda").toString();
		String commento = "";
		String matricolaValidatore = "";
		if (execution.getVariable("commento") != null) {
			commento = execution.getVariable("commento").toString();
		}

		Map<String, Object> siperPayload = new HashMap<String, Object>();
		siperPayload.put("idDomanda", idDomanda);
		siperPayload.put("stato", statoDomanda.name().toString());
		siperPayload.put("commento", commento);

		String url = urlSiper + pathValida;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, siperPayload, ExternalApplication.SIPER);
	}

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
				startValidaSetGroupsAndVisibility.configuraVariabiliStart(execution);
			};break;    

			case "valida-start": {
				//utils.updateJsonSearchTerms(executionId, processInstanceId, "FIRMA");
			};break; 

			case "valida-end": {
				// FIRMA MULTIPLA TUTTI I DOCUMENTI DI UN CERTO TIPO
			};break; 

			case "endevent-respinta-start": {
				execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoDomandeValidaEnum.RESPINTA);
				execution.setVariable("statoFinale", Enum.StatoDomandeValidaEnum.RESPINTA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeValidaEnum.RESPINTA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeValidaEnum.RESPINTA);
			};break;    	

			case "endevent-validata-start": {
				execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoDomandeValidaEnum.VALIDATA);
				execution.setVariable("statoFinale", Enum.StatoDomandeValidaEnum.VALIDATA.toString());
				utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoDomandeValidaEnum.VALIDATA.toString());
				restToApplicazioneSiper(execution, Enum.StatoDomandeValidaEnum.VALIDATA);
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
