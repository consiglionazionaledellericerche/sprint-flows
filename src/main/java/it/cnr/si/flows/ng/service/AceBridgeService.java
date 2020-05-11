package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.base.PageDto;
import it.cnr.si.service.dto.anagrafica.enums.TipoAppartenenza;
import it.cnr.si.service.dto.anagrafica.letture.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static it.cnr.si.security.PermissionEvaluatorImpl.CNR_CODE;

@Service
@Profile("cnr")
public class AceBridgeService {

	private final Logger log = LoggerFactory.getLogger(AceBridgeService.class);

	@Inject
	private AceService aceService;

	public List<String> getAceGroupsForUser(String loginUsername) {

		log.debug("Recupero i ruoli per l'utente {}", loginUsername);

		ArrayList<RuoloUtenteWebDto> ruoliUtente = aceService.ruoloUtente(loginUsername);

		return ruoliUtente.stream()
				.map(ruoloUtente -> {
					if ( ruoloUtente.getEntitaOrganizzativa() != null) {
						return ruoloUtente.getRuolo().getSigla() + "@" + ruoloUtente.getEntitaOrganizzativa().getId();
					} else {
						return ruoloUtente.getRuolo().getSigla() + "@" + CNR_CODE;
					}
				})
				.collect(Collectors.toList());
	}

	/*
	 * ATTENZIONE! Usare ???() per prendere tutti gli utenti, compresi col ruolo-nel-ruolo
	 * Usare questo solo per prendere solo i gruppi di uno specifico gruppo Ace
	 *
	 * L'unico utilizzo giustificato di questo service e' in MembershipService
	 */
	@Deprecated
	public List<String> getUsersInAceGroup(String groupName) {

		if (!groupName.contains("@"))
			return new ArrayList<>();

		String[] split = groupName.split("@");
		String sigla = split[0];
		if ("STRUTTURA".equals(split[1]))
			return new ArrayList<String>();
		
		int idEo = Integer.parseInt(split[1]);

		int idRuolo = getIdRuoloBySigla(sigla);

		if (idEo != 0 )
			return aceService.getUtentiInRuoloEo(idRuolo, idEo).stream()
					.map(p -> aceService.getUtente(p.getUtente()))
					.map(u -> u.getUsername())
					.collect(Collectors.toList());
		else
			return aceService.getUtentiInRuoloCnr(idRuolo)
					.stream()
					.map(RuoloUtenteWebDto::getUtente)
					.map(UtenteWebDto::getUsername)
					.collect(Collectors.toList());
	}

	public EntitaOrganizzativaWebDto getUoById(int id) {

		return aceService.entitaOrganizzativaById(id);
	}

	public List<EntitaOrganizzativaWebDto> getUoLike(String uoName) {

		return aceService.entitaOrganizzativaFind(null, null, uoName, LocalDate.now(), null)
				.getItems()
				.stream()
				.filter(e -> Enum.TipiEOPerAutocomplete.contains(e.getTipo().getId()))
				//                .map(e -> Pair.of(e.getId(), e.getCdsuo() +" - "+ e.getDenominazione()))
				.collect(Collectors.toList());
	}


	//	todo: in futuro rendere cacheable?
	public List<EntitaOrganizzativaWebDto> getUoByTipo(int tipo) {

		return aceService.entitaOrganizzativaFind(null, null, null, LocalDate.now(), tipo)
				.getItems()
				.stream()
				.filter(e -> Enum.TipiEOPerAutocomplete.contains(e.getTipo().getId()))
				//                .map(e -> Pair.of(e.getId(), e.getCdsuo() +" - "+ e.getDenominazione()))
				.collect(Collectors.toList());
	}

	@Cacheable("idRuoloBySigla")
	public int getIdRuoloBySigla(String sigla) {

		return aceService.getRuoloBySigla(sigla).getId();
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

	public EntitaOrganizzativaWebDto getStrutturaById(Integer id) {
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

		PersonaWebDto persona = aceService.getPersonaByUsername(username);
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


	public EntitaOrganizzativaWebDto getAfferenzaUtenteTipoSede(String username) {

		PersonaWebDto persona = aceService.getPersonaByUsername(username);
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


	public EntitaOrganizzativaWebDto getEntitaOrganizzativaDellUtente(String username) {

		String cdsuo = getAfferenzaUtente(username).getCdsuo();

		PageDto<EntitaOrganizzativaWebDto> entitaOrganizzativaWebDtoPageDto = aceService.entitaOrganizzativaFind(null, null, cdsuo, LocalDate.now(), null);

		List<EntitaOrganizzativaWebDto> eos = entitaOrganizzativaWebDtoPageDto.getItems().stream()
				.filter(eo -> Objects.isNull(eo.getEntitaLocale()))
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
}