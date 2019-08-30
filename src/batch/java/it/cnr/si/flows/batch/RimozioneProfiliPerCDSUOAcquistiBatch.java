package it.cnr.si.flows.batch;


import com.opencsv.CSVParser;
import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils.associazioneRuoloPersonaCDSUO;
import it.cnr.si.service.AceService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;

import org.junit.Test;
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
public class RimozioneProfiliPerCDSUOAcquistiBatch {

	private static final Logger log = LoggerFactory.getLogger(RimozioneProfiliPerCDSUOAcquistiBatch.class);

	@Inject
	private AceService aceService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private SiperService siperService;
	@Inject
	private RelationshipService relationshipService;
	private final Map<String, String> errors = new HashMap<>();

	int i = 0;
	List <String> results = new ArrayList<>();
	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {
		//String[][] persone = getPersoneDaFile();

		results.add("---------- LISTA RISULTATI -----------------");

		Map<Integer, associazioneRuoloPersonaCDSUO> persone = new HashMap<Integer, associazioneRuoloPersonaCDSUO>();
		persone = getPersoneDaFile();


		for (int i = 0; i < persone.size(); i++) {
			results.add("---------- LISTA Persone con ruolo '" +  persone.get(i).getRuolo() + "' per CDSUO:  " +  persone.get(i).getCdsuo() + "-----------------");
			inserisciRuolo(persone.get(i).getPersona(), persone.get(i).getRuolo(), persone.get(i).getCdsuo());
			// fruit is an element of the `fruits` array.
		}


		errors.forEach( (tripla, risultato) -> {
			log.error(tripla +": "+ risultato);
		});



		results.forEach(result -> {
			log.info(result);
		});


	}

	private void inserisciRuolo(String username, String siglaRuolo, String cdsuo) {

		//EntitaOrganizzativaWebDto afferenzaUtente = aceBridgeService.getAfferenzaUtente(username);
		//String cdsuo = afferenzaUtente.getCdsuo();

		log.info("****** UTENTE {} ******", username);

		//List<EntitaOrganizzativaWebDto> ListaIdCDSUO = aceBridgeService.getUoLike(cdsuo);
		List<EntitaOrganizzativaWebDto> ListaIdCDSUO = aceService.entitaOrganizzativaFind(null, null, cdsuo, LocalDate.now(), null)
				.getItems()
				.stream()
				//.filter(e -> Enum.TipiEOPerAutocomplete.contains(e.getTipo().getId()))
				//                .map(e -> Pair.of(e.getId(), e.getCdsuo() +" - "+ e.getDenominazione()))
				.collect(Collectors.toList());

		log.info(" INFO: Il gruppo CDSUO {} HA nr.{} strutture", cdsuo, ListaIdCDSUO.size());
		results.add("Il gruppo CDSUO [" + cdsuo  +"] ha nr. "+ ListaIdCDSUO.size() + " strutture");
		ListaIdCDSUO.forEach(idCDSUO -> {
			log.info("La Struttura {} [{} ] fa parte del gruppo CDSUO: {} ", idCDSUO.getDenominazione(), idCDSUO.getId(), cdsuo);
			String gruppoRuolo = siglaRuolo + "@" + idCDSUO.getId();
			Set<String> members = relationshipService.getAllUsersInGroup(gruppoRuolo);
			//List<String> members = membershipService.findMembersInGroup(gruppoRuolo);
			if (members.size() == 0) {
				log.info(" INFO: Il gruppo {} NON HA NESSUNO", gruppoRuolo);
			} 
			if (members.size() > 1) {
				log.info(" INFO: Il gruppo {} HA PIU' MEMBRI", gruppoRuolo);
			} 
			members.forEach(member -> {
				log.info("L'utente {} fa parte del gruppo {} ", member.toString(), gruppoRuolo);
				results.add("L'utente " + member.toString()  +" a parte del gruppo "+ gruppoRuolo + " [" + idCDSUO.getDenominazioneBreve() + " / " + idCDSUO.getSigla() + " / " + idCDSUO.getIdnsip() + "]");
				Integer idRuolo = aceService.getRuoloBySigla(siglaRuolo).getId();
				Integer idPersona = aceService.getPersonaByUsername(member.toString()).getId();
				log.info("idRuolo {} / idPersona {} ", idRuolo, idPersona);
			});

		});

		//		try {
		//			log.info("Associato ruolo {} persona {} eo {}", idRuolo, idPersona, idEntitaorganizzativaResponsabileUtente);
		//			aceService.associaRuoloPersona(idRuolo, idPersona, idEntitaorganizzativaResponsabileUtente);
		//			errors.put(username + " "+ siglaRuolo + " "+ siglaEntitaorganizzativaResponsabileUtente + "("+ idEntitaorganizzativaResponsabileUtente +")", "OK");
		//		} catch (RuntimeException e) {
		//			if (e.getMessage().contains("Il Ruolo specificato e' gia' presente")) {
		//				errors.put(username + " "+ siglaRuolo + " "+ siglaEntitaorganizzativaResponsabileUtente + "("+ idEntitaorganizzativaResponsabileUtente +")", "PRESENTE");
		//			} else {
		//				log.error("Errore nella richiesta", e);
		//				errors.put(username + " "+ siglaRuolo + " "+ siglaEntitaorganizzativaResponsabileUtente + "("+ idEntitaorganizzativaResponsabileUtente +")", e.getMessage());	
		//			}
		//		}




	}

	private Map<Integer, associazioneRuoloPersonaCDSUO> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		//		INSERIMENTO FILE CON NOMINATIVI		
		//		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/singoloGruppoUtentiProceduraAcquisti.csv"));
		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/ProceduraAcquisti-ListaRimozioneRuoliPerCDSUO.csv"));

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





















