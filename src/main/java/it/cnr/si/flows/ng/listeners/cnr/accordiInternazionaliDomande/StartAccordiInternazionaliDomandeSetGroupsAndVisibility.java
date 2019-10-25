package it.cnr.si.flows.ng.listeners.cnr.accordiInternazionaliDomande;


import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.GerarchiaWebDto;

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
		try {
			cdsuoAppartenenzaUtente = aceBridgeService.getAfferenzaUtenteTipoSede(richiedente.toString()).getCdsuo();
		} catch(UnexpectedResultException | FeignException e) {
			cdsuoAppartenenzaUtente = siperService.getCDSUOAfferenzaUtente(richiedente.toString()).get("codice_uo").toString();
		}
		finally {
			LOGGER.debug("getDirettoreCDSUO  FUNZIONA ");
			Object insdipResponsabileUo = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("codice_sede");
			String usernameDirettore = siperService.getDirettoreCDSUO(cdsuoAppartenenzaUtente).get(0).get("uid").toString();
			EntitaOrganizzativaWebDto entitaOrganizzativaDirUo = aceService.entitaOrganizzativaFindByTerm(insdipResponsabileUo.toString()).get(0);

			//			NEW - codice per ricavare il padre della struttura
			List<GerarchiaWebDto> listaGerarchia = null;
			try {
				listaGerarchia = aceBridgeService.getParents(entitaOrganizzativaDirUo.getId());
			} catch(UnexpectedResultException | FeignException | HttpClientErrorException | IndexOutOfBoundsException error5) {
				LOGGER.debug("ERROR: " + richiedente.toString() + " --la struttura " + entitaOrganizzativaDirUo.getId() + " -- CDSUO " +entitaOrganizzativaDirUo.getCdsuo()+ " -- IDNSIP " +entitaOrganizzativaDirUo.getIdnsip() + " NON HA PARTENT");
			}
			finally {
				if (listaGerarchia != null) {

					LOGGER.info("-------------- listaGerarchia size {}", listaGerarchia.size());
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
						entitaOrganizzativaResponsabileStruttura = entitaOrganizzativaDirUo;
					}
					int idEentitaOrganizzativaResponsabileStruttura = entitaOrganizzativaResponsabileStruttura.getId();
					String denominazioneEntitaOrganizzativaResponsabileStruttura = entitaOrganizzativaResponsabileStruttura.getDenominazione();
					LOGGER.info("-------------- padre: id: {} [{}], Gerarchia tipo: {}  [{}]", entitaOrganizzativaResponsabileStruttura.getId(), denominazioneEntitaOrganizzativaResponsabileStruttura, gerarchia.getTipo().getId(), gerarchia.getTipo().getDescr());
					// CHECK DIRETTORE
					String nomeDirettoreSiper = usernameDirettore.toString();
					String gruppoResponsabileStrutturaPadre = "responsabile-struttura@" + entitaOrganizzativaResponsabileStruttura.getId();

					Set<String> membriResponsabileStrutturaPadre = membershipService.getAllUsersInGroup(gruppoResponsabileStrutturaPadre);
					LOGGER.info("nr membriResponsabileStrutturaPadre  {} per struttura {} ", membriResponsabileStrutturaPadre.size(), entitaOrganizzativaResponsabileStruttura.getId());
					if (membriResponsabileStrutturaPadre.size() == 0){
						LOGGER.info("NO --- il direttore della struttura padre {} [{}] NON ESISTE ", entitaOrganizzativaResponsabileStruttura.getId(), denominazioneEntitaOrganizzativaResponsabileStruttura);
					}
					if (membriResponsabileStrutturaPadre.size() > 1){
						LOGGER.info("NO --- ci sono {} direttori per la struttura padre {} [{}] ", membriResponsabileStrutturaPadre.size(), entitaOrganizzativaResponsabileStruttura.getId(), denominazioneEntitaOrganizzativaResponsabileStruttura);
					}
					membriResponsabileStrutturaPadre.forEach(responsabile -> {
						if(responsabile.toString().equals(nomeDirettoreSiper)) {
							LOGGER.info("OK --- " + richiedente.toString() + " -- il direttore della struttura padre [" + idEentitaOrganizzativaResponsabileStruttura + " - " + denominazioneEntitaOrganizzativaResponsabileStruttura +"  [" + responsabile.toString()  +"] CORRISPONDE al direttore SIPER "+ nomeDirettoreSiper + " --");

						} else {
							LOGGER.info("il direttore trovanto in ACE nella struttura padre {} [{}] è {} NON CORRISPONDE al direttore trovato su SIPER {} ", idEentitaOrganizzativaResponsabileStruttura, denominazioneEntitaOrganizzativaResponsabileStruttura, responsabile.toString(), nomeDirettoreSiper);
							LOGGER.debug("NO --- " + richiedente.toString() + " -- il direttore della struttura padre [" + idEentitaOrganizzativaResponsabileStruttura + " - " + denominazioneEntitaOrganizzativaResponsabileStruttura +"  [" + responsabile.toString()  +"] NON CORRISPONDE al direttore SIPER "+ nomeDirettoreSiper + " --");
						}
					});				
			//END NEW
					
					String siglaEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getSigla().toString();
					String denominazioneEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getDenominazione().toString();
					String cdsuoEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getCdsuo().toString();
					String idnsipEntitaorganizzativaResponsabileUtente = entitaOrganizzativaDirUo.getIdnsip().toString();			
					LOGGER.info("L'utente {} ha come direttore {} della struttura {} ({}) [ID: {}] [CDSUO: {}] [IDNSIP: {}]", richiedente.toString(), usernameDirettore, denominazioneEntitaorganizzativaResponsabileUtente, siglaEntitaorganizzativaResponsabileUtente, idEentitaOrganizzativaResponsabileStruttura, cdsuoEntitaorganizzativaResponsabileUtente, idnsipEntitaorganizzativaResponsabileUtente);

					String gruppoValidatoriAccordiInternazionali = "validatoriAccordiInternazionali@0000";
					String gruppoUfficioProtocollo = "ufficioProtocolloAccordiInternazionali@0000";
					String gruppoValutatoreScientificoDipartimento = "valutatoreScientificoDipartimento@0000";
					String gruppoResponsabileAccordiInternazionali = "responsabileAccordiInternazionali@0000";
					//DA CAMBIARE - ricavando il direttore della persona che afferisce alla sua struttura
					String gruppoDirigenteRichiedente = "responsabile-struttura@" + idEentitaOrganizzativaResponsabileStruttura;

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

					execution.setVariable("strutturaValutazioneDirigente", idEentitaOrganizzativaResponsabileStruttura + "-" + denominazioneEntitaorganizzativaResponsabileUtente);
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
		}
	}
}