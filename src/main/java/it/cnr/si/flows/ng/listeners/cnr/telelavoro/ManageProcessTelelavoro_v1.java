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

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;

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


	@Value("${cnr.siper-telelavoro.url}")
	private String urlTelelavoro;
	@Value("${cnr.siper-telelavoro.domandePath}")
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

	public void restToApplicazioneTelelavoro(DelegateExecution execution, StatoTelelavoroEnum statoTelelavoro) {


		String idStruttura = execution.getVariable("idStruttura").toString();
		String codiceSedeTelelavoro = execution.getVariable("codiceSedeTelelavoro").toString();
		String idDomanda = execution.getVariable("idDomanda").toString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dataFirma = new Date();
		String dataAzioneFlusso = dateFormat.format(dataFirma);
		//LocalDate dataFirmaFlusso = LocalDate.now();
		
		Map<String, Object> telelavoroPayload = new HashMap<String, Object>()
		{
			{
				put("codiceSedeTelelavoro", codiceSedeTelelavoro);
				put("idDomanda", idDomanda);
				put("stato", statoTelelavoro.name().toString());
				put("dataAzioneFlusso", dataAzioneFlusso);
				put("matricola", execution.getVariable("matricola").toString());
				put("processInstanceId", execution.getProcessInstanceId().toString());
				if(execution.getVariable("commento") != null) {
					put("commento", execution.getVariable("commento").toString());
				} else {
					put("commento", "");
				}
			}	
		};

		String url = urlTelelavoro + pathTelelavoro;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, telelavoroPayload, ExternalApplication.SIPER);
	}


	@Override
	public void notify(DelegateExecution execution) throws Exception {
		String currentUser = securityService.getCurrentUserLogin();
		String processInstanceId =  execution.getProcessInstanceId();
		String executionId =  execution.getId();
		String stato =  execution.getCurrentActivityName();
		String matricola = execution.getVariable("matricola").toString();
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
		case "validazione-start": {
			startTelelavoroSetGroupsAndVisibility.configuraVariabiliStart(execution);			
		};break;
		
		// MODIFICA
		case "modifica-start": {
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.MODIFICA);
		};break;
	
	
		case "endevent-annullata-start": {
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.ANNULLATA);
			execution.setVariable("statoFinale", Enum.StatoTelelavoroEnum.ANNULLATA.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
		};break;
		
		case "endevent-respinta-start": {
			if(execution.getVariable("sceltaUtente") != "Rifiuta") {
				execution.setVariable("notaDomandaRespinta", "Scadenza termini temporali Valutazione Dirigente");
			}
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.RIFIUTATA);
			execution.setVariable("statoFinale", Enum.StatoTelelavoroEnum.RIFIUTATA.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
		};break;
		
		case "endevent-scaduta-start": {
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.SCADUTA);
			execution.setVariable("statoFinale", Enum.StatoTelelavoroEnum.SCADUTA.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
		};break;
				
		
		case "endevent-telelavoro-start": {
			String statoFinaleSiper = execution.getVariable("sceltaUtente").toString();
			execution.setVariable("STATO_FINALE_DOMANDA", statoFinaleSiper);
			execution.setVariable("statoFinale",statoFinaleSiper);
			utils.updateJsonSearchTerms(executionId, processInstanceId, statoFinaleSiper);
		};break;  
		case "notificaMail-start": {
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.MODIFICA);
		};break;  
		case "finalizzazione-start": {
			restToApplicazioneTelelavoro(execution, Enum.StatoTelelavoroEnum.FINALIZZAZIONE);
		};break;

		case "process-end": {
		};break; 
		// DEFAULT  
		default: {
		};break;

		} 
	}

}
