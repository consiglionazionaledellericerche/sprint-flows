package it.cnr.si.flows.batch;

import com.opencsv.CSVParser;
import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.GerarchiaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.EntitaOrganizzativaDto;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
public class AggiornaFlussiAcquistiPerTrasparenzaURP {

	private static final Logger log = LoggerFactory.getLogger(AggiornaFlussiAcquistiPerTrasparenzaURP.class);

	@Inject
	private AceService aceService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private SiperService siperService;
	@Inject
	private MembershipService membershipService;
	@Inject
	private HistoryService historyService;
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private RelationshipService relationshipService;
	@Value("${ace.url}")
	private String aceURL;

	private final Map<String, String> errors = new HashMap<>();
	int personNr = 1;
	int cnt = 0;
	List <String> results = new ArrayList<>();
	EntitaOrganizzativaWebDto entitaOrganizzativaResponsabileStruttura = null;

	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {

		results.add("---------- LISTA RISULTATI -----------------");


		HistoricProcessInstanceQuery historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery()
				.includeProcessVariables()
				.processDefinitionKey("acquisti")
				.unfinished();
		//.processInstanceNameLike("%Pre Determina%")
		//.variableValueEquals("flagIsTrasparenza", "true");
		historicProcessInstanceQuery.list().forEach(processInstance -> {
			cnt++;
			log.info("{}) il processo: {}, id = {} del flusso {}", cnt, processInstance.getId(), processInstance.getName(), processInstance.getProcessDefinitionKey());
			if(processInstance.getProcessVariables().get("strumentoAcquisizione") != null) {
				log.info(" strumentoAcquisizione: {} ", processInstance.getProcessVariables().get("strumentoAcquisizione").toString());
			}
			setFlagIsTrasparenza(processInstance);
			setDataScadenzaBando(processInstance);
			setStatoFinaleDomanda(processInstance);
		});

		//RPINT RISULTATI
		results.forEach(result -> {
			log.info(result);
		});
	}

	private void setFlagIsTrasparenza(HistoricProcessInstance processInstance) {

		if(!(processInstance.getName().indexOf("Modifica Decisione") != -1) 
				&& !(processInstance.getName().indexOf("Pre Determina") != -1)
				&& !(processInstance.getName().indexOf("Verifica Decisione") != -1)
				&& !(processInstance.getName().indexOf("Firma Decisione") != -1)
				&& !(processInstance.getName().indexOf("Annulla Decisione") != -1)
				&& !(processInstance.getName().indexOf("Protocollo Decisione") != -1)
				)
		{
			if (processInstance.getProcessVariables().get("flagIsTrasparenza") != null) {
				log.info(" ***  il processo: " + processInstance.getId() + " -- " + processInstance.getName() + " flagIsTrasparenza: " + processInstance.getProcessVariables().get("flagIsTrasparenza").toString());
			} else {
				results.add(" *** AGGIUNGO flagIsTrasparenza true al processo: " + processInstance.getId() + " -- " + processInstance.getName() );
				runtimeService.setVariable(processInstance.getId(), "flagIsTrasparenza", "true");
			}
		}
	}

	private void setDataScadenzaBando(HistoricProcessInstance processInstance) {

		if(!(processInstance.getName().indexOf("Modifica Decisione") != -1) 
				&& !(processInstance.getName().indexOf("Pre Determina") != -1)
				&& !(processInstance.getName().indexOf("Verifica Decisione") != -1)
				&& !(processInstance.getName().indexOf("Firma Decisione") != -1)
				&& !(processInstance.getName().indexOf("Annulla Decisione") != -1)
				&& !(processInstance.getName().indexOf("Protocollo Decisione") != -1)
				&& (processInstance.getProcessVariables().get("strumentoAcquisizione").toString().indexOf("PROCEDURA SELETTIVA") != -1)
				)
		{
			if (processInstance.getProcessVariables().get("dataScadenzaBando") != null) {
				log.info(" ---- " + processInstance.getId() + " -- "  + processInstance.getName() + " dataScadenzaBando: " + processInstance.getProcessVariables().get("dataScadenzaBando").toString());
			} else {
				results.add(" ---- AGGIUNGO dataScadenzaBando 2019-09-12T22:00:00.000Z al processo: " + processInstance.getId() + " -- "  + processInstance.getName() );
				runtimeService.setVariable(processInstance.getId(), "dataScadenzaBando", "2019-09-12T22:00:00.000Z");
			}
		}
	}

	private void setStatoFinaleDomanda(HistoricProcessInstance processInstance) {

		if (processInstance.getProcessVariables().get("statoFinaleDomanda") != null) {
			log.info(" *** il processo: " + processInstance.getId() + " -- " + processInstance.getName() + " statoFinaleDomanda: " + processInstance.getProcessVariables().get("statoFinaleDomanda").toString());
		} else {
			results.add(" *** AGGIUNGO statoFinaleDomanda IN CORSO al processo: " + processInstance.getId() + " -- " + processInstance.getName() );
			runtimeService.setVariable(processInstance.getId(), "statoFinaleDomanda", "IN CORSO");
		}
	}
}






















