package it.cnr.si.flows.ng.listeners.cnr.telelavoro;



import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import it.cnr.si.flows.ng.service.FirmaDocumentoService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoTelelavoroEnum;
import it.cnr.si.flows.ng.utils.Enum.TipologieeMissioniEnum;

import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.service.SecurityService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

@Component
@Profile("cnr")
public class ManageProcessTelelavoro_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessTelelavoro_v1.class);


	@Value("${cnr.telelavoro.url}")
	private String urlTelelavoro;
	@Value("${cnr.telelavoro.domandePath}")
	private String pathTelelavoro;


	@Inject
	private StartTelelavoroSetGroupsAndVisibility startTelelavoroSetGroupsAndVisibility;
	@Inject
	private ExternalMessageService externalMessageService;	
	@Inject
	private AceService aceService;
	@Inject
	private Utils utils;
    @Inject
    private SecurityService securityService;

	private Expression faseEsecuzione;

	public void restToApplicazioneTelelavoro(DelegateExecution execution, StatoTelelavoroEnum statoTelelavoro, String user) {


		String idStruttura = execution.getVariable("idStruttura").toString();
		String codiceSedeTelelavoro = execution.getVariable("codiceSedeTelelavoro").toString();
		String meseTelelavoro = execution.getVariable("meseTelelavoro").toString();
		String annoTelelavoro = execution.getVariable("annoTelelavoro").toString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dataFirma = new Date();
		String dataFirmaFlusso = dateFormat.format(dataFirma);
		//LocalDate dataFirmaFlusso = LocalDate.now();
		
		Map<String, Object> missioniPayload = new HashMap<String, Object>()
		{
			{
				put("codiceSedeTelelavoro", codiceSedeTelelavoro);
				put("meseTelelavoro", meseTelelavoro);
				put("annoTelelavoro", annoTelelavoro);
				put("stato", statoTelelavoro.name().toString());
				put("dataFirmaFlusso", dataFirmaFlusso);
				put("processInstanceId", execution.getProcessInstanceId().toString());
				put("user", user);
				if(execution.getVariable("commento") != null) {
					put("commento", execution.getVariable("commento").toString());
				} else {
					put("commento", "");
				}
			}	
		};

		String url = urlTelelavoro + pathTelelavoro;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, missioniPayload, ExternalApplication.ATTESTATI);
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
			startTelelavoroSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    

		// START
		case "respinto-start": {
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoTelelavoroEnum.RESPINTO.toString());
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.RESPINTO, currentUser);		
		};break;
		
		//case "respinto-end": {
		//case "validazione-start": {
		//case "validazione-end": {
		

		case "endevent-annulla": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoTelelavoroEnum.ANNULLATO);
			execution.setVariable("statoFinale", Enum.StatoTelelavoroEnum.ANNULLATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoTelelavoroEnum.ANNULLATO.toString());
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.ANNULLATO, currentUser);
		};break;    	


		case "endevent-approvato-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoTelelavoroEnum.APPROVATO);
			execution.setVariable("statoFinale", Enum.StatoTelelavoroEnum.APPROVATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoTelelavoroEnum.APPROVATO.toString());
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.APPROVATO, currentUser);
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
