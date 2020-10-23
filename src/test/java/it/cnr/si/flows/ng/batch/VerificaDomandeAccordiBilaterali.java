package it.cnr.si.flows.ng.batch;

import com.opencsv.CSVParser;
import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
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
import java.util.Set;
import java.util.stream.Stream;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore // TODO TUTTI I TESTS FUNZIONALI e DIPENDENTI DA ACE/SIPER/ALTRI SERVIZI DA SPOSTARE FUORI DA JUNIT
public class VerificaDomandeAccordiBilaterali {

	private static final Logger log = LoggerFactory.getLogger(VerificaDomandeAccordiBilaterali.class);

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


	private final Map<String, String> errors = new HashMap<>();
	int personNr = 1;

	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {
		Map<String, String> persone = getPersoneDaFile();

		persone.forEach( (username, siglaRuolo) -> {
			log.info("****** VERIFICA DIRETTORE PER UTENTE {}  nr.{} di:{} totali ******", username, personNr, persone.size());
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
			log.info("WARNING: L'UTENTE {} NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ", username);
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(username).get("codice_uo").toString();
		}
		finally {
			Object insdipResponsabileUo = new Object();
			String usernameDirettore = null;
			SimpleEntitaOrganizzativaWebDto entitaOrganizzativaDirUo = null;
			try {
				insdipResponsabileUo = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("codice_sede");
				log.info("getDirettoreCDSUO  FUNZIONA ");
				usernameDirettore = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
				try {
					entitaOrganizzativaDirUo = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0);
				} catch(UnexpectedResultException | FeignException | HttpClientErrorException error3) {
					log.info("-------------- WARNING: entitaOrganizzativaDirUo  NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la CDSUO {}", cdsuoAppartenenzaUtente);
					try {
						entitaOrganizzativaDirUo = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0);
					} catch(UnexpectedResultException | FeignException | HttpClientErrorException error4) {
						log.info("-------------- ERROR: entitaOrganizzativaDirUo  2o TENTATIVO NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la CDSUO {}", cdsuoAppartenenzaUtente);
					}
				}
				finally {

					Integer idEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getId();
					String siglaEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getSigla().toString();
					String denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getDenominazione().toString();
					String cdsuoEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getCdsuo().toString();
					String idnsipEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getIdnsip().toString();
					log.info("OK: L'utente {} ha come direttore {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", username, usernameDirettore, denominazioneEntitaorganizzativaResponsabileUtente, siglaEntitaorganizzativaResponsabileUtente, idEntitaorganizzativaResponsabileUtente, cdsuoEntitaorganizzativaResponsabileUtente, idnsipEntitaorganizzativaResponsabileUtente);
					String gruppoDirigenteRichiedente = "responsabile-struttura@" + idEntitaorganizzativaResponsabileUtente;

					Set<String> members = membershipService.getAllUsersInGroup(gruppoDirigenteRichiedente);
					//List<String> members = membershipService.findMembersInGroup(gruppoDirigenteRichiedente);
					if (members.size() == 0) {
						log.info(" ERROR: Il gruppo RESPONSABILE STRUTTURA: {} NON HA NESSUNO", gruppoDirigenteRichiedente);
					} 
					if (members.size() > 1) {
						log.info(" ERROR: Il gruppo RESPONSABILE STRUTTURA: {} HA PIU' MEMBRI", gruppoDirigenteRichiedente);
					} 
					members.forEach(member -> {
						log.info("L'utente {} fa parte del gruppo {} ", member.toString(), gruppoDirigenteRichiedente);
					});
				}	
			} catch(UnexpectedResultException | FeignException | HttpClientErrorException error2) {
				log.info("-------------- ERROR: getDirettoreCDSUO  NON HA FUNZIONATO!!!!!!!!!!!!!!! l'utente {} non ha DIRETTORE per la CDSUO {}", username, cdsuoAppartenenzaUtente);
			}
			finally {
				log.info("-------------- NEXT");


			}
		}
	}

	private Map<String, String> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		Stream<String> lines = Files.lines(Paths.get("./src/test/resources/batch/utentiDomandeAccordiBilaterali5.csv"));

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





















