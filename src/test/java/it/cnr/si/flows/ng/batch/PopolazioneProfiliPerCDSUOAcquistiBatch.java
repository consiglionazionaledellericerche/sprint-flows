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
import java.util.Set;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.CompletionStageReturnValueHandler;

import com.opencsv.CSVParser;

import feign.FeignException;
import it.cnr.si.SprintApp;
import it.cnr.si.config.Constants;
import it.cnr.si.config.DefaultProfileUtil;
import it.cnr.si.config.JHipsterProperties;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Utils.associazioneRuoloPersonaCDSUO;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.base.PageDto;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class PopolazioneProfiliPerCDSUOAcquistiBatch {

	private static final Logger log = LoggerFactory.getLogger(PopolazioneProfiliPerCDSUOAcquistiBatch.class);

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

	int i = 0;

	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {
		//String[][] persone = getPersoneDaFile();
		
		Map<Integer, associazioneRuoloPersonaCDSUO> persone = new HashMap<Integer, associazioneRuoloPersonaCDSUO>();
		persone = getPersoneDaFile();
		
		
		for (int i = 0; i < persone.size(); i++) {
			inserisciRuolo(persone.get(i).getPersona(), persone.get(i).getRuolo(), persone.get(i).getCdsuo());
		    // fruit is an element of the `fruits` array.
		}
		



		errors.forEach( (tripla, risultato) -> {
			log.error(tripla +": "+ risultato);
		});
	}

	private void inserisciRuolo(String username, String siglaRuolo, String idCDSUO) {

		//EntitaOrganizzativaWebDto afferenzaUtente = aceBridgeService.getAfferenzaUtente(username);
		//String cdsuo = afferenzaUtente.getCdsuo();
		
		log.info("****** UTENTE {} ******", username);

		String cdsuoAppartenenzaUtente = null;
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtente(username).getCdsuo();
		} catch(UnexpectedResultException | FeignException | HttpClientErrorException error1) {
			log.info("WARNING: L'UTENTE {} NON esiste in anagrafica ACE !!!!!!!!!!!!!!! ", username);
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(username).get("codice_uo").toString();
		}
		finally {
			if (cdsuoAppartenenzaUtente.equalsIgnoreCase(idCDSUO)) {
				log.info("OK -- ID CDSUO CORRISPONDE  {}",idCDSUO);
			} else
			{
				log.info("ERROR -- ID CDSUO  trovato {} non CORRISPONDE a quello indicato  {}",cdsuoAppartenenzaUtente, idCDSUO);
			}
			Object insdipResponsabileUo = new Object();
			String usernameResponsabileUO = null;
			EntitaOrganizzativaWebDto entitaOrganizzativaRespUo = null;
			try {
				insdipResponsabileUo = siperService.getResponsabileCDSUO(cdsuoAppartenenzaUtente).get(0).get("codice_sede");
				log.info("getResponsabileCDSUO  FUNZIONA ");
				usernameResponsabileUO = siperService.getResponsabileCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
				try {
					entitaOrganizzativaRespUo = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0);
				} catch(UnexpectedResultException | FeignException | HttpClientErrorException error3) {
					log.info("-------------- WARNING: entitaOrganizzativaRespUo  NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la CDSUO {}", cdsuoAppartenenzaUtente);
					try {
						entitaOrganizzativaRespUo = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0);
					} catch(UnexpectedResultException | FeignException | HttpClientErrorException error4) {
						log.info("-------------- ERROR: entitaOrganizzativaRespUo  2o TENTATIVO NON RIESCO A TROVARE L'ENTITA' ORGANIZZATIVA per la CDSUO {}", cdsuoAppartenenzaUtente);
					}
				}
				finally {

					Integer idEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getId();
					String siglaEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getSigla().toString();
					String denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getDenominazione().toString();
					String cdsuoEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getCdsuo().toString();
					String idnsipEntitaorganizzativaResponsabileUtente = entitaOrganizzativaRespUo.getIdnsip().toString();
					log.info("OK: L'utente {} ha come responsabile {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", username, usernameResponsabileUO, denominazioneEntitaorganizzativaResponsabileUtente, siglaEntitaorganizzativaResponsabileUtente, idEntitaorganizzativaResponsabileUtente, cdsuoEntitaorganizzativaResponsabileUtente, idnsipEntitaorganizzativaResponsabileUtente);
					String gruppoDirigenteRichiedente = "responsabileFirmaAcquisti@" + idEntitaorganizzativaResponsabileUtente;

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
					
					Integer idRuolo = aceService.getRuoloBySigla(siglaRuolo).getId();
					Integer idPersona = aceService.getPersonaByUsername(username).getId();

					try {
						log.info("Associato ruolo {} persona {} eo {}", idRuolo, idPersona, idEntitaorganizzativaResponsabileUtente);
						aceService.associaRuoloPersona(idRuolo, idPersona, idEntitaorganizzativaResponsabileUtente);
						errors.put(username + " "+ siglaRuolo + " "+ siglaEntitaorganizzativaResponsabileUtente + "("+ idEntitaorganizzativaResponsabileUtente +")", "OK");
					} catch (RuntimeException e) {
						if (e.getMessage().contains("Il Ruolo specificato e' gia' presente")) {
							errors.put(username + " "+ siglaRuolo + " "+ siglaEntitaorganizzativaResponsabileUtente + "("+ idEntitaorganizzativaResponsabileUtente +")", "PRESENTE");
						} else {
							log.error("Errore nella richiesta", e);
							errors.put(username + " "+ siglaRuolo + " "+ siglaEntitaorganizzativaResponsabileUtente + "("+ idEntitaorganizzativaResponsabileUtente +")", e.getMessage());	
						}
					}
					
				}	
			} catch(UnexpectedResultException | FeignException | HttpClientErrorException error2) {
				log.info("-------------- ERROR: getResponsabileCDSUO  NON HA FUNZIONATO!!!!!!!!!!!!!!! l'utente {} non ha RESPONSABILE UO per la CDSUO {}", username, cdsuoAppartenenzaUtente);
			}
			finally {
				log.info("-------------- NEXT");


			}
		}

//		List<EntitaOrganizzativaWebDto> eos = aceService.entitaOrganizzativaFind(null, null, cdsuo, LocalDate.now(), null).getItems();
//		PersonaWebDto persona = aceService.personaByUsername(username);
//
//		eos.forEach(eo -> {
//

//		});


	}

	private Map<Integer, associazioneRuoloPersonaCDSUO> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		Stream<String> lines = Files.lines(Paths.get("./src/test/resources/batch/singoloGruppoUtentiProceduraAcquisti.csv"));

		i = 0;
		
		Map<Integer, associazioneRuoloPersonaCDSUO> associazioni = new HashMap<Integer, associazioneRuoloPersonaCDSUO>();

		lines
				.skip(1).
				forEach(l -> {
					try {

						String[] values = parser.parseLine(l);
						log.info(values[0] + " " + values[1]);
						associazioneRuoloPersonaCDSUO asso = new associazioneRuoloPersonaCDSUO();
						asso.setPersona(values[0]);
						asso.setRuolo(values[1]);
						asso.setCdsuo(values[2]);
						associazioni.putIfAbsent(i, asso) ;
						i=i+1;

					} catch (IOException e) {e.printStackTrace();}
				});

		return associazioni;
	}
}





















