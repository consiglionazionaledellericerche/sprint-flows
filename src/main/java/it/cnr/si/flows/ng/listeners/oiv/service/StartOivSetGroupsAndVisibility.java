package it.cnr.si.flows.ng.listeners.oiv.service;


import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;


import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Service
public class StartOivSetGroupsAndVisibility {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartOivSetGroupsAndVisibility.class);


    @Inject
    private RuntimeService runtimeService;

    public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

        String initiator = (String) execution.getVariable("initiator");
        LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));

        String struttura = "99999";
        String gruppoIstruttori = "istruttore@"+ struttura;
        String gruppoDirettore = "direttore@"+ struttura;
        String gruppoCoordinatoreResponsabile = "coordinatoreresponsabile@"+ struttura;

        execution.setVariable("gruppoIstruttori", gruppoIstruttori);
        execution.setVariable("gruppoDirettore", gruppoDirettore);
        execution.setVariable("gruppoCoordinatoreResponsabile", gruppoCoordinatoreResponsabile);
        execution.setVariable("domandaImprocedibileFlag", "0");
        execution.setVariable("soccorsoIstruttoriaFlag", "0");
        LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}, {}", gruppoIstruttori, gruppoDirettore, gruppoCoordinatoreResponsabile);

        //conversione semplice di date
//		if (execution.getVariable("valutazioneEsperienze_json") !=  null) {
//			String valutazioneEsperienze_json = execution.getVariable("valutazioneEsperienze_json").toString();
//			valutazioneEsperienze_json = valutazioneEsperienze_json.replaceAll("T23:00:00.000Z", "");
//			execution.setVariable("valutazioneEsperienze_json", valutazioneEsperienze_json);
//		}

        LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));
        runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoIstruttori, PROCESS_VISUALIZER);
        runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirettore, PROCESS_VISUALIZER);
        runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoCoordinatoreResponsabile, PROCESS_VISUALIZER);
        runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoCoordinatoreResponsabile, PROCESS_VISUALIZER);

    }
}
