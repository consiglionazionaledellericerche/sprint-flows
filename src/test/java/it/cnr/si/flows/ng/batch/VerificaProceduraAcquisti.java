package it.cnr.si.flows.ng.batch;

import com.opencsv.CSVParser;
import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore // TODO TUTTI I TESTS FUNZIONALI e DIPENDENTI DA ACE/SIPER DA SPOSTARE FUORI DA JUNIT
public class VerificaProceduraAcquisti {

	private static final Logger log = LoggerFactory.getLogger(VerificaProceduraAcquisti.class);

	@Inject
	private AceService aceService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private SiperService siperService;
	private final Map<String, String> errors = new HashMap<>();
	int personNr = 0;

	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {
		Map<String, String> persone = getPersoneDaFile();

		persone.forEach( (username, siglaRuolo) -> {
			log.info("****** VERIFICA RESPONSABILE PER UTENTE {}  nr.{} di:{} totali ******", username, personNr, persone.size());
			personNr = personNr + 1;
			verificaDirettore(username, siglaRuolo);
		});

		errors.forEach( (tripla, risultato) -> {
			log.error(tripla +": "+ risultato);
		});
	}

	private void verificaDirettore(String username, String siglaRuolo) {

		log.info("****** UTENTE {} ******", username);

		String cdsuoAppartenenzaUtente = null;
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(username).getCdsuo();
		} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
			log.info("L'UTENTE {} NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ", username);
			try {
				cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(username).get("codice_uo").toString();
			} catch(UnexpectedResultException | FeignException | HttpClientErrorException error2) {
				log.info("L'UTENTE {} NON esiste in anagrafica SIPER !!!!!!!!!!!!!!! ", username);
			}
			finally {
				log.info("------------------------------ ");
			}
		}
		finally {
			if (cdsuoAppartenenzaUtente == null) {
				log.info("L'UTENTE {} NON esiste in anagrafica ACE e SIPER !!!!!!!!!!!!!!! ", username);
			} else {
				Map<String, Object> responsabileUo;
				try {
					responsabileUo = siperService.getResponsabileCDSUO(cdsuoAppartenenzaUtente).get(0);
					String idnsipResponsabileUo = responsabileUo.get("codice_sede").toString();
					log.info("-------------- getResponsabileCDSUO  FUNZIONA per CDSUO {} con IDNSIP {}", cdsuoAppartenenzaUtente, idnsipResponsabileUo);
					String usernameResponsabile = responsabileUo.get("uid").toString();
					EntitaOrganizzativaWebDto entitaOrganizzativaRespUo = null;
					try {
						entitaOrganizzativaRespUo = aceService.entitaOrganizzativaFindByTerm(idnsipResponsabileUo.toString()).get(0);
					} catch(UnexpectedResultException | FeignException | HttpClientErrorException error4) {
						log.info("-------------- entitaOrganizzativaRespUo  NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la IDNSIP {}", idnsipResponsabileUo);
					}
			
					Integer idEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getId();
					String siglaEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getSigla().toString();
					String denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getDenominazione().toString();
					String cdsuoEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getCdsuo().toString();
					String idnsipEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getIdnsip().toString();
					log.info("L'utente {} ha come Responsabile {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", username, usernameResponsabile, denominazioneEntitaorganizzativaResponsabileUtente, siglaEntitaorganizzativaResponsabileUtente, idEntitaorganizzativaResponsabileUtente, cdsuoEntitaorganizzativaResponsabileUtente, idnsipEntitaorganizzativaResponsabileUtente);
				} catch(UnexpectedResultException | FeignException | HttpClientErrorException error3) {
					log.info("-------------- getResponsabileCDSUO  NON HA FUNZIONATO!!!!!!!!!!!!!!! l'utente {} non ha Responsabile per la CDSUO {}", username, cdsuoAppartenenzaUtente);
				}
			}
		}
	}

	private Map<String, String> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		Stream<String> lines = Files.lines(Paths.get("./src/test/resources/batch/utentiProceduraAcquisti.csv"));

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





















