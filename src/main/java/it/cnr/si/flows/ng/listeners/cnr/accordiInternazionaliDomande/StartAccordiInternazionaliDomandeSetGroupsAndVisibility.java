package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliDomande;


import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.dto.anagrafica.enums.Carattere;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.GerarchiaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.RuoloWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.DelegateExecution;
import org.h2.util.New;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import feign.FeignException;

import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
@Profile("!oiv")

@Service
public class StartAccordiInternazionaliDomandeSetGroupsAndVisibility {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartAccordiInternazionaliDomandeSetGroupsAndVisibility.class);

	@Inject
	private RuntimeService runtimeService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private AceService aceService;
	@Inject
	private SiperService siperService;
	@Inject
	private MembershipService membershipService;	


	public void configuraVariabiliStart(DelegateExecution execution)  throws IOException, ParseException  {
		EntitaOrganizzativaWebDto entitaOrganizzativaResponsabileStruttura = null;

		String initiator = (String) execution.getVariable(Enum.VariableEnum.initiator.name());
		String richiedente = execution.getVariable("userNameRichiedente", String.class);
		// LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable(Enum.VariableEnum.title.name()));
		LOGGER.info("L'utente {} sta avviando il flusso {} (con titolo {})", initiator, execution.getId(), execution.getVariable("title"));
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

		try {
			EntitaOrganizzativaWebDto entitaOrganizzativaUtente = aceBridgeService.getAfferenzaUtenteTipoSede(richiedente.toString());
			cdsuoAppartenenzaUtente = entitaOrganizzativaUtente.getCdsuo();
			idnsipAppartenenzaUtente = entitaOrganizzativaUtente.getIdnsip();
			denominazioneAppartenenzaUtente = entitaOrganizzativaUtente.getDenominazione();
			siglaAppartenenzaUtente = entitaOrganizzativaUtente.getSigla();
			carattereEntitaOrganizzativa = entitaOrganizzativaUtente.getCarattere();
			IdEntitaOrganizzativa = entitaOrganizzativaUtente.getId();
		} catch(UnexpectedResultException | FeignException e) {
			Map<String, Object> afferenzaUtente = siperService.getCDSUOAfferenzaUtente(richiedente.toString());
			cdsuoAppartenenzaUtente = afferenzaUtente.get("codice_uo").toString();
			idnsipAppartenenzaUtente = afferenzaUtente.get("codice_sede").toString();
			denominazioneAppartenenzaUtente = afferenzaUtente.get("struttura_appartenenza").toString();
			siglaAppartenenzaUtente = afferenzaUtente.get("sigla_sede").toString();

			List<EntitaOrganizzativaWebDto> listaEntità = aceService.entitaOrganizzativaFindByTerm(cdsuoAppartenenzaUtente);
			if (listaEntità.size() == 1) {
				carattereEntitaOrganizzativa = listaEntità.get(0).getCarattere();
				IdEntitaOrganizzativa = listaEntità.get(0).getId();
			}
		}
		finally {

			if (carattereEntitaOrganizzativa != null) {
				if (carattereEntitaOrganizzativa.equals(it.cnr.si.service.dto.anagrafica.enums.Carattere.RICERCA)) {
					responsabileStrutturaRichiedente = aceService.bossDirettoreByUsername(richiedente.toString());
				} else {
					try {
						responsabileStrutturaRichiedente = aceService.bossSedeResponsabileByUsername(richiedente.toString());
					} catch(UnexpectedResultException | FeignException e) {
							responsabileStrutturaRichiedente.setIdEntitaOrganizzativa(aceBridgeService.getAfferenzaUtenteTipoSede(richiedente.toString()).getId());
							responsabileStrutturaRichiedente.setUsername(siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString());
							responsabileStrutturaRichiedente.setSiglaEO(siglaAppartenenzaUtente);
							responsabileStrutturaRichiedente.setDenominazioneEO(siglaAppartenenzaUtente);
							Set<String> listaMembriResponsabileStruttura = membershipService.getAllUsersInGroup("responsabile-struttura@" + aceBridgeService.getAfferenzaUtenteTipoSede(richiedente.toString()).getId());
							if (listaMembriResponsabileStruttura.size() < 1 && !listaMembriResponsabileStruttura.contains(responsabileStrutturaRichiedente.getUsername())) {
								LOGGER.info("responsabileStrutturaRichiedente {} trovato in SIPER non trova corrispondenza con quello in ACE]", responsabileStrutturaRichiedente.getUsername());
								throw new IllegalStateException();
							}
						}				
					finally {
						LOGGER.info("responsabileStrutturaRichiedente {} ]", responsabileStrutturaRichiedente);
					}
				}
			}
			if (responsabileStrutturaRichiedente != null) {
				idEntitaResponsabileStrutturaRichiedente = responsabileStrutturaRichiedente.getIdEntitaOrganizzativa();
				userNameResponsabileStrutturaRichiedente = responsabileStrutturaRichiedente.getUsername();
				String usernameDirettore = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
				if (!userNameResponsabileStrutturaRichiedente.equals(usernameDirettore)) {
					LOGGER.info("responsabileStrutturaRichiedente {} trovato in SIPER non trova corrispondenza con quello in ACE]", responsabileStrutturaRichiedente.getUsername());
					throw new IllegalStateException();
				}
			}
		}

		String siglaEntitaorganizzativaResponsabileUtente = responsabileStrutturaRichiedente.getSiglaEO();
		String denominazioneEntitaorganizzativaResponsabileUtente = responsabileStrutturaRichiedente.getDenominazioneEO();
		String cdsuoEntitaorganizzativaResponsabileUtente = aceService.entitaOrganizzativaById(idEntitaResponsabileStrutturaRichiedente).getCdsuo();
		String idnsipEntitaorganizzativaResponsabileUtente = aceService.entitaOrganizzativaById(idEntitaResponsabileStrutturaRichiedente).getIdnsip();			
		LOGGER.info("L'utente {} ha come direttore {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", richiedente.toString(), responsabileStrutturaRichiedente, denominazioneEntitaorganizzativaResponsabileUtente, siglaEntitaorganizzativaResponsabileUtente, idEntitaResponsabileStrutturaRichiedente, cdsuoEntitaorganizzativaResponsabileUtente, idnsipEntitaorganizzativaResponsabileUtente);

		String gruppoValidatoriAccordiInternazionali = "validatoriAccordiInternazionali@0000";
		String gruppoUfficioProtocollo = "ufficioProtocolloAccordiInternazionali@0000";
		String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@0000";
		String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
		//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
		String gruppoDirigenteRichiedente = "responsabile-struttura@" + idEntitaResponsabileStrutturaRichiedente;

		String applicazioneAccordiInternazionali = "app.abil";
		String applicazioneScrivaniaDigitale = "app.scrivaniadigitale";

		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriAccordiInternazionali, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);
		LOGGER.debug("Imposto i gruppi del flusso {}, {}, {}",  gruppoValidatoriAccordiInternazionali, gruppoResponsabileAccordiInternazionali, gruppoUfficioProtocollo);

		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValidatoriAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoResponsabileAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneAccordiInternazionali, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoUfficioProtocollo, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoDirigenteRichiedente, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), gruppoValutatoreScientificoDipartimento, PROCESS_VISUALIZER);
		runtimeService.addGroupIdentityLink(execution.getProcessInstanceId(), applicazioneScrivaniaDigitale, PROCESS_VISUALIZER);

		execution.setVariable("strutturaValutazioneDirigente", idEntitaResponsabileStrutturaRichiedente + "-" + denominazioneEntitaorganizzativaResponsabileUtente);
		execution.setVariable("gruppoValidatoriAccordiInternazionali", gruppoValidatoriAccordiInternazionali);
		execution.setVariable("gruppoResponsabileAccordiInternazionali", gruppoResponsabileAccordiInternazionali);
		execution.setVariable("gruppoUfficioProtocollo", gruppoUfficioProtocollo);
		execution.setVariable("applicazioneAccordiInternazionali", applicazioneAccordiInternazionali);
		execution.setVariable("gruppoDirigenteRichiedente", gruppoDirigenteRichiedente);
		execution.setVariable("gruppoValutatoreScientificoDipartimento", gruppoValutatoreScientificoDipartimento);
		execution.setVariable("applicazioneScrivaniaDigitale", applicazioneScrivaniaDigitale);
		execution.setVariable("cdsuoRichiedente", cdsuoAppartenenzaUtente);
	}
}
