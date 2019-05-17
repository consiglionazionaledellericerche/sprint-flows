package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.enums.TipoAppartenenza;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaEntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.RuoloUtenteWebDto;
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
@Profile("!oiv")
public class AceBridgeService {

	@Inject
	private AceService aceService;

	public List<String> getAceGroupsForUser(String loginUsername) {

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

	public List<String> getUsersInAceGroup(String groupName) {

		if (!groupName.contains("@"))
			return new ArrayList<>();

		String[] split = groupName.split("@");
		String sigla = split[0];
		int idEo = Integer.parseInt(split[1]);

		int idRuolo = getIdRuoloBySigla(sigla);

		return aceService.getUtentiInRuoloEo(idRuolo, idEo)
				.stream()
				.map(p -> p.getUsername())
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

	//    @Cacheable("idRuoloBySigla")
	public int getIdRuoloBySigla(String sigla) {

		return aceService.getRuoloBySigla(sigla).getId();
	}

	//    @Cacheable("nuomeRuoloBySigla")
	public String getNomeRuoloBySigla(String sigla) {

		return aceService.getRuoloBySigla(sigla).getDescr();
	}

	//    @Cacheable("nomiStrutture")
	public String getNomeStruturaById(Integer id) {

		if (id == 0) {
			return "CNR";
		} else {
			return aceService.entitaOrganizzativaById(id).getDenominazione();
		}
	}

	//    @Cacheable("nomiEstesiGruppiRuoloStruttura")
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
}