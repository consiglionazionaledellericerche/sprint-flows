package it.cnr.si.flows.ng.listeners.oiv.service;


import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;

import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;
import static it.cnr.si.flows.ng.utils.Utils.parseInt;

@Service
public class OivSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(OivSetGroupsAndVisibility.class);


	@Inject
	private RuntimeService runtimeService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private OperazioniTimer operazioniTimer;


	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable("initiator");
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

		String struttura = "99999";
		String gruppoIstruttori = "istruttore@"+ struttura;
		String gruppoFirmaRigetto = "firmaRigetto@"+ struttura;
		String gruppoDirettore = "direttore@"+ struttura;
		String gruppoCoordinatoreResponsabile = "coordinatoreresponsabile@"+ struttura;

		execution.setVariable("gruppoIstruttori", gruppoIstruttori);
		execution.setVariable("gruppoFirmaRigetto", gruppoFirmaRigetto);
		execution.setVariable("gruppoCoordinatoreResponsabile", gruppoCoordinatoreResponsabile);
		execution.setVariable("domandaImprocedibileFlag", "0");
		execution.setVariable("soccorsoIstruttoriaFlag", "0");
		execution.setVariable("giorniDurataPreavvisoRigetto", "0");
		execution.setVariable("giorniDurataSoccorsoIstruttorio", "0");
		execution.setVariable("giorniEffettiviDurataProcedimentoOiv", "0");
		
		LOGGER.debug("Imposto i gruppi del flusso gruppoIstruttori: {}, gruppoFirmaRigetto: {}, gruppoCoordinatoreResponsabile: {}", gruppoIstruttori, gruppoDirettore, gruppoCoordinatoreResponsabile);

		//conversione semplice di date
		//if (execution.getVariable("valutazioneEsperienze_json") !=  null) {
		//String valutazioneEsperienze_json = execution.getVariable("valutazioneEsperienze_json").toString();
		//	valutazioneEsperienze_json = valutazioneEsperienze_json.replaceAll("T23:00:00.000Z", "");
		//	execution.setVariable("valutazioneEsperienze_json", valutazioneEsperienze_json);
		//}


		//		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		//		String dataInizioStr = "00-00-0000";
		//		String dataFineStr = "00-00-0000";
		//		Date dataInizioShort = sdf.parse("00-00-0000");
		//		Date dataFineShort = sdf.parse("00-00-0000");
		//		String valutazioneEsperienzeString = (String) execution.getVariable("valutazioneEsperienze_json");
		//		JSONArray valutazioneEsperienze = new JSONArray();
		//		try {
		//			valutazioneEsperienze = new JSONArray(valutazioneEsperienzeString);
		//
		//			for ( int i = 0; i < valutazioneEsperienze.length(); i++) {
		//				JSONObject esperienza = new JSONObject();
		//				esperienza = valutazioneEsperienze.getJSONObject(i);
		//				dataInizioStr = esperienza.getString("dataInizio");
		//				dataInizioShort = sdf.parse(dataInizioStr);
		//				esperienza.put("dataInizioShort", i);
		//				dataFineStr = esperienza.getString("dataFine");
		//				dataFineShort = sdf.parse(dataFineStr);
		//				esperienza.put("dataFineShort", i);
		//			}
		//		} catch (JSONException e1) {
		//			// TODO Auto-generated catch block
		//			e1.printStackTrace();
		//		}
		//		execution.setVariable("valutazioneEsperienze_json", valutazioneEsperienze.toString());

		LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoIstruttori, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirettore, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmaRigetto, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoCoordinatoreResponsabile, PROCESS_VISUALIZER);
	}

	public void configuraVariabiliEnd(DelegateExecution execution)  throws IOException, ParseException  {

		Date dataNow = new Date();
		String processInstanceId =  execution.getProcessInstanceId();
		Map<String, Object> processInstanceDetails = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
		HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) processInstanceDetails.get("entity");
		int giorniDurataFlusso = operazioniTimer.calcolaGiorniTraDate(processInstance.getStartTime(), dataNow);
		int giorniEffettiviDurataProcedimentoOiv =  giorniDurataFlusso - parseInt(execution.getVariable("giorniDurataPreavvisoRigetto").toString()) - parseInt(execution.getVariable("giorniDurataSoccorsoIstruttorio").toString()); 
		execution.setVariable("giorniEffettiviDurataProcedimentoOiv", giorniEffettiviDurataProcedimentoOiv);
		LOGGER.info("------ giorniEffettiviDurataProcedimentoOiv: " + execution.getVariable("giorniEffettiviDurataProcedimentoOiv"));

	}
}

