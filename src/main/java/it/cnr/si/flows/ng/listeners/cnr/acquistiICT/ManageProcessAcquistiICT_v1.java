package it.cnr.si.flows.ng.listeners.cnr.acquistiICT;


import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageVerb;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Enum.StatoDomandeAccordiInternazionaliEnum;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.AceService;
import it.cnr.si.service.ExternalMessageService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;

import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.runtime.Job;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.Optional;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;
import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")
public class ManageProcessAcquistiICT_v1 implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessAcquistiICT_v1.class);


	@Value("${cnr.abil.url}")
	private String urlAcquistiICT;
	@Value("${cnr.abil.domandePath}")
	private String pathDomandeAcquistiICT;

	@Inject
	private FirmaDocumentoService firmaDocumentoService;
	@Inject
	private ProtocolloDocumentoService protocolloDocumentoService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private StartAcquistiICTSetGroupsAndVisibility StartAcquistiICTSetGroupsAndVisibility;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsPdfService flowsPdfService;
	@Inject
	private FlowsAttachmentService flowsAttachmentService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private ExternalMessageService externalMessageService;
	@Inject
	private TaskService taskService;
	@Inject
	private ManagementService managementService;
	@Inject
	private Utils utils;
	@Inject
	private FlowsPdfBySiglaRestService flowsPdfBySiglaRestService;

	private Expression faseEsecuzione;
	
	@Inject
	private AceService aceService;


	public void restToApplicazioneAcquistiICT(DelegateExecution execution, StatoDomandeAccordiInternazionaliEnum statoDomanda) {

		// @Value("${cnr.accordi-bilaterali.url}")
		// private String urlAcquistiICT;
		// @Value("${cnr.accordi-bilaterali.usr}")
		// private String usrAcquistiICT;
		// @Value("${cnr.accordi-bilaterali.psw}")
		// private String pswAcquistiICT;
		Double idDomanda = Double.parseDouble(execution.getVariable("idDomanda").toString());
		Map<String, Object> abilPayload = new HashMap<String, Object>()
		{
			{
				put("idDomanda", idDomanda);
				put("stato", statoDomanda.name().toString());
			}
		};

		String url = urlAcquistiICT + pathDomandeAcquistiICT;
		externalMessageService.createExternalMessage(url, ExternalMessageVerb.POST, abilPayload, ExternalApplication.ABIL);
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

			impegno.put("uo_label", aceBridgeService.getUoLike(impegno.getString("uo")).get(0).getDenominazione());
			impegno.put("cdsuo", impegno.get("uo").toString());
		}

		execution.setVariable("impegni_json", impegni.toString());
		execution.setVariable("importoTotaleNetto", importoTotaleNetto);
		execution.setVariable("importoTotaleLordo", importoTotaleLordo);
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
		//CHECK PER ANNULLO FLUSSO
		if (execution.getVariableInstance("motivazioneEliminazione") == null) {
			switch(faseEsecuzioneValue){
			// START
			case "process-start": {
				//String propostaHtml = String.valueOf(execution.getVariable("propostaDiRicerca"));
				//String propostaPulita = Utils.sanitizeHtml(propostaHtml);
				//execution.setVariable("propostaDiRicerca", propostaPulita);

				StartAcquistiICTSetGroupsAndVisibility.configuraVariabiliStart(execution);

			};break;
			// START


			case "predisposizione-determina-start": {

			};break;
			case "predisposizione-determina-end": {
				//String gruppoRUP = "gruppoRUP@" + IdEntitaOrganizzativaDirettore;
				if(execution.getVariable("rup") != null){
					SimplePersonaWebDto rupUser = aceService.getPersonaByUsername(execution.getVariable("rup").toString());
				}
				LOGGER.debug("Il rup {}",   execution.getVariable("rup").toString());
				runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), execution.getVariable("rup").toString(), PROCESS_VISUALIZER);
				CalcolaTotaleImpegni(execution);
			};break;


			case "firma-determina-start": {

			};break;
			case "firma-determina-end": {

				// FIRMA MULTIPLA TUTTI I DOCUMENTI DI UN CERTO TIPO
				if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
					firmaDocumentoService.eseguiFirma(execution, "determina", null);
				}

			};break;


			case "protocollo-determina-start": {


			};break;
			case "protocollo-determina-end": {
				protocolloDocumentoService.protocolla(execution, "determina");

			};break;



			case "espletamento-procedura-start": {

			};break;
			case "espletamento-procedura-end": {

			};break;

			case "predisposizione-ordine-start": {

			};break;
			case "predisposizione-ordine-end": {

			};break;

			case "firma-ordine-start": {


			};break;
			case "firma-ordine-end": {

				// FIRMA MULTIPLA TUTTI I DOCUMENTI DI UN CERTO TIPO
				if(sceltaUtente != null && sceltaUtente.equals("Firma")) {
					firmaDocumentoService.eseguiFirma(execution, "contratto", null);
				}
			};break;


			case "protocollo-ordine-start": {

			};break;
			case "protocollo-ordine-end": {
				protocolloDocumentoService.protocolla(execution, "contratto");

			};break;


			case "consuntivo-start": {

			};break;
			case "consuntivo-end": {

			};break;


			case "carica-ordine-mepa-start": {

			};break;
			case "carica-ordine-mepa-end": {

			};break;


			case "protocollo-ordine-mepa-start": {

			};break;
			case "protocollo-ordine-mepa-end": {
				protocolloDocumentoService.protocolla(execution, "ordine-mepa");

			};break;


			case "invio-ordine-mepa-start": {

			};break;
			case "invio-ordine-mepa-end": {

			};break;


			case "endevent-annullato-start": {
				execution.setVariable(statoFinaleDomanda.name(), Enum.StatoAcquistiICTEnum.ANNULLATO);
				execution.setVariable("statoFinale", Enum.StatoAcquistiICTEnum.ANNULLATO);
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			};break;
			case "endevent-acquistato-start": {
				execution.setVariable(statoFinaleDomanda.name(), Enum.StatoAcquistiICTEnum.ACQUISTATO);
				execution.setVariable("statoFinale", Enum.StatoAcquistiICTEnum.ACQUISTATO);
				utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());
			};break;


			// DEFAULT
			default:  {
			};break;

			}
		} else {
			execution.setVariable(statoFinaleDomanda.name(), Enum.StatoAcquistiICTEnum.ELIMINATO);
			execution.setVariable("statoFinale", Enum.StatoAcquistiICTEnum.ELIMINATO);
			utils.updateJsonSearchTerms(executionId, processInstanceId, execution.getVariable("statoFinale").toString());

		}
	}
}
