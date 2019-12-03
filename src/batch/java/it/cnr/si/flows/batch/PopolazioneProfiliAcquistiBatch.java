package it.cnr.si.flows.batch;

import com.opencsv.CSVParser;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
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

		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/190627 associazione utenze ruolo per ACE 07 maggio.csv"));

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





















