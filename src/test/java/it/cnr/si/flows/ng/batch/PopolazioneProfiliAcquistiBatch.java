package it.cnr.si.flows.ng.batch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.time.LocalDate;

import it.cnr.si.FlowsApp;
import org.activiti.spring.boot.AbstractProcessEngineAutoConfiguration;
import org.activiti.spring.boot.DataSourceProcessEngineAutoConfiguration;
import org.activiti.spring.boot.EndpointAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration;
import org.activiti.spring.boot.RestApiAutoConfiguration;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration.JpaConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.mvc.method.annotation.CompletionStageReturnValueHandler;

import com.opencsv.CSVParser;

import it.cnr.si.SprintApp;
import it.cnr.si.config.Constants;
import it.cnr.si.config.DefaultProfileUtil;
import it.cnr.si.config.JHipsterProperties;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.base.PageDto;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class PopolazioneProfiliAcquistiBatch {

	private static final Logger log = LoggerFactory.getLogger(PopolazioneProfiliAcquistiBatch.class);

	@Inject
	private AceService aceService;
	@Inject
	private AceBridgeService aceBridgeService;
	private final Map<String, String> errors = new HashMap<>();

	//@Test questa riga non va mai messa su git
	public void runBatch() throws IOException {
		Map<String, String> persone = getPersoneDaFile();

		persone.forEach( (username, siglaRuolo) -> {
			inserisciRuolo(username, siglaRuolo);
		});

		errors.forEach( (tripla, risultato) -> {
			log.error(tripla +": "+ risultato);
		});
	}

	private void inserisciRuolo(String username, String siglaRuolo) {

		EntitaOrganizzativaWebDto afferenzaUtente = aceBridgeService.getAfferenzaUtente(username);
		String cdsuo = afferenzaUtente.getCdsuo();


		List<EntitaOrganizzativaWebDto> eos = aceService.entitaOrganizzativaFind(null, null, cdsuo, LocalDate.now(), null).getItems();
		PersonaWebDto persona = aceService.personaByUsername(username);

		eos.forEach(eo -> {

			Integer idRuolo = aceService.getRuoloBySigla(siglaRuolo).getId();
			Integer idEo = eo.getId();
			Integer idPersona = persona.getId();

			try {
				aceService.associaRuoloPersona(idRuolo, idPersona, idEo);
				log.info("Associato ruolo {} persona {} eo {}", idRuolo, idPersona, idEo);
				errors.put(username + " "+ siglaRuolo + " "+ eo.getSigla() + "("+ eo.getId() +")", "OK");
			} catch (RuntimeException e) {
				if (e.getMessage().contains("Il Ruolo specificato e' gia' presente")) {
					errors.put(username + " "+ siglaRuolo + " "+ eo.getSigla() + "("+ eo.getId() +")", "PRESENTE");
				} else {
					log.error("Errore nella richiesta", e);
					errors.put(username + " "+ siglaRuolo + " "+ eo.getSigla() + "("+ eo.getId() +")", e.getMessage());	
				}
			}
		});


	}

	private Map<String, String> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		Stream<String> lines = Files.lines(Paths.get("./src/test/resources/batch/190627 associazione utenze ruolo per ACE 07 maggio.csv"));

		Map<String, String> associazioni = new HashMap<>();

		lines
				.skip(1).
				forEach(l -> {
					try {

						String[] values = parser.parseLine(l);
						log.info(values[0] + " " + values[1]);
						associazioni.put(values[0], values[1]);

					} catch (IOException e) {e.printStackTrace();}
				});


		return associazioni;
	}
}





















