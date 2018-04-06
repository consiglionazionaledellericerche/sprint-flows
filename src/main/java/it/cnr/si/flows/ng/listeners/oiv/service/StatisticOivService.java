package it.cnr.si.flows.ng.listeners.oiv.service;

import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.inject.Inject;
import java.text.ParseException;
import java.util.*;
import java.util.Calendar;
import static it.cnr.si.flows.ng.utils.Utils.*;


/**
 * Created by massimo on 04/04/2018.
 */
@Service
public class StatisticOivService {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticOivService.class);

	@Inject
	FlowsProcessInstanceService flowsProcessInstanceService;
	@Inject
	private Utils utils;

	// ELENCO PARAMETRI STATISTICHE
	int domandeTotali = 0;
	int domandeInEsame = 0;
	int domandeEsaminate = 0;
	int nrUominiFascia1 = 0;
	int nrUominiFascia2 = 0;
	int nrUominiFascia3 = 0;
	int nrDonneFascia1 = 0;
	int nrDonneFascia2 = 0;
	int nrDonneFascia3 = 0;

	int nrFaseSmistamento = 0;
	int nrFaseIstruttoria = 0;
	int nrFaseSoccorsoIstruttorio = 0;
	int nrFaseCambioIstruttore = 0;
	int nrFaseValutazione = 0;
	int nrFasePreavvisoRigetto = 0;
	int nrFaseIstruttoriaSuPreavviso = 0;
	int nrFaseValutazionePreavviso = 0;
	int nrFaseFirmaDgRigetto = 0;

	int nrDomandeImprocedibili = 0;
	int nrDomandeApprovate = 0;
	int nrDomandeRespinte = 0;
	int nrDomandeSoccorsoIstruttorio = 0;

	int nrDomandeTempiProcedimentaliInScadenza = 0;
	int nrDomandeTempiProcedimentaliScaduti = 0;
	int nrDomandeTempiSoccorsoIstruttorioScaduti = 0;
	int nrDomandeTempiPreavvisoRigettoScaduti = 0;

	Calendar newDate = Calendar.getInstance();	
	Date dataOdierna = newDate.getTime();
	Date dataScadenzaTerminiDomanda = newDate.getTime();
	Date dataInvioDomanda = newDate.getTime();
	Date dataPreavviso = newDate.getTime();

	int nrGiorniInvioDomanda = 0;
	int nrGiorniScadenzaTerminiDomanda = 0;
	int nrGiorniDataPreavviso = 0;
	int nrGiorniCompletamentoDomanda = 0;
	int nrGiorniExtraTerminiDomanda = 0;

	int nrDomandeScadenza_0_5 = 0;
	int nrDomandeScadenza_5_10 = 0;
	int nrDomandeScadenza_10_more = 0;
	int nrDomandeExtraTermini_0_5 = 0;
	int nrDomandeExtraTermini_5_10 = 0;
	int nrDomandeExtraTermini_10_more = 0;	

	String sessoRichiedente = "";
	String fasciaAppartenenzaAttribuita = "";
	String tipologiaRichiesta = "";
	String faseUltima = "";
	String statoFinaleDomanda = "";
	String tempiProcedimentaliDomanda = "";
	String tempiSoccorsoIstruttorio = "";
	String tempiPreavvisoRigetto = "";	
	String soccorsoIstruttoriaFlag = "";	

	public void getOivStatistics (String processDefinitionKey, String startDateGreat, String startDateLess) throws ParseException {
		Map<String, String> req = new HashMap<>();
		req.put("startDateGreat", startDateGreat);
		req.put("startDateLess", startDateLess);
		req.put(processDefinitionKey, processDefinitionKey);
		String order = "ASC";
		Integer firstResult = -1;
		Integer maxResults = -1;
		Boolean active = true;
		Boolean finished = false;

		resetStatisticvariables();

		Map<String, Object>  flussiAttivi = flowsProcessInstanceService.search(req, processDefinitionKey, active, order, firstResult, maxResults);
		Map<String, Object>  flussiTerminati = flowsProcessInstanceService.search(req, processDefinitionKey, finished, order, firstResult, maxResults);

		//VALORIZZAZIONE PARAMETRI STATISTICHE
		domandeInEsame = parseInt(flussiAttivi.get("totalItems").toString());
		domandeEsaminate = parseInt(flussiTerminati.get("totalItems").toString());
		domandeTotali = domandeInEsame + domandeEsaminate;


		LOGGER.debug("nr. domandeInEsame: {} - nr. domandeEsaminate: {} - nr. domandeTotali: {}", domandeInEsame, domandeEsaminate, domandeTotali);

		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI ATTIVI
		List<HistoricProcessInstanceResponse> activeProcessInstances = (List<HistoricProcessInstanceResponse>) flussiAttivi.get("processInstances");
		for (HistoricProcessInstanceResponse pi : activeProcessInstances) {
			LOGGER.debug(" getId= " + pi.getId());
			LOGGER.debug(" getDurationInMillis= " + pi.getDurationInMillis());
			LOGGER.debug(" elementi= " + pi.getName());
			String processInstanceId = pi.getId();
			Map<String, Object> processInstanceDetails = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
			HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) processInstanceDetails.get("entity");
			List<RestVariable> variables = processInstance.getVariables();

			mappaturaVariabili(variables);
			statoFlussiAttivi();
			determinaSessoFascia();
			statoScadenzeTemporali();
			calcolaNrSoccorsoIstruttorio();
			calcolaVariabiliDateFlussiAttivi();
		}

		// GESTIONE VARIABILI SINGOLE ISTANZE FLUSSI TERMINATI
		List<HistoricProcessInstanceResponse> terminatedProcessInstances = (List<HistoricProcessInstanceResponse>) flussiTerminati.get("processInstances");
		for (HistoricProcessInstanceResponse pi : terminatedProcessInstances) {
			LOGGER.debug(" getId= " + pi.getId());
			LOGGER.debug(" getDurationInMillis= " + pi.getDurationInMillis());
			LOGGER.debug(" elementi= " + pi.getName());
			String processInstanceId = pi.getId();
			Map<String, Object> processInstanceDetails = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
			HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) processInstanceDetails.get("entity");
			List<RestVariable> variables = processInstance.getVariables();

			mappaturaVariabili(variables);
			statoFlussiCompletati();
			determinaSessoFascia();
			statoScadenzeTemporali();
			calcolaNrSoccorsoIstruttorio();
			calcolaVariabiliDateFlussiCompletati(pi);
		}

		LOGGER.info("-- nrUominiFascia1: {} - nrUominiFascia2: {} - nrUominiFascia3: {} - nrUominiTotale: {} ",  nrUominiFascia1, nrUominiFascia2, nrUominiFascia3, nrUominiFascia1 + nrUominiFascia2 + nrUominiFascia3);
		LOGGER.info("-- nrDonneFascia1: {} - nrDonneFascia2: {} - nrDonneFascia3: {} - nrDonneTotale: {} ",  nrDonneFascia1, nrDonneFascia2, nrDonneFascia3, nrDonneFascia1 + nrDonneFascia2 + nrDonneFascia3);
		LOGGER.info("-- STATO DOMANDE ATTIVE nrFaseSmistamento: {} - nrFaseIstruttoria: {} - nrFaseSoccorsoIstruttorio: {} - nrFaseCambioIstruttore: {} - nrFaseValutazione: {} - nrFasePreavvisoRigetto: {} - nrFaseIstruttoriaSuPreavviso: {} - nrFaseValutazionePreavviso: {} - nrFaseFirmaDgRigetto: {} ", nrFaseSmistamento, nrFaseIstruttoria, nrFaseSoccorsoIstruttorio, nrFaseCambioIstruttore, nrFaseValutazione, nrFasePreavvisoRigetto, nrFaseIstruttoriaSuPreavviso, nrFaseValutazionePreavviso, nrFaseFirmaDgRigetto);
		LOGGER.info("-- STATO DOMANDE TERMINATE nrDomandeImprocedibili: {} - nrDomandeApprovate: {} - nrDomandeRespinte: {} - nrDomandeTerminateTotale: {} ",  nrDomandeImprocedibili, nrDomandeApprovate, nrDomandeRespinte, nrDomandeImprocedibili + nrDomandeApprovate + nrDomandeRespinte);
		LOGGER.info("-- STATO TEMPI DOMANDE nrDomandeTempiProcedimentaliInScadenza: {} - nrDomandeTempiProcedimentaliScaduti: {} - nrDomandeTempiSoccorsoIstruttorioScaduti: {} - nrDomandeTempiPreavvisoRigettoScaduti: {} ",  nrDomandeTempiProcedimentaliInScadenza, nrDomandeTempiProcedimentaliScaduti, nrDomandeTempiSoccorsoIstruttorioScaduti, nrDomandeTempiPreavvisoRigettoScaduti);
		LOGGER.info("-- STATO TEMPI DOMANDE nrDomandeScadenza_0_5: {} - nrDomandeScadenza_5_10: {} - nrDomandeScadenza_10_more: {}",  nrDomandeScadenza_0_5, nrDomandeScadenza_5_10, nrDomandeScadenza_10_more);
		LOGGER.info("-- STATO TEMPI DOMANDE nrDomandeExtraTermini_0_5: {} - nrDomandeExtraTermini_5_10: {} - nrDomandeExtraTermini_10_more: {}",  nrDomandeExtraTermini_0_5, nrDomandeExtraTermini_5_10, nrDomandeExtraTermini_10_more);	
	}

	private void mappaturaVariabili(List<RestVariable> variables) throws ParseException {
		//GESTIONE DEI PARAMETRI DA VISUALIZZARE
		for (RestVariable var : variables) {
			String variableName = var.getName().toString();
			switch(variableName){  
			case "sessoRichiedente": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				sessoRichiedente = var.getValue().toString();
			};break; 
			case "fasciaAppartenenzaAttribuita": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				fasciaAppartenenzaAttribuita = var.getValue().toString();
			};break; 
			case "tipologiaRichiesta": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				tipologiaRichiesta = var.getValue().toString();
			};break; 
			case "statoFinaleDomanda": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				statoFinaleDomanda = var.getValue().toString();
			};break;
			case "faseUltima": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				faseUltima = var.getValue().toString();
			};break;
			case "dataInvioDomanda": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				dataInvioDomanda = utils.parsaData(var.getValue().toString());
			};break;
			case "dataScadenzaTerminiDomanda": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				dataScadenzaTerminiDomanda = utils.parsaData(var.getValue().toString());
			};break;
			case "dataPreavviso": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				dataPreavviso = utils.parsaData(var.getValue().toString());
			};break;		    
			case "tempiProcedimentaliDomanda": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				tempiProcedimentaliDomanda = var.getValue().toString();
			};break;
			case "tempiSoccorsoIstruttorio": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				tempiSoccorsoIstruttorio = var.getValue().toString();
			};break;
			case "tempiPreavvisoRigetto": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				tempiPreavvisoRigetto = var.getValue().toString();
			};break;
			case "soccorsoIstruttoriaFlag": {
				LOGGER.info("-- " + var.getName() + ": " + var.getValue());
				soccorsoIstruttoriaFlag = var.getValue().toString();
			};break;
			default:  {
				//LOGGER.info("-- " + var.getName() + ": " + var.getValue());
			};break;  

			}
		}
	}	



	private void determinaSessoFascia() {
		if (sessoRichiedente.equals("M")){
			switch(fasciaAppartenenzaAttribuita){  
			case "1": {
				nrUominiFascia1 = nrUominiFascia1 +1;
			};break;   
			case "2": {
				nrUominiFascia2 = nrUominiFascia2 +1;
			};break;   
			case "3": {
				nrUominiFascia3 = nrUominiFascia3 +1;
			};break; 
			} 
		}else {
			switch(fasciaAppartenenzaAttribuita){  
			case "1": {
				nrDonneFascia1 = nrDonneFascia1 +1;
			};break;   
			case "2": {
				nrDonneFascia2 = nrDonneFascia2 +1;
			};break;   
			case "3": {
				nrDonneFascia3 = nrDonneFascia3 +1;
			};break; 
			}
		}
	}	

	private void statoFlussiAttivi() {
		switch(faseUltima){    
		case "smistamento": {
			nrFaseSmistamento = nrFaseSmistamento + 1;
		};break; 
		case "istruttoria": {
			nrFaseIstruttoria = nrFaseIstruttoria + 1;
		};break;
		case "soccorso-istruttorio":  {
			nrFaseSoccorsoIstruttorio = nrFaseSoccorsoIstruttorio + 1;
		};break;
		case "cambio-istruttore":  {
			nrFaseCambioIstruttore = nrFaseCambioIstruttore + 1;
		};break;  
		case "valutazione":  {
			nrFaseValutazione = nrFaseValutazione + 1;
		};break;     
		case "preavviso-rigetto":  {
			nrFasePreavvisoRigetto = nrFasePreavvisoRigetto + 1;
		};break;  
		case "istruttoria-su-preavviso":  {
			nrFaseIstruttoriaSuPreavviso = nrFaseIstruttoriaSuPreavviso + 1;
		};break;    
		case "valutazione-preavviso":  {
			nrFaseValutazionePreavviso = nrFaseValutazionePreavviso + 1;
		};break;
		case "firma-dg-rigetto":  {
			nrFaseFirmaDgRigetto = nrFaseFirmaDgRigetto + 1;
		};break;
		default:  {
			LOGGER.info("--faseUltima: " + faseUltima);
		};break; 
		}
	}

	private void statoFlussiCompletati() {
		switch(statoFinaleDomanda){    
		case "IMPROCEDIBILE": {
			nrDomandeImprocedibili = nrDomandeImprocedibili +1;
		};break; 
		case "DOMANDA APPROVATA": {
			nrDomandeApprovate = nrDomandeApprovate +1;
		};break;
		case "RESPINTA":  {
			nrDomandeRespinte = nrDomandeRespinte +1;
		};break;
		default:  {
			LOGGER.info("--statoFinaleDomanda: " + statoFinaleDomanda);
		};break; }
	}

	private void statoScadenzeTemporali() {
		if (tempiProcedimentaliDomanda.equals("IN SCADENZA")){
			nrDomandeTempiProcedimentaliInScadenza = nrDomandeTempiProcedimentaliInScadenza +1;
		}
		if (tempiProcedimentaliDomanda.equals("SCADUTI")){
			nrDomandeTempiProcedimentaliScaduti = nrDomandeTempiProcedimentaliScaduti +1;
		}
		if (tempiSoccorsoIstruttorio.equals("SCADUTI")){
			nrDomandeTempiSoccorsoIstruttorioScaduti = nrDomandeTempiSoccorsoIstruttorioScaduti +1;
		}
		if (tempiPreavvisoRigetto.equals("SCADUTI")){
			nrDomandeTempiPreavvisoRigettoScaduti = nrDomandeTempiPreavvisoRigettoScaduti +1;
		}
	}

	private void calcolaNrSoccorsoIstruttorio() {
		if (soccorsoIstruttoriaFlag.equals("1")){
			nrDomandeSoccorsoIstruttorio = nrDomandeSoccorsoIstruttorio +1;
		}	
	}

	private void calcolaVariabiliDateFlussiAttivi() {
		nrGiorniInvioDomanda = calcolaGiorniTraDate(dataInvioDomanda, dataOdierna);
		nrGiorniScadenzaTerminiDomanda = calcolaGiorniTraDate(dataOdierna, dataScadenzaTerminiDomanda);
		nrGiorniDataPreavviso = calcolaGiorniTraDate(dataPreavviso, dataOdierna);
		LOGGER.debug("--- nrGiorniInvioDomanda: {} nrGiorniScadenzaTerminiDomanda: {} nrGiorniDataPreavviso: {}", nrGiorniInvioDomanda, nrGiorniScadenzaTerminiDomanda, nrGiorniDataPreavviso);
		if ( 0 < nrGiorniScadenzaTerminiDomanda && nrGiorniScadenzaTerminiDomanda <= 5) {
			nrDomandeScadenza_0_5 = nrDomandeScadenza_0_5 +1;
		}
		if ( 5 < nrGiorniScadenzaTerminiDomanda && nrGiorniScadenzaTerminiDomanda <= 10) {
			nrDomandeScadenza_5_10 = nrDomandeScadenza_5_10 +1;
		}
		if ( 10 < nrGiorniScadenzaTerminiDomanda) {
			nrDomandeScadenza_10_more = nrDomandeScadenza_10_more +1;
		}
	}

	private void calcolaVariabiliDateFlussiCompletati(HistoricProcessInstanceResponse processInstance) {
		nrGiorniCompletamentoDomanda = (int) (processInstance.getDurationInMillis()/ (1000 * 60 * 60 * 24));
		nrGiorniExtraTerminiDomanda = calcolaGiorniTraDate(processInstance.getEndTime(), dataScadenzaTerminiDomanda);
		LOGGER.debug("--- nrGiorniCompletamentoDomanda: {} -  nrGiorniExtraTerminiDomanda: {}", nrGiorniCompletamentoDomanda, nrGiorniExtraTerminiDomanda);
		if ( 0 < nrGiorniExtraTerminiDomanda && nrGiorniExtraTerminiDomanda <= 5) {
			nrDomandeExtraTermini_0_5 = nrDomandeExtraTermini_0_5 +1;
		}
		if ( 5 < nrGiorniExtraTerminiDomanda && nrGiorniExtraTerminiDomanda <= 10) {
			nrDomandeExtraTermini_5_10 = nrDomandeExtraTermini_5_10 +1;
		}
		if ( 10 < nrGiorniExtraTerminiDomanda) {
			nrDomandeExtraTermini_10_more = nrDomandeExtraTermini_10_more +1;
		}
	}



	private int calcolaGiorniTraDate(Date dateInf, Date dateSup) {

		int timeVariableRecordDateValue =(int) (dateSup.getTime() - dateInf.getTime());
		int timeVariableRecordDateDays = timeVariableRecordDateValue/ (1000 * 60 * 60 * 24);
		int timeVariableRecordDateHours = timeVariableRecordDateValue/ (1000 * 60 * 60);
		int timeVariableRecordDateMinutes = timeVariableRecordDateValue/ (1000 * 60);

		LOGGER.debug("--- {} gg diff tra : {} e: {}", timeVariableRecordDateDays, dateInf, dateSup);
		return timeVariableRecordDateDays;
	}

	private String formatDate(Date date) {
		return date != null ? utils.formattaDataOra(date) : "";
	}

	private void resetStatisticvariables() {

		domandeTotali = 0;
		domandeInEsame = 0;
		domandeEsaminate = 0;
		nrUominiFascia1 = 0;
		nrUominiFascia2 = 0;
		nrUominiFascia3 = 0;
		nrDonneFascia1 = 0;
		nrDonneFascia2 = 0;
		nrDonneFascia3 = 0;

		nrFaseSmistamento = 0;
		nrFaseIstruttoria = 0;
		nrFaseSoccorsoIstruttorio = 0;
		nrFaseCambioIstruttore = 0;
		nrFaseValutazione = 0;
		nrFasePreavvisoRigetto = 0;
		nrFaseIstruttoriaSuPreavviso = 0;
		nrFaseValutazionePreavviso = 0;
		nrFaseFirmaDgRigetto = 0;

		nrDomandeImprocedibili = 0;
		nrDomandeApprovate = 0;
		nrDomandeRespinte = 0;
		nrDomandeSoccorsoIstruttorio = 0;

		nrDomandeTempiProcedimentaliInScadenza = 0;
		nrDomandeTempiProcedimentaliScaduti = 0;
		nrDomandeTempiSoccorsoIstruttorioScaduti = 0;
		nrDomandeTempiPreavvisoRigettoScaduti = 0;
		dataOdierna = newDate.getTime();
		dataScadenzaTerminiDomanda = newDate.getTime();
		dataInvioDomanda = newDate.getTime();
		dataPreavviso = newDate.getTime();

		nrGiorniInvioDomanda = 0;
		nrGiorniScadenzaTerminiDomanda = 0;
		nrGiorniDataPreavviso = 0;
		nrGiorniCompletamentoDomanda = 0;
		nrGiorniExtraTerminiDomanda = 0;

		nrDomandeScadenza_0_5 = 0;
		nrDomandeScadenza_5_10 = 0;
		nrDomandeScadenza_10_more = 0;
		nrDomandeExtraTermini_0_5 = 0;
		nrDomandeExtraTermini_5_10 = 0;
		nrDomandeExtraTermini_10_more = 0;	
		sessoRichiedente = "";
		fasciaAppartenenzaAttribuita = "";
		tipologiaRichiesta = "";
		faseUltima = "";
		statoFinaleDomanda = "";
		tempiProcedimentaliDomanda = "";
		tempiSoccorsoIstruttorio = "";
		tempiPreavvisoRigetto = "";	
		soccorsoIstruttoriaFlag = "";

	}

}
