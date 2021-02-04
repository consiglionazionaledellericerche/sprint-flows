package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.enums.TipoAppartenenza;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.GerarchiaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimplePersonaWebDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleUtenteWebDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.security.PermissionEvaluatorImpl.CNR_CODE;

@Service
@Profile("cnr")
public class AceBridgeService {

	public static final String SEDE_PRINCIPALE = "SPRINC";
	private final Logger log = LoggerFactory.getLogger(AceBridgeService.class);

	@Inject
	private AceService aceService;

	public Set<String> getAceRolesForUser(String username) {

		Set<String> ruoli = new HashSet<>();
		aceService.ruoliUtenteAttivi(username).stream().forEach(ruoloUtente -> {
					String idStruttura = ruoloUtente.getEntitaOrganizzativa() != null ?
							ruoloUtente.getEntitaOrganizzativa().getId().toString() :
							CNR_CODE;
					ruoli.add(ruoloUtente.getRuolo().getSigla() + "@" + idStruttura);
					ruoloUtente.getRuolo().getRuoliGruppoAssociati().stream().forEach(ruoloAssociato ->
						ruoli.add(ruoloAssociato.getSigla() + "@" + idStruttura));
				});
		log.debug("Ruoli da ace per l'utente {}: {}", username, ruoli);
		return ruoli;
	}

	/*
	 * ATTENZIONE! Usare ???() per prendere tutti gli utenti, compresi col ruolo-nel-ruolo
	 * Usare questo solo per prendere solo i gruppi di uno specifico gruppo Ace
	 *
	 * L'unico utilizzo giustificato di questo service e' in MembershipService
	 */
	@Deprecated
	public Set<String> getUsersInAceGroup(String groupName) {

		if (!groupName.contains("@"))
			return Collections.emptySet();

		String[] split = groupName.split("@");
		String sigla = split[0];
		if ("STRUTTURA".equals(split[1]))
			return Collections.emptySet();
		
		int idEo = Integer.parseInt(split[1]);

		if (idEo != 0 )
			return aceService.getUtentiInRuoloEo(sigla, idEo).stream()
					.map(SimpleUtenteWebDto::getUsername)
					.collect(Collectors.toSet());
		else
			return aceService.getUtentiInRuoloCnr(sigla)
					.stream()
					.map(BossDto::getUtente)
					.map(SimpleUtenteWebDto::getUsername)
					.collect(Collectors.toSet());
	}

	public SimpleEntitaOrganizzativaWebDto getUoById(int id) {

		return aceService.entitaOrganizzativaById(id);
	}

	public List<SimpleEntitaOrganizzativaWebDto> getUoLike(String uoName) {

		return aceService.entitaOrganizzativaFind(null, uoName, LocalDate.now(), null)
				.stream()
				.filter(e -> e.getTipo().getSigla().contains(SEDE_PRINCIPALE))
				.collect(Collectors.toList());
	}

	public List<SimpleEntitaOrganizzativaWebDto> getUoByTipo(int idTipo) {

		return aceService.entitaOrganizzativaFind(null, null, LocalDate.now(), idTipo)
				.stream()
				.filter(e -> e.getTipo().getDescr().equals(Enum.TipiEOPerAutocomplete.byId(idTipo).name()) )
//				                .map(e -> Pair.of(e.getId(), e.getCdsuo() +" - "+ e.getDenominazione()))
				.collect(Collectors.toList());
	}

	@Cacheable("nuomeRuoloBySigla")
	public String getNomeRuoloBySigla(String sigla) {

		return aceService.getRuoloBySigla(sigla).getDescr();
	}

	@Cacheable("nomiStrutture")
	public String getNomeStruturaById(Integer id) {

		if (id == 0) {
			return "CNR";
		} else {
			return getStrutturaById(id).getDenominazione();
		}
	}

	public SimpleEntitaOrganizzativaWebDto getStrutturaById(Integer id) {
		return aceService.entitaOrganizzativaById(id);
	}

	@Cacheable("getExtendedGroupNome")
	public String getExtendedGroupNome(String groupRuoloStrutturaName) {
		if (groupRuoloStrutturaName == null)
			return null;

		String[] splitGroupRuoloStrutturaName = groupRuoloStrutturaName.split("@");
		String ruoloName = splitGroupRuoloStrutturaName[0];
		String descrizioneRuolo = getNomeRuoloBySigla(ruoloName);

		if (splitGroupRuoloStrutturaName.length > 1) {
			Integer strutturaId = Integer.parseInt(splitGroupRuoloStrutturaName[1]);
			String descrizioneStruttura = getNomeStruturaById(strutturaId);
			return (descrizioneRuolo + "@" + descrizioneStruttura);
		} else {
			return (descrizioneRuolo);

		}
	}


