package it.cnr.si.flows.ng.listeners.cnr.acquistiICT;


import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.enums.TipoAppartenenza;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import feign.FeignException;

import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("cnr")

@Service
public class StartAcquistiICTSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartAcquistiICTSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;

	@Inject
	private AceService aceService;


	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException, FeignException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		//String richiedente = execution.getVariable("userNameRichiedente", String.class);
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		//Integer cdsuoAppartenenzaUtente = aceBridgeService.getEntitaOrganizzativaDellUtente(richiedente.toString()).getId();
		String cdsuoAppartenenzaUtente = null;
		Integer IdEntitaOrganizzativaDirettore = 0;
		SimpleEntitaOrganizzativaWebDto entitaOrganizzativaDirettore = null;
		LocalDate dateRif = LocalDate.now();
		BossDto responsabileStruttura = null;
		String denominazioneEntitaorganizzativaResponsabileUtente = null;

		String userNameRichiedente = initiator;
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", userNameRichiedente, execution.getId(), execution.getVariable("title"));

		// VERIFICA RESPOSNABILE STRUTTURA AFFERENZA CDSUO
		//direttoreAce = aceService.bossFirmatarioByUsername(userNameRichiedente, dateRif);
		//412 Precondition Failed per i casi tipo    "message": "L'utente fabio.diloreto risulta assegnato ad una sede esterna (id: 34611) in data 2020-10-09",

		try {
			responsabileStruttura = aceService.findResponsabileStruttura(userNameRichiedente, dateRif, TipoAppartenenza.SEDE, "responsabile-struttura");
			if (responsabileStruttura.getUtente()== null) {
				throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + userNameRichiedente + " <br>Si prega di contattare l'help desk in merito<br>");
			} else {
			}
			if (responsabileStruttura.getEntitaOrganizzativa().getId()== null) {
				throw new BpmnError("412", "l'utenza: " + userNameRichiedente + " non risulta associata ad alcuna struttura<br>");
			} else {
				IdEntitaOrganizzativaDirettore = responsabileStruttura.getEntitaOrganizzativa().getId();
				entitaOrganizzativaDirettore = aceService.entitaOrganizzativaById(IdEntitaOrganizzativaDirettore);
				cdsuoAppartenenzaUtente = entitaOrganizzativaDirettore.getCdsuo();
				denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirettore.getDenominazione();
			}
		} catch (Exception  e) {
			throw new BpmnError("412", "Errore nell'avvio del flusso " + e.getMessage().toString());
		}

		LOGGER.info("L'utente {} ha come responabile-struttura [{}] {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", userNameRichiedente, responsabileStruttura.getRuolo().getDescr(), responsabileStruttura.getUtente().getUsername(), denominazioneEntitaorganizzativaResponsabileUtente, entitaOrganizzativaDirettore.getSigla(), entitaOrganizzativaDirettore.getId(), entitaOrganizzativaDirettore.getCdsuo(), entitaOrganizzativaDirettore.getIdnsip());

		
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";
		String gruppoResponsabileAcquisti = "responsabileAcquistiICT@" + IdEntitaOrganizzativaDirettore;
		String gruppoFirmatari = "responsabile-struttura@" + IdEntitaOrganizzativaDirettore;
		//String gruppoRUP = "gruppoRUP@" + IdEntitaOrganizzativaDirettore;
		if(execution.getVariable("rup") != null){
			SimplePersonaWebDto rupUser = aceService.getPersonaByUsername(execution.getVariable("rup").toString());
		}
		LOGGER.debug("Imposto i gruppi del flusso gruppoResponsabileAcquisti {}, gruppoFirmatari {}, gruppoRUP {}",  gruppoResponsabileAcquisti, gruppoFirmatari, execution.getVariable("rup").toString());

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAcquisti, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoFirmatari, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), execution.getVariable("rup").toString(), PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);

		execution.setVariable("gruppoResponsabileAcquisti", gruppoResponsabileAcquisti);
		execution.setVariable("gruppoFirmatari", gruppoFirmatari);
		execution.setVariable("cdsuoRichiedente", cdsuoAppartenenzaUtente);
		execution.setVariable("idStruttura", String.valueOf(IdEntitaOrganizzativaDirettore));

	}
}