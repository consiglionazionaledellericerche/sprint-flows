package it.cnr.si.flows.ng.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import feign.Feign;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import it.cnr.si.flows.ng.dto.EntitaOrganizzativaWebDto;
import it.cnr.si.flows.ng.dto.RuoloUtenteWebDto;
import it.cnr.si.flows.ng.utils.AceJwt;
import it.cnr.si.flows.ng.utils.Enum;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Profile("!oiv")
public class AceBridgeService {

    // e' necessario iniettare un'istanza di se stessa per potere utilizzare le cache
    // vedi l'uso di getIdRuoloBySigla
    @Inject
    private AceBridgeService aceService;

    @Value("${spring.ace.url}")
    private String aceUrl;

    @Value("${spring.ace.password}")
    private String acePassword;
    @Value("${spring.ace.username}")
    private String aceUsername;

    public List<String> getAceGroupsForUser(String loginUsername) {

        Ace ace = getAce();

        ArrayList<RuoloUtenteWebDto> ruoliUtente = ace.ruoloUtente(loginUsername);

        return ruoliUtente.stream()
                .map(ruoloUtente -> ruoloUtente.getRuolo().getSigla() + "@" + ruoloUtente.getEntitaOrganizzativa().getId())
                .collect(Collectors.toList());
    }

    public List<String> getUsersInAceGroup(String groupName) {

        if (!groupName.contains("@"))
            return new ArrayList<>();

        String[] split = groupName.split("@");
        String sigla = split[0];
        int idEo = Integer.parseInt(split[1]);


        Ace ace = getAce();

        int idRuolo = aceService.getIdRuoloBySigla(sigla);

        return ace.utentiInRuoloEo(idRuolo, idEo)
                .stream()
                .map(p -> p.getUsername())
                .collect(Collectors.toList());

    }


    public List<EntitaOrganizzativaWebDto> getUoLike(String uoName) {
        Ace ace = getAce();

        return ace.entitaOrganizzativaFind(uoName)
                .getItems()
                .stream()
                .filter(e -> Enum.TipiEOPerAutocomplete.contains(e.getTipo().getId()))
//                .map(e -> Pair.of(e.getId(), e.getCdsuo() +" - "+ e.getDenominazione()))
                .collect(Collectors.toList());
    }

    @Cacheable("idRuoloBySigla")
    public int getIdRuoloBySigla(String sigla) {

        Ace ace = getAce();

        return ace.ruoloBySigla(sigla).getId();
    }

    @Cacheable("nuomeRuoloBySigla")
    public String getNomeRuoloBySigla(String sigla) {

        Ace ace = getAce();

        return ace.ruoloBySigla(sigla).getDescr();
    }

    @Cacheable("nomiStrutture")
    public String getNomeStruturaById(Integer id) {
        Ace ace = getAce();

        return ace.entitaOrganizzativaById(id).getDenominazione();
    }

    @Cacheable("nomiEstesiGruppiRuoloStruttura")
    public String getExtendedGroupNome(String groupRuoloStrutturaName) {
        if (groupRuoloStrutturaName == null)
            return null;

        String[] splitGroupRuoloStrutturaName = groupRuoloStrutturaName.split("@");
        String ruoloName = splitGroupRuoloStrutturaName[0];
        Integer strutturaId = Integer.parseInt(splitGroupRuoloStrutturaName[1]);

        String descrizioneRuolo = aceService.getNomeRuoloBySigla(ruoloName);
        String descrizioneStruttura = aceService.getNomeStruturaById(strutturaId);

        return (descrizioneRuolo + "@" + descrizioneStruttura);
    }

    private Ace getAce() {
        final AceAuthService service = Feign.builder()
                .decoder(new GsonDecoder())
                .encoder(new FormEncoder(new GsonEncoder()))
                .target(AceAuthService.class, aceUrl + "api");

        // final AceJwt refreshed = service.getRefreshedToken(token.getRefresh_token());
        final AceJwt token = service.getToken(aceUsername, acePassword);

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ITALIAN)
                .withZone(ZoneId.of("Europe/Rome"));
        GsonJava8TypeAdapterFactory typeAdapterFactory = new GsonJava8TypeAdapterFactory()
                .setInstantFormatter(formatter);
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeAdapterFactory).create();

        Ace ace = Feign.builder()
                // Aggiunge l'header con il token per tutte le richieste fatte da questo servizio
                .requestInterceptor(new TokenRequestInterceptor(token.getAccess_token()))
                .decoder(new GsonDecoder(gson))
                .encoder(new GsonEncoder(gson))
                .target(Ace.class, aceUrl);
        return ace;
    }

}