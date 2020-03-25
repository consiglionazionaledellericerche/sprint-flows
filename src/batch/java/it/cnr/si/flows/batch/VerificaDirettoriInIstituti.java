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
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.RuoloWebDto;
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
public class VerificaDirettoriInIstituti {

	private static final Logger log = LoggerFactory.getLogger(VerificaDirettoriInIstituti.class);

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
	EntitaOrganizzativaWebDto entitaOrganizzativaResponsabileStruttura = null;

	//@Test questa riga non va mai messa su git
	@Test
	public void runBatch() throws IOException {

		log.info("----------------- URL ACE"+ aceURL);
		int tipo = 1;
		List<EntitaOrganizzativaWebDto> elencoIstituti = aceBridgeService.getUoByTipo(tipo);
		elencoIstituti.forEach( istituto -> {
			log.info("******  {}", personNr);
			log.info("******  {} [{} - ({})]  ******", istituto.getDenominazione(), istituto.getSigla(), istituto.getId());
			personNr = personNr + 1;
			Integer idEo = istituto.getId();
			//21	Direttore	direttore
			Integer idDirettore = aceService.getRuoloBySigla("direttore").getId();
			//30	Direttore F.F.	direttoreFF
			Integer idDirettoreDFF = aceService.getRuoloBySigla("direttoreFF").getId();
			//33	DIRETTORE AD INTERIM	direttoreAI
			Integer idDirettoreAI = aceService.getRuoloBySigla("direttoreAI").getId();
			//35	DIRETTORE GENERALE F.F.	direttoreGeneraleFF
			Integer idDirettoreGeneraleFF = aceService.getRuoloBySigla("direttoreGeneraleFF").getId();

			Integer totaleDirettore = aceService.getUtentiInRuoloEo(idDirettore, idEo).size();
			Integer totaleDirettorDFF = aceService.getUtentiInRuoloEo(idDirettoreDFF, idEo).size();
			Integer totaleDirettoreAI = aceService.getUtentiInRuoloEo(idDirettoreAI, idEo).size();
			Integer totaleDirettoreGeneraleFF = aceService.getUtentiInRuoloEo(idDirettoreGeneraleFF, idEo).size();

			Integer totaleDirettori = totaleDirettore + totaleDirettorDFF + totaleDirettoreAI + totaleDirettoreGeneraleFF;

			if(totaleDirettori == 0 ) {
				log.info("****** ERRORE NON CI SONO DIRETTORI IN  {}", istituto.getSigla());
			}
			if(totaleDirettori > 1 ) {
				log.info("****** WRNING  CI {} SONO DIRETTORI IN  {}", totaleDirettori, istituto.getSigla());
			}
		});
	}
}























