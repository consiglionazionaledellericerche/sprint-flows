package it.cnr.si.flows.ng.listeners.cnr.covid19;


import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.enums.TipoAppartenenza;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.scritture.UtenteDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
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
public class StartCovid19SetGroupsAndVisibility_v1 {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartCovid19SetGroupsAndVisibility_v1.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceService aceService;

	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());		
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
		//Integer cdsuoAppartenenzaResponsabile = aceBridgeService.getEntitaOrganizzativaDellUtente(proponente.toString()).getId();
		//String cdsuoAppartenenzaResponsabile = null;
		String idnsipAppartenenzaResponsabile = null;
		String cdsuoAppartenenzaResponsabile = null;
		SimpleUtenteWebDto direttoreAce = null;
		Integer IdEntitaOrganizzativaDirettore = 0;
		String denominazioneEO  = null;
		SimpleEntitaOrganizzativaWebDto entitaOrganizzativaDirettore = null;
		LocalDate dateRif = LocalDate.now();
		LocalDate dataUltimoGiornoServizio = LocalDate.now();
		BossDto responsabileStruttura = null;
		String idSedeUtenteRichiedente = null;

		// VERIFICA DIRETTORE
		//VERIFICA DIPENDENTI CESSATI
		if (aceService.getPersonaByUsername(initiator.toString()).getDataCessazione() != null) {			
			dateRif = LocalDate.of(Integer.parseInt(execution.getVariable("anno").toString()), Integer.parseInt(execution.getVariable("meseNumerico").toString()), 1);
			dataUltimoGiornoServizio = aceService.getPersonaByUsername(initiator.toString()).getDataCessazione().minusDays(1);
			if (dataUltimoGiornoServizio.isBefore(dateRif)) {
				throw new BpmnError("416", "l'utenza: " + initiator + " non risulta associata <br>ad alcuna struttura per il periodo di riferimento<br> "+ execution.getVariable("mese").toString() + " - " + execution.getVariable("anno").toString());
			} 	
		}
		try {
			idSedeUtenteRichiedente = aceService.getPersonaByUsername(initiator.toString()).getSede().getIdnsip();
			//direttoreAce = aceService.bossFirmatarioByUsername(initiator, dateRif);
			responsabileStruttura = aceService.findResponsabileStruttura(initiator, dateRif, TipoAppartenenza.SEDE, "responsabile-struttura");


		} catch ( FeignException  e) {
			if ((e.getMessage().indexOf("PERSONA_ASSEGNATA_SEDE_ESTERNA") >= 0)  && execution.getVariable("tipoAttivita").toString().equals("rendicontazione") ) {
				dateRif = LocalDate.of(Integer.parseInt(execution.getVariable("anno").toString()), Integer.parseInt(execution.getVariable("meseNumerico").toString()), 1);
				responsabileStruttura = aceService.findResponsabileStruttura(initiator, dateRif, TipoAppartenenza.SEDE, "responsabile-struttura");
			} else {
				throw new BpmnError("412", "Errore nell'avvio del flusso " + " <br>Si prega di contattare l'help desk in merito<br>");
			}
		}
		if (responsabileStruttura.getUtente()== null) {
			throw new BpmnError("412", "Non risulta alcun Direttore / Dirigente associato all'utenza: " + initiator + " <br>Si prega di contattare l'help desk in merito<br>");
		} else {
			direttoreAce = responsabileStruttura.getUtente();
		}
		if (responsabileStruttura.getEntitaOrganizzativa().getId()== null) {
			throw new BpmnError("412", "l'utenza: " + initiator + " non risulta associata ad alcuna struttura<br>");
		} else {
			IdEntitaOrganizzativaDirettore = responsabileStruttura.getEntitaOrganizzativa().getId();
			entitaOrganizzativaDirettore = aceService.entitaOrganizzativaById(IdEntitaOrganizzativaDirettore);
			cdsuoAppartenenzaResponsabile = entitaOrganizzativaDirettore.getCdsuo();
			idnsipAppartenenzaResponsabile = entitaOrganizzativaDirettore.getIdnsip();	
		}
		LOGGER.info("L'utente {} ha  {} come responsabile-struttura [{}] per la struttura {} ({}} - id:{}", initiator.toString(), direttoreAce.getUsername(), responsabileStruttura.getRuolo().getDescr(), entitaOrganizzativaDirettore.getDenominazione(), entitaOrganizzativaDirettore.getSigla(), IdEntitaOrganizzativaDirettore);

		String gruppoResponsabileProponente = "responsabile-struttura@" + IdEntitaOrganizzativaDirettore;

		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";


		//EntitaOrganizzativaWebDto utenteAce = aceBridgeService.getAfferenzaUtentePerSede(execution.getVariable("initiator").toString());
		SimpleUtenteWebDto utente = aceService.getUtente(execution.getVariable("initiator").toString());

		execution.setVariable("matricola", utente.getPersona().getMatricola());
		execution.setVariable("nomeCognomeUtente", utente.getPersona().getNome() + " " + utente.getPersona().getCognome());
		execution.setVariable("userNameUtente", utente.getUsername());
		execution.setVariable("tipoContratto", utente.getPersona().getTipoContratto());
		execution.setVariable("cds", cdsuoAppartenenzaResponsabile);
		execution.setVariable("idnsip", idnsipAppartenenzaResponsabile);
		execution.setVariable("direttore", direttoreAce.getPersona().getNome() + " " +  direttoreAce.getPersona().getCognome());
		execution.setVariable("denominazioneEO", denominazioneEO);
		execution.setVariable("idSedeUtenteRichiedente", idSedeUtenteRichiedente);


		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileProponente, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);


		execution.setVariable("gruppoResponsabileProponente", gruppoResponsabileProponente);
		execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		execution.setVariable("idStruttura", String.valueOf(IdEntitaOrganizzativaDirettore));

	}
}
