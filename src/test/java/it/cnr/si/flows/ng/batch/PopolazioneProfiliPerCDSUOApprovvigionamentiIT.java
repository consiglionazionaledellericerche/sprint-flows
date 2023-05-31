package it.cnr.si.flows.ng.batch;


import com.opencsv.CSVParser;
import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils.associazioneRuoloPersonaCDSUO;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.RelationshipService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;

import org.junit.Ignore;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "dev,cnr,keycloak")
@RunWith(SpringJUnit4ClassRunner.class)
public class PopolazioneProfiliPerCDSUOApprovvigionamentiIT {

	private static final Logger log = LoggerFactory.getLogger(PopolazioneProfiliPerCDSUOApprovvigionamentiIT.class);

	@Inject
	private AceService aceService;

	@Inject
	private RelationshipService relationshipService;
	@Inject
	private MembershipService membershipService;
	private final Map<String, String> errors = new HashMap<>();

	int i = 0;

	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {
		//String[][] persone = getPersoneDaFile();

		Map<Integer, associazioneRuoloPersonaCDSUO> persone = new HashMap<Integer, associazioneRuoloPersonaCDSUO>();
		persone = getPersoneDaFile();


		for (int i = 0; i < persone.size(); i++) {
			inserisciRuolo(persone.get(i).getPersona(), persone.get(i).getRuolo(), persone.get(i).getCdsuo(),persone.get(i).getData());
			// fruit is an element of the `fruits` array.
		}

		errors.forEach( (tripla, risultato) -> {
			log.error(tripla +": "+ risultato);
		});
	}

	private void inserisciRuolo(String username, String siglaRuolo, String NR_CDSUO, String data) {

		siglaRuolo = "staffApprovvigionamenti";
		username = "domenica.cava";
		NR_CDSUO = "113000";
		Integer idRuolo = aceService.getRuoloBySigla(siglaRuolo).getId();
		Integer idPersona = aceService.getPersonaByUsername(username).getId();

		List<SimpleEntitaOrganizzativaWebDto> listaSedi = aceService.entitaOrganizzativaFind(null,NR_CDSUO,java.time.LocalDate.now(),null);
		//List<SimpleEntitaOrganizzativaWebDto> listaSedi = aceService.entitaOrganizzativaFindByTerm)(NR_CDSUO);
		//List<SimpleEntitaOrganizzativaWebDto> listaSedi = aceService.entitaOrganizzativaFind(0,"113000",java.time.LocalDate.now(),23);
		String nrSede;

		for (int i = 0; i < listaSedi.size(); i++) {
			nrSede = listaSedi.get(i).getIdnsip();
			System.out.println(nrSede);
			if(nrSede != null && listaSedi.get(i).getCdsuo().equals(NR_CDSUO) ) {
				if(aceService.getSedeIdByIdNsip(nrSede) != null) {

					Integer idSede = Integer.valueOf(aceService.getSedeIdByIdNsip(nrSede).toString());

					System.out.println("idRuolo: " + idRuolo);
					System.out.println("idPersona: " + idPersona);
					System.out.println("idSede: " + idSede);	

					System.out.println("Associato ruolo " + idRuolo + "idPersona " + idPersona + " idSede " + idSede);
					aceService.associaRuoloPersona(idRuolo, idPersona, idSede, java.time.LocalDate.now(),java.time.LocalDate.parse("2025-10-23"),false,false,"","");

				}
			}
		}


	}

	private Map<Integer, associazioneRuoloPersonaCDSUO> getPersoneDaFile() throws IOException {

		CSVParser parser = new CSVParser(',');

		//		INSERIMENTO FILE CON NOMINATIVI		
		//		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/singoloGruppoUtentiProceduraAcquisti.csv"));
		//		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/ProceduraAcquisti-Utenti-ICCOM.csv"));
		//		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/ProceduraAcquisti-Utenti-SISINFO.csv"));
		Stream<String> lines = Files.lines(Paths.get("./src/batch/resources/batch/Approvvigionamenti-IT-Utenti.csv"));


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
				asso.setData(values[3]);
				associazioni.putIfAbsent(i, asso) ;
				i=i+1;

			} catch (IOException e) {e.printStackTrace();}
		});

		return associazioni;
	}
}
