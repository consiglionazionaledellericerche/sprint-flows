package it.cnr.si.flows.ng.listeners.cnr.attestati;



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
import it.cnr.si.flows.ng.utils.Enum.StatoAttestatiEnum;
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
public class ManageProcessAttestati_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAttestati_v1.class);


	@Value("${cnr.attestati.url}")
	private String urlAttestati;
	@Value("${cnr.attestati.domandePath}")
	private String pathAttestati;


	@Inject
	private StartAttestatiSetGroupsAndVisibility startAttestatiSetGroupsAndVisibility;
	@Inject
	private ExternalMessageService externalMessageService;	
	@Inject
	private AceService aceService;
	@Inject
	private Utils utils;
    @Inject
    private SecurityService securityService;

	private Expression faseEsecuzione;

	public void restToApplicazioneAttestati(DelegateExecution execution, StatoAttestatiEnum statoAttestato, String user) {


		String idStruttura = execution.getVariable("idStruttura").toString();
		String codiceSedeAttestato = execution.getVariable("codiceSedeAttestato").toString();
		String meseAttestato = execution.getVariable("meseAttestato").toString();
		String annoAttestato = execution.getVariable("annoAttestato").toString();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dataFirma = new Date();
		String dataFirmaFlusso = dateFormat.format(dataFirma);
		//LocalDate dataFirmaFlusso = LocalDate.now();
		
		Map<String, Object> missioniPayload = new HashMap<String, Object>()
		{
			{
				put("codiceSedeAttestato", codiceSedeAttestato);
				put("meseAttestato", meseAttestato);
				put("annoAttestato", annoAttestato);
				put("stato", statoAttestato.name().toString());
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

		String url = urlAttestati + pathAttestati;
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
			startAttestatiSetGroupsAndVisibility.configuraVariabiliStart(execution);
		};break;    

		// START
		case "respinto-start": {
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoAttestatiEnum.RESPINTO.toString());
			restToApplicazioneAttestati(execution, Enum.StatoAttestatiEnum.RESPINTO, currentUser);		
		};break;
		
		//case "respinto-end": {
		//case "validazione-start": {
		//case "validazione-end": {
		

		case "endevent-annulla": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoAttestatiEnum.ANNULLATO);
			execution.setVariable("statoFinale", Enum.StatoAttestatiEnum.ANNULLATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoAttestatiEnum.ANNULLATO.toString());
			restToApplicazioneAttestati(execution, Enum.StatoAttestatiEnum.ANNULLATO, currentUser);
		};break;    	


		case "endevent-approvato-start": {
			execution.setVariable("STATO_FINALE_DOMANDA", Enum.StatoAttestatiEnum.APPROVATO);
			execution.setVariable("statoFinale", Enum.StatoAttestatiEnum.APPROVATO.toString());
			utils.updateJsonSearchTerms(executionId, processInstanceId, Enum.StatoAttestatiEnum.APPROVATO.toString());
			restToApplicazioneAttestati(execution, Enum.StatoAttestatiEnum.APPROVATO, currentUser);
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
