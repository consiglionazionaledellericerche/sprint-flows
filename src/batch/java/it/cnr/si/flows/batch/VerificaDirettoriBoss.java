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
public class VerificaDirettoriBoss {

	private static final Logger log = LoggerFactory.getLogger(VerificaDirettoriBoss.class);

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
	//@Test
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
		BossDto direttoreACE = null;
		BossDto responsabileSede = null;
		String usernameDirettoreACE = "";
		String usernameDirettoreSIPER = "";
		String denominazioneEntitaOrganizzativaDirettoreAce = null;
		String denominazioneEntitaOrganizzativaDirettoreSIPER = null;
		String siglaEntitaOrganizzativaDirettoreACE = null;
		String siglaEntitaOrganizzativaDirettoreSIPER = null;
		String ruoloEntitaOrganizzativaDirettore = null;
		String cdsuoAppartenenzaUtente = null;
		Integer idEntitaOrganizzativaDirettoreACE = 0;


		try {
			//cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(username).getCdsuo();
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtenteTipoSede(username).getCdsuo();
		} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
			log.info("WARNING: L'UTENTE {} NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ", username);
			//cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(username).get("codice_uo").toString();
			results.add("WARNING: L'UTENTE " + username + " NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ");
		}
		finally {
			if(cdsuoAppartenenzaUtente != null) {
				Object insdipAppartenenzaUtente = new Object();
				//insdipAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(username).getIdnsip();
				log.info("-------------- getAfferenzaUtente {}", aceBridgeService.getAfferenzaUtente(username).getIdnsip());
				log.info("-------------- getSede {}", aceBridgeService.getAfferenzaUtenteTipoSede(username).getIdnsip());

				insdipAppartenenzaUtente = aceBridgeService.getAfferenzaUtenteTipoSede(username).getIdnsip();
				EntitaOrganizzativaWebDto entitaOrganizzativaUtente = null;
				try {
					entitaOrganizzativaUtente = aceService.entitaOrganizzativaFindByTerm(insdipAppartenenzaUtente.toString()).get(0);
				} catch(UnexpectedResultException | FeignException | HttpClientErrorException error3) {
					log.info("-------------- WARNING: entitaOrganizzativaUtente  NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la IDNSIP {}", insdipAppartenenzaUtente);
				}
				finally {
					try {
						usernameDirettoreSIPER = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
						denominazioneEntitaOrganizzativaDirettoreSIPER = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("struttura_appartenenza").toString();
						siglaEntitaOrganizzativaDirettoreSIPER = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("sigla_sede").toString();
					} catch(UnexpectedResultException | FeignException | HttpClientErrorException error6) {
						log.info("-------------- ERROR: usernameDirettoreSIPER SIPER  NON RIESCO A TROVARE il direttore per la CDSUO {}", cdsuoAppartenenzaUtente);
						results.add("ERROR: " + username + " --usernameDirettoreSIPER SIPER NON RIESCO A TROVARE il direttore per la CDSUO " + cdsuoAppartenenzaUtente + "IDSIP (" + insdipAppartenenzaUtente + ") - CDSUO (" + entitaOrganizzativaUtente + ")");
					}
					finally {
						if (usernameDirettoreSIPER != null) {

							try {
								direttoreACE = aceService.bossDirettoreByUsername(username);
								usernameDirettoreACE = direttoreACE.getUsername().toString();
								results.add("BOSS --- userNameDirettore ACE " + usernameDirettoreACE + " idEntitaOrganizzativa: [" + direttoreACE.getIdEntitaOrganizzativa() + "] - denominazioneEO: " + direttoreACE.getDenominazioneEO() + " (" + direttoreACE.getSiglaEO() +")");
								idEntitaOrganizzativaDirettoreACE = direttoreACE.getIdEntitaOrganizzativa();
								denominazioneEntitaOrganizzativaDirettoreAce = direttoreACE.getDenominazioneEO();
								siglaEntitaOrganizzativaDirettoreACE = direttoreACE.getSiglaEO();
								ruoloEntitaOrganizzativaDirettore = direttoreACE.getDescrRuolo();
							} catch(UnexpectedResultException | FeignException | HttpClientErrorException | IndexOutOfBoundsException error5) {
								results.add("ERROR ACE: " + username + " --usernameDirettore ACE NON RIESCO A TROVARE il direttore per l'utente " + username + " - con errore: " + error5.getMessage());
							}

							finally {
								if (direttoreACE != null) {
									// CHECK DIRETTORE
									String gruppoResponsabileStrutturaPadre = "responsabile-struttura@" + idEntitaOrganizzativaDirettoreACE;
									Set<String> membriResponsabileStruttura = membershipService.getAllUsersInGroup(gruppoResponsabileStrutturaPadre);
									log.info("nr membriResponsabileStruttura  {} per struttura {} ", membriResponsabileStruttura.size(), idEntitaOrganizzativaDirettoreACE);
									if (membriResponsabileStruttura.size() == 0){
										log.info("NO --- il ResponsabileStruttura della struttura ACE id:{} [{}] NON ESISTE ", idEntitaOrganizzativaDirettoreACE, denominazioneEntitaOrganizzativaDirettoreAce);
										results.add("NO --- il ResponsabileStruttura della struttura ACE id:" + idEntitaOrganizzativaDirettoreACE +" [" + denominazioneEntitaOrganizzativaDirettoreAce + "] NON ESISTE " );
									}
									if (membriResponsabileStruttura.size() > 1){
										results.add("NO --- ci sono " + membriResponsabileStruttura.size() + " direttori per la struttura ACE id: " + idEntitaOrganizzativaDirettoreACE +" [" + denominazioneEntitaOrganizzativaDirettoreAce + "] NON ESISTE " );
										log.info("NO --- ci sono {} direttori per la struttura ACE id: {} [{}] ", membriResponsabileStruttura.size(), idEntitaOrganizzativaDirettoreACE, denominazioneEntitaOrganizzativaDirettoreAce);
									}
									String direttore_SIPER = usernameDirettoreSIPER;
									String direttore_ACE = usernameDirettoreACE;
									String DenominazioneEO_SIPER = denominazioneEntitaOrganizzativaDirettoreSIPER;
									String DenominazioneEO_ACE = denominazioneEntitaOrganizzativaDirettoreAce;
									String siglaEO_SIPER = siglaEntitaOrganizzativaDirettoreSIPER;
									String siglaEO_ACE = siglaEntitaOrganizzativaDirettoreACE;
									Integer idEO_ACE = idEntitaOrganizzativaDirettoreACE;
									String ruoloEODirettore_ACE = ruoloEntitaOrganizzativaDirettore;
									membriResponsabileStruttura.forEach(responsabile -> {
										//CONFRONTO SIPER
										if(responsabile.toString().equals(direttore_SIPER)) {
											log.info("il direttore trovato in ACE nella struttura SIPER {} [{}] è {} CORRISPONDE al direttore trovato su SIPER {} ", siglaEO_SIPER, DenominazioneEO_SIPER, responsabile.toString(), direttore_SIPER);
											results.add("OK --- " + username + " -- il direttore della struttura SIPER [" + siglaEO_SIPER + " - " + DenominazioneEO_SIPER +"  [" + responsabile.toString()  +"] CORRISPONDE al direttore SIPER "+ direttore_SIPER + " --");

										} else {
											log.info("il direttore trovato in ACE nella struttura SIPER {} [{}] è {} NON CORRISPONDE al direttore trovato su SIPER {} ", siglaEO_SIPER, DenominazioneEO_SIPER, responsabile.toString(), direttore_SIPER);
											results.add("NO --- " + username + " -- il direttore della struttura SIPER [" + siglaEO_SIPER + " - " + DenominazioneEO_SIPER +"  [" + responsabile.toString()  +"] NON CORRISPONDE al direttore SIPER "+ direttore_SIPER + " --");
										}
										//CONFRONTO ACE
										if(responsabile.toString().equals(direttore_ACE)) {
											log.info("il direttore trovato in ACE nella struttura ACE id:{} {} [{}] è {} CORRISPONDE al direttore trovato su ACE BOSS {} ", siglaEO_ACE, idEO_ACE, DenominazioneEO_ACE, responsabile.toString(), direttore_ACE);
											results.add("OK --- " + username + " -- il direttore della struttura ACE id: " + idEO_ACE + " [" + siglaEO_ACE + " - " + DenominazioneEO_ACE +"  [" + responsabile.toString()  +"] CORRISPONDE al direttore ACE "+ direttore_ACE + " --");

										} else {
											log.info("il direttore trovato in ACE nella struttura ACE id:{}  {} [{}] è {} NON CORRISPONDE al direttore trovato su ACE BOSS {} ", siglaEO_ACE, idEO_ACE, DenominazioneEO_ACE, responsabile.toString(), direttore_ACE);
											results.add("NO --- " + username + " -- il direttore della struttura ACE id: " + idEO_ACE + " [" + siglaEO_ACE + " - " + DenominazioneEO_ACE +"  [" + responsabile.toString()  +"] NON CORRISPONDE al direttore ACE "+ direttore_ACE + " --");
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





















