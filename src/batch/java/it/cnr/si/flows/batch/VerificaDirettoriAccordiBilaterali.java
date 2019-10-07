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
public class VerificaDirettoriAccordiBilaterali {

	private static final Logger log = LoggerFactory.getLogger(VerificaDirettoriAccordiBilaterali.class);

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

	private final Map<String, String> errors = new HashMap<>();
	int personNr = 1;
	List <String> results = new ArrayList<>();
	EntitaOrganizzativaWebDto entitaOrganizzativaResponsabileStruttura = null;

	//@Test questa riga non va mai messa su git
	@Test
	public void runBatch() throws IOException {
		Map<String, String> persone = getPersoneDaFile();

		results.add("---------- LISTA RISULTATI -----------------");

		persone.forEach( (username, siglaRuolo) -> {
			log.info("****** VERIFICA DIRETTORE PER UTENTE {}  nr.{} di:{} totali ******", username, personNr, persone.size());
			personNr = personNr + 1;
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

		log.info("****** UTENTE {} ******", username);

		String cdsuoAppartenenzaUtente = null;
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(username).getCdsuo();
		} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
			log.info("WARNING: L'UTENTE {} NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ", username);
			//cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(username).get("codice_uo").toString();
			results.add("WARNING: L'UTENTE " + username + " NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ");
		}
		finally {
			if(cdsuoAppartenenzaUtente != null) {
				Object insdipAppartenenzaUtente = new Object();
				insdipAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(username).getIdnsip();
				String usernameDirettore = null;
				EntitaOrganizzativaWebDto entitaOrganizzativaUtente = null;
				try {
					entitaOrganizzativaUtente = aceService.entitaOrganizzativaFindByTerm(insdipAppartenenzaUtente.toString()).get(0);
				} catch(UnexpectedResultException | FeignException | HttpClientErrorException error3) {
					log.info("-------------- WARNING: entitaOrganizzativaUtente  NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la IDNSIP {}", insdipAppartenenzaUtente);
					try {
						entitaOrganizzativaUtente = aceService.entitaOrganizzativaFindByTerm(insdipAppartenenzaUtente.toString()).get(0);
					} catch(UnexpectedResultException | FeignException | HttpClientErrorException error4) {
						log.info("-------------- ERROR: entitaOrganizzativaUtente  2o TENTATIVO NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la IDNSIP {}", insdipAppartenenzaUtente);
					}
				}
				finally {
					try {
						usernameDirettore = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
					} catch(UnexpectedResultException | FeignException | HttpClientErrorException error6) {
						log.info("-------------- ERROR: usernameDirettore  NON RIESCO A TROVARE il direttore per la CDSUO {}", cdsuoAppartenenzaUtente);
						results.add("ERROR: " + username + " --usernameDirettore  NON RIESCO A TROVARE il direttore per la CDSUO " + cdsuoAppartenenzaUtente);

					}
					finally {
						if (usernameDirettore != null) {
							//	NEW
							List<GerarchiaWebDto> listaGerarchia = null;
							try {
								listaGerarchia = aceBridgeService.getParents(entitaOrganizzativaUtente.getId());
							} catch(UnexpectedResultException | FeignException | HttpClientErrorException | IndexOutOfBoundsException error5) {
								results.add("ERROR: " + username + " --la struttura " + entitaOrganizzativaUtente.getId() + " -- CDSUO " +entitaOrganizzativaUtente.getCdsuo()+ " -- IDNSIP " +entitaOrganizzativaUtente.getIdnsip() + " NON HA PARTENT");
							}
							finally {
								if (listaGerarchia != null) {

									log.info("-------------- listaGerarchia size {}", listaGerarchia.size());
									List<GerarchiaWebDto> gerarchiaResults = listaGerarchia.stream()
											.filter(gerarchiaSingola -> gerarchiaSingola.getTipo().getId() == 2 || gerarchiaSingola.getTipo().getId() == 1)
											.collect(Collectors.toList());
									if (gerarchiaResults.size() != 1) {
										throw new IllegalStateException();
									}
									GerarchiaWebDto gerarchia = gerarchiaResults.get(0);
									if (gerarchia.getTipo().getId() == 1) {
										entitaOrganizzativaResponsabileStruttura = gerarchia.getPadre();
									} else {
										entitaOrganizzativaResponsabileStruttura = entitaOrganizzativaUtente;
									}
									log.info("-------------- padre: id: {} [{}], Gerarchia tipo: {}  [{}]", entitaOrganizzativaResponsabileStruttura.getId(), entitaOrganizzativaResponsabileStruttura.getDenominazione(), gerarchia.getTipo().getId(), gerarchia.getTipo().getDescr());
									// CHECK DIRETTORE
									String nomeDirettoreSiper = usernameDirettore.toString();
									String gruppoResponsabileStrutturaPadre = "responsabile-struttura@" + entitaOrganizzativaResponsabileStruttura.getId();

									Set<String> membriResponsabileStrutturaPadre = membershipService.getAllUsersInGroup(gruppoResponsabileStrutturaPadre);
									log.info("nr membriResponsabileStrutturaPadre  {} per struttura {} ", membriResponsabileStrutturaPadre.size(), entitaOrganizzativaResponsabileStruttura.getId());
									if (membriResponsabileStrutturaPadre.size() == 0){
										log.info("NO --- il direttore della struttura padre {} [{}] NON ESISTE ", entitaOrganizzativaResponsabileStruttura.getId(), entitaOrganizzativaResponsabileStruttura.getDenominazione());
									}
									if (membriResponsabileStrutturaPadre.size() > 1){
										log.info("NO --- ci sono {} direttori per la struttura padre {} [{}] ", membriResponsabileStrutturaPadre.size(), entitaOrganizzativaResponsabileStruttura.getId(), entitaOrganizzativaResponsabileStruttura.getDenominazione());
									}
									membriResponsabileStrutturaPadre.forEach(responsabile -> {
										if(responsabile.toString().equals(nomeDirettoreSiper)) {
											log.info("il direttore trovanto in ACE nella struttura (padre) {} [{}] è {} CORRISPONDE al direttore trovato su SIPER {} ", entitaOrganizzativaResponsabileStruttura.getId(), entitaOrganizzativaResponsabileStruttura.getDenominazione(), responsabile.toString(), nomeDirettoreSiper);
											results.add("OK --- " + username + " -- il direttore della struttura padre [" + entitaOrganizzativaResponsabileStruttura.getId() + " - " + entitaOrganizzativaResponsabileStruttura.getDenominazione() +"  [" + responsabile.toString()  +"] CORRISPONDE al direttore SIPER "+ nomeDirettoreSiper + " --");

										} else {
											log.info("il direttore trovanto in ACE nella struttura padre {} [{}] è {} NON CORRISPONDE al direttore trovato su SIPER {} ", entitaOrganizzativaResponsabileStruttura.getId(), entitaOrganizzativaResponsabileStruttura.getDenominazione(), responsabile.toString(), nomeDirettoreSiper);
											results.add("NO --- " + username + " -- il direttore della struttura padre [" + entitaOrganizzativaResponsabileStruttura.getId() + " - " + entitaOrganizzativaResponsabileStruttura.getDenominazione() +"  [" + responsabile.toString()  +"] NON CORRISPONDE al direttore SIPER "+ nomeDirettoreSiper + " --");
										}
									});				

									//FINE - NEW

								}
							}
						}
					}
				}
			}
		}
	}

	private Map<String, String> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

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





