	public EntitaOrganizzativaWebDto getAfferenzaUtente(String username) {

		SimplePersonaWebDto persona = aceService.getPersonaByUsername(username);
		List<PersonaEntitaOrganizzativaWebDto> personaEntitaOrganizzativaWebDtos = aceService.personaEntitaOrganizzativaFind(null, null, null, persona.getId(), TipoAppartenenza.AFFERENZA_UO, null, null, null, null);
		List<PersonaEntitaOrganizzativaWebDto> afferenze = personaEntitaOrganizzativaWebDtos.stream()
				.filter(p -> Objects.isNull(p.getFineValidita()))
				.collect(Collectors.toList());

		if (afferenze.size() == 0)
			throw new UnexpectedResultException("Nessuna afferenza corrente per l'utente: "+ username);
		if (afferenze.size() > 1)
			throw new UnexpectedResultException("L'utente risulta avere piu' di una afferenza: "+ username);

		return afferenze.get(0).getEntitaOrganizzativa();
	}

	public EntitaOrganizzativaWebDto getAfferenzaUtentePerSede(String username) {

	    SimplePersonaWebDto persona = aceService.getPersonaByUsername(username);
		List<PersonaEntitaOrganizzativaWebDto> personaEntitaOrganizzativaWebDtos = aceService.personaEntitaOrganizzativaFind(null, null, null, persona.getId(), TipoAppartenenza.SEDE, null, null, null, null);
		List<PersonaEntitaOrganizzativaWebDto> afferenze = personaEntitaOrganizzativaWebDtos.stream()
				.filter(p -> Objects.isNull(p.getFineValidita()))
				.collect(Collectors.toList());

		if (afferenze.size() == 0)
			throw new UnexpectedResultException("Nessuna afferenza corrente per l'utente: "+ username);
		if (afferenze.size() > 1)
			throw new UnexpectedResultException("L'utente risulta avere piu' di una afferenza: "+ username);

		return afferenze.get(0).getEntitaOrganizzativa();
	}

	public EntitaOrganizzativaWebDto getAfferenzaUtenteTipoSede(String username) {

	    SimplePersonaWebDto persona = aceService.getPersonaByUsername(username);
		List<PersonaEntitaOrganizzativaWebDto> personaEntitaOrganizzativaWebDtos = aceService.personaEntitaOrganizzativaFind(null, null, null, persona.getId(), TipoAppartenenza.SEDE, null, null, null, null);
		List<PersonaEntitaOrganizzativaWebDto> afferenze = personaEntitaOrganizzativaWebDtos.stream()
				.filter(p -> Objects.isNull(p.getFineValidita()))
				.collect(Collectors.toList());

		if (afferenze.size() == 0)
			throw new UnexpectedResultException("Nessuna afferenza corrente per l'utente: "+ username);
		if (afferenze.size() > 1)
			throw new UnexpectedResultException("L'utente risulta avere piu' di una afferenza: "+ username);

		return afferenze.get(0).getEntitaOrganizzativa();
	}


	public SimpleEntitaOrganizzativaWebDto getEntitaOrganizzativaDellUtente(String username) {

		String cdsuo = getAfferenzaUtente(username).getCdsuo();

		List<SimpleEntitaOrganizzativaWebDto> entitaOrganizzativaWebDtoPageDto = aceService.entitaOrganizzativaFind(null, cdsuo, LocalDate.now(), null);

		List<SimpleEntitaOrganizzativaWebDto> eos = entitaOrganizzativaWebDtoPageDto.stream()
		        // TODO verificare che finalita' avesse questo check eo.getEntitaLocale()
//				.filter(eo -> Objects.isNull(eo.getEntitaLocale()))
				.collect(Collectors.toList());

		if (eos.size() == 0)
			throw new UnexpectedResultException("Nessuna entita' organizzativa per il cdsuo: "+ cdsuo);
		if (eos.size() > 1)
			throw new UnexpectedResultException("Il Cdsuo risulta avere piu' entita' organizzative: "+ username);

		return eos.get(0);
	}

    public List<GerarchiaWebDto> getParents(long id) {
		return aceService.getParentsForEo(id);
	}

	public BossDto bossFirmatarioByUsername(String username) {
		return aceService.findResponsabileStruttura(username);
	}

	public PersonaEntitaOrganizzativaWebDto personaEntitaOrganizzativaById(Integer id) {
		return aceService.personaEntitaOrganizzativaById(id);
	}
}