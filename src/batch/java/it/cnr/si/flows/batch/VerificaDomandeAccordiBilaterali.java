package it.cnr.si.flows.batch;

import com.opencsv.CSVParser;
import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.enums.Carattere;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;

import javax.inject.Inject;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
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
	List <String> results = new ArrayList<>();

	//@Test questa riga non va mai messa su git
	@Test
	public void runBatch() throws IOException {
		Map<String, String> persone = getPersoneDaFile();

		results.add("---------- LISTA RISULTATI -----------------");


		persone.forEach( (username, siglaRuolo) -> {
			log.info("****** VERIFICA DIRETTORE PER UTENTE {}  nr.{} di:{} totali ******", username, personNr, persone.size());
			personNr = personNr + 1;
			results.add("----------utente nr "+personNr +"/" + persone.size()+ ": L'UTENTE " + username + " NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ");
			verificaDirettore(username, siglaRuolo);
		});

		errors.forEach( (tripla, risultato) -> {
			log.error(tripla +": "+ risultato);
		});


		//RPINT RISULTATI
		results.forEach(result -> {
			log.info(result);
		});
	}

	private void verificaDirettore(String username, String siglaRuolo) {
		EntitaOrganizzativaWebDto entitaOrganizzativaResponsabileStruttura = null;

		String richiedente = username;
		// log.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		//Integer cdsuoAppartenenzaUtente = aceBridgeService.getEntitaOrganizzativaDellUtente(richiedente.toString()).getId();
		String cdsuoAppartenenzaUtente = null;
		String idnsipAppartenenzaUtente = null;
		String denominazioneAppartenenzaUtente = null;
		String siglaAppartenenzaUtente = null;
		BossDto responsabileStrutturaRichiedente = new BossDto();
		Integer idEntitaResponsabileStrutturaRichiedente = null;
		String userNameResponsabileStrutturaRichiedente  = null;
		Carattere carattereEntitaOrganizzativa = null;
		int IdEntitaOrganizzativa = 0;
		boolean afferenzaACE = true;
		boolean bossACE = true;


		try {
			EntitaOrganizzativaWebDto entitaOrganizzativaUtente = aceBridgeService.getAfferenzaUtenteTipoSede(richiedente.toString());
			cdsuoAppartenenzaUtente = entitaOrganizzativaUtente.getCdsuo();
			idnsipAppartenenzaUtente = entitaOrganizzativaUtente.getIdnsip();
			denominazioneAppartenenzaUtente = entitaOrganizzativaUtente.getDenominazione();
			siglaAppartenenzaUtente = entitaOrganizzativaUtente.getSigla();
			carattereEntitaOrganizzativa = entitaOrganizzativaUtente.getCarattere();
			IdEntitaOrganizzativa = entitaOrganizzativaUtente.getId();
		} catch(UnexpectedResultException | FeignException e) {
			afferenzaACE = false;
			Map<String, Object> afferenzaUtente = siperService.getCDSUOAfferenzaUtente(richiedente.toString());
			cdsuoAppartenenzaUtente = afferenzaUtente.get("codice_uo").toString();
			idnsipAppartenenzaUtente = afferenzaUtente.get("codice_sede").toString();
			denominazioneAppartenenzaUtente = afferenzaUtente.get("struttura_appartenenza").toString();
			siglaAppartenenzaUtente = afferenzaUtente.get("sigla_sede").toString();
			results.add("WARNING: L'UTENTE " + username + " NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ");
			List<EntitaOrganizzativaWebDto> listaEntità = aceService.entitaOrganizzativaFindByTerm(cdsuoAppartenenzaUtente);
			if (listaEntità.size() == 1) {
				carattereEntitaOrganizzativa = listaEntità.get(0).getCarattere();
				IdEntitaOrganizzativa = listaEntità.get(0).getId();
			} else {
				results.add("WARNING: L'UTENTE " + username + " NON esiste in anagrafica ACE e SIPER ATTRIBUISCE un cdsuo " + cdsuoAppartenenzaUtente + " CHE NON CORRISPONDE IN ACE !!!!!!!!!!!!!!! ");
			}
		}
		finally {

			if (carattereEntitaOrganizzativa != null) {
				if (carattereEntitaOrganizzativa.equals(it.cnr.si.service.dto.anagrafica.enums.Carattere.RICERCA)) {
					try {
						responsabileStrutturaRichiedente = aceService.bossDirettoreByUsername(richiedente.toString());
					} catch(UnexpectedResultException | FeignException e) {
						bossACE = false;
						results.add("ERROR: L'UTENTE " + username + " fallisce per aceService.bossDirettoreByUsername " );
						responsabileStrutturaRichiedente.setIdEntitaOrganizzativa(IdEntitaOrganizzativa);
						responsabileStrutturaRichiedente.setUsername(siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString());
						responsabileStrutturaRichiedente.setSiglaEO(siglaAppartenenzaUtente);
						responsabileStrutturaRichiedente.setDenominazioneEO(siglaAppartenenzaUtente);
						Set<String> listaMembriResponsabileStruttura = membershipService.getAllUsersInGroup("responsabile-struttura@" + IdEntitaOrganizzativa);
						if (listaMembriResponsabileStruttura.size() < 1 && !listaMembriResponsabileStruttura.contains(responsabileStrutturaRichiedente.getUsername())) {
							log.info("responsabileStrutturaRichiedente {} trovato in SIPER non trova corrispondenza con quello in ACE]", responsabileStrutturaRichiedente.getUsername());
							results.add("WARNING: IL DIRETTORE " + responsabileStrutturaRichiedente + "DELL'UTENTE " + username + " trovato in SIPER non trova corrispondenza con quello in ACE ");
							throw new IllegalStateException();
						}
					}
					finally {
						log.info("responsabileStrutturaRichiedente {} ", responsabileStrutturaRichiedente.getUsername());
					}
				} else {
					try {
						responsabileStrutturaRichiedente = aceService.bossSedeResponsabileByUsername(richiedente.toString());
					} catch(UnexpectedResultException | FeignException e) {
						bossACE = false;
						results.add("WARNING: IL DIRETTORE DELL'UTENTE " + username + " NON SI RICAVA IN ACE CON BOSS!!!!!!!!!!!!!!! ");
						responsabileStrutturaRichiedente.setIdEntitaOrganizzativa(IdEntitaOrganizzativa);
						responsabileStrutturaRichiedente.setUsername(siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString());
						responsabileStrutturaRichiedente.setSiglaEO(siglaAppartenenzaUtente);
						responsabileStrutturaRichiedente.setDenominazioneEO(siglaAppartenenzaUtente);
						Set<String> listaMembriResponsabileStruttura = membershipService.getAllUsersInGroup("responsabile-struttura@" + IdEntitaOrganizzativa);
						if (listaMembriResponsabileStruttura.size() < 1 && !listaMembriResponsabileStruttura.contains(responsabileStrutturaRichiedente.getUsername())) {
							log.info("responsabileStrutturaRichiedente {} trovato in SIPER non trova corrispondenza con quello in ACE]", responsabileStrutturaRichiedente.getUsername());
							results.add("WARNING: IL DIRETTORE " + responsabileStrutturaRichiedente + "DELL'UTENTE " + username + " trovato in SIPER non trova corrispondenza con quello in ACE ");
							throw new IllegalStateException();
						}
					}				
					finally {
						log.info("responsabileStrutturaRichiedente {} ]", responsabileStrutturaRichiedente.getUsername());
					}

				}
				if (responsabileStrutturaRichiedente != null) {
					idEntitaResponsabileStrutturaRichiedente = responsabileStrutturaRichiedente.getIdEntitaOrganizzativa();
					userNameResponsabileStrutturaRichiedente = responsabileStrutturaRichiedente.getUsername();
					String usernameDirettore = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
					if (!userNameResponsabileStrutturaRichiedente.equals(usernameDirettore)) {
						log.info("responsabileStrutturaRichiedente {} trovato in SIPER non trova corrispondenza con quello in ACE]", responsabileStrutturaRichiedente.getUsername());
						results.add("WARNING: IL DIRETTORE " + responsabileStrutturaRichiedente + "DELL'UTENTE " + username + " trovato in SIPER non trova corrispondenza con quello in ACE: " + responsabileStrutturaRichiedente.getUsername());
						throw new IllegalStateException();
					}
				}
				String siglaEntitaorganizzativaResponsabileUtente = responsabileStrutturaRichiedente.getSiglaEO();
				String denominazioneEntitaorganizzativaResponsabileUtente = responsabileStrutturaRichiedente.getDenominazioneEO();
				String cdsuoEntitaorganizzativaResponsabileUtente = aceService.entitaOrganizzativaById(idEntitaResponsabileStrutturaRichiedente).getCdsuo();
				String idnsipEntitaorganizzativaResponsabileUtente = aceService.entitaOrganizzativaById(idEntitaResponsabileStrutturaRichiedente).getIdnsip();			
				log.info("L'utente {} ha come direttore {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", richiedente.toString(), responsabileStrutturaRichiedente.getUsername(), denominazioneEntitaorganizzativaResponsabileUtente, siglaEntitaorganizzativaResponsabileUtente, idEntitaResponsabileStrutturaRichiedente, cdsuoEntitaorganizzativaResponsabileUtente, idnsipEntitaorganizzativaResponsabileUtente);
				String gruppoDirigenteRichiedente = "responsabile-struttura@" + idEntitaResponsabileStrutturaRichiedente;
				results.add("OK: L'UTENTE " + username + " ha come direttore " + responsabileStrutturaRichiedente.getUsername() + " -- " + denominazioneEntitaorganizzativaResponsabileUtente + "[ID: " + idEntitaResponsabileStrutturaRichiedente +  "] [CDSUO: " + cdsuoEntitaorganizzativaResponsabileUtente +" ] [IDNSIP: " + idnsipEntitaorganizzativaResponsabileUtente  + "]");


			}
		}
	}

	private Map<String, String> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		//Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/utentiDomandeAccordiBilaterali5.csv"));
		//Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/utentiDomandeAccordiBilaterali5.csv"));
		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/utentiGenericiTest.csv"));

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





















