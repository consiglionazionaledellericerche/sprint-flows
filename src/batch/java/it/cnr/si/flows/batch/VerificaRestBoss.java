package it.cnr.si.flows.batch;

import com.opencsv.CSVParser;
import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.FlowsLdapAccountService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.GerarchiaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.scritture.EntitaOrganizzativaDto;

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
public class VerificaRestBoss {

	private static final Logger log = LoggerFactory.getLogger(VerificaRestBoss.class);

	@Inject
	private AceService aceService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private SiperService siperService;
	@Inject
	private MembershipService membershipService;
	@Inject
	private RelationshipService relationshipService;
	@Value("${ace.url}")
	private String aceURL;

	@Inject
	private FlowsLdapAccountService flowsLdapAccountService;


	private final Map<String, String> errors = new HashMap<>();
	int personNr = 1;
	List <String> results = new ArrayList<>();
	EntitaOrganizzativaWebDto entitaOrganizzativaResponsabileStruttura = null;

	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {
		Map<String, String> persone = getPersoneDaFile();

		results.add("---------- LISTA RISULTATI -----------------");

		persone.forEach( (username, siglaRuolo) -> {
			log.info("****** VERIFICA DIRETTORE PER UTENTE {}  nr.{} di:{} totali ******", username, personNr, persone.size());
			personNr = personNr + 1;
			verifica4RestBoss(username, siglaRuolo);
		});

		errors.forEach( (tripla, risultato) -> {
			log.error(tripla +": "+ risultato);
		});

		//RPINT RISULTATI
		results.forEach(result -> {
			log.info(result);
		});
	}

	private void verifica4RestBoss(String username, String siglaRuolo) {

		log.info("****** UTENTE {} ******", username);
		BossDto direttoreACE = null;
		BossDto responsabileSede = null;
		BossDto direttore0 = null;
		BossDto direttore1 = null;
		BossDto direttore2 = null;
		BossDto bossSede = null;

		String usernameDirettoreACE = "";
		String usernameDirettoreSIPER = "";
		String denominazioneEntitaOrganizzativaDirettoreAce = null;
		String denominazioneEntitaOrganizzativaDirettoreSIPER = null;
		String siglaEntitaOrganizzativaDirettoreACE = null;
		String siglaEntitaOrganizzativaDirettoreSIPER = null;
		String ruoloEntitaOrganizzativaDirettore = null;
		String cdsuoAppartenenzaUtente = null;
		Integer idEntitaOrganizzativaDirettoreACE = 0;


		List<Map<String, String>> utente = flowsLdapAccountService.getFulluser(username);
		if (utente.size() > 0) {
			if (utente.get(0).get("cnrextra4") != null) {
				String livelloUtente = utente.get(0).get("cnrextra4").toString();
				log.info("OK: L'UTENTE {} IN LDAP FIGURA COME DIPENDENTE ed ha come livello {} ", username, livelloUtente);
			} else {
				log.info("OK: L'UTENTE {} IN LDAP FIGURA COME NON DIPENDENTE ", username );				
				results.add("WARNING: L'UTENTE "+ username + " IN LDAP FIGURA COME NON DIPENDENTE " );
			}
			try {
				direttoreACE = aceService.bossDirettoreByUsername(username);
				log.info("OK: L'UTENTE {} ha direttoreACE {} ", username, direttoreACE.getUsername());
				results.add("OK: L'UTENTE "+ username + " ha responsabileSede " +  direttoreACE.getUsername());
			} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
				log.info("ERROR: L'UTENTE {} NON direttoreACE ", username);
				results.add("ERROR: L'UTENTE "+ username + " NON ha responsabileSede "  + " - con errore: " + error1.getMessage());
			} finally {
				try {
					direttore0 = aceService.bossLevelByUsername(0, username);
					log.info("OK: L'UTENTE {} ha direttore 0 {} ", username, direttore0.getUsername());
					results.add("OK: L'UTENTE "+ username + " ha responsabileSede " +  direttore0.getUsername());
				} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
					log.info("ERROR: L'UTENTE {} NON ha direttore 0 ", username);
					results.add("ERROR: L'UTENTE "+ username + " NON ha responsabileSede " + " - con errore: " + error1.getMessage() );
				} finally {
					try {
						direttore1 = aceService.bossLevelByUsername(1, username);
						log.info("OK: L'UTENTE {} ha direttore 1 {} ", username, direttore1.getUsername());
						results.add("OK: L'UTENTE "+ username + " ha responsabileSede " +  direttore1.getUsername());
					} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
						log.info("WARNING: L'UTENTE {} NON ha direttore 1 ", username);
						results.add("ERROR: L'UTENTE "+ username + " NON ha responsabileSede " + " - con errore: " + error1.getMessage() );
					} finally {
						try {
							direttore2 = aceService.bossLevelByUsername(0, username);
							log.info("OK: L'UTENTE {} ha direttore 2 {} ", username, direttore2.getUsername());
							results.add("OK: L'UTENTE "+ username + " ha responsabileSede " +  direttore2.getUsername());
						} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
							if (error1.getMessage().contains("Responsabile Sede not found") ) {
								log.info("WARNING: L'UTENTE {} NON ha direttore 2  ", username);
								results.add("WARNING: L'UTENTE "+ username + " NON ha responsabileSede " + " - con errore: " + error1.getMessage() );
							} else {
								log.info("ERROR: L'UTENTE {} NON ha direttore 2  ", username);
								results.add("ERROR: L'UTENTE "+ username + " NON ha responsabileSede " + " - con errore: " + error1.getMessage() );
							}
						} finally {
							try {
								bossSede = aceService.bossSedeByUsername(username);
								log.info("OK: L'UTENTE {} ha boss Sede {} ", username, bossSede.getUsername());
								results.add("OK: L'UTENTE "+ username + " ha responsabileSede " +  bossSede.getUsername());
							} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
								log.info("WARNING: L'UTENTE {} NON ha boss Sede ", username);
								results.add("ERROR: L'UTENTE "+ username + " NON ha bossSede " + " - con errore: " + error1.getMessage() );
							} finally {
								try {
									responsabileSede = aceService.bossSedeResponsabileByUsername(username);
									log.info("OK: L'UTENTE {} ha responsabileSede {} ", username, responsabileSede.getUsername());
									results.add("OK: L'UTENTE "+ username + " ha responsabileSede " +  responsabileSede.getUsername());
								} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
									log.info("WARNING: L'UTENTE {} NON ha responsabileSede ", username);
									results.add("WARNING: L'UTENTE "+ username + " NON ha responsabileSede " + " - con errore: " + error1.getMessage() );
								} finally {
									results.add(" ------------------------------------------------------------------------------------------------ ");
								}
							}
						}
					}
				}
			}
		} else {
			log.info("ERROR: L'UTENTE {} NON direttoreACE ", username);
			results.add("ERROR: L'UTENTE "+ username + " NON E' PRESENTE IN LDAP");
		}

	}

	private Map<String, String> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/utentiGenericiTest.csv"));
		//Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/utentiDomandeAccordiBilaterali1.csv"));
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





















