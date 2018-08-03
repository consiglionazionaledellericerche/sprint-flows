package it.cnr.si.flows.ng.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import feign.Feign;
import feign.form.FormEncoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import it.cnr.si.flows.ng.dto.PersonaWebDto;
import it.cnr.si.flows.ng.dto.RuoloUtenteWebDto;
import it.cnr.si.flows.ng.utils.AceJwt;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    @Deprecated
    @Resource(name = "aceJdbcTemplate")
    private JdbcTemplate aceJdbcTemplate;

    @Value("${spring.ace.url}")
    private String aceUrl;

    @Value("${spring.ace.password}")
    private String acePassword;
    @Value("${spring.ace.username}")
    private String aceUsername;


    @Deprecated
    private static final String UO_LIKE = "select distinct entitaorganizzativa.id, entitaorganizzativa.sigla, entitaorganizzativa.denominazione, entitaorganizzativa.cdsuo " +
            "from ace_old.entitaorganizzativa " +
            "INNER JOIN ace_old.tipoentitaorganizzativa ON tipoentitaorganizzativa.id = entitaorganizzativa.tipo_id " +
            "where  " +
            "(tipoentitaorganizzativa.id = 1 " +
            "OR tipoentitaorganizzativa.id = 6 " +
            "OR tipoentitaorganizzativa.id = 21 " +
            "OR tipoentitaorganizzativa.id = 26 " +
            "OR tipoentitaorganizzativa.id = 41 " +
            "OR tipoentitaorganizzativa.id = 42 " +
            "OR tipoentitaorganizzativa.id = 43) " +
            "AND (entitaorganizzativa.finevalidita IS NULL AND entitaorganizzativa.cdsuo <> 'SOPPRE') " +
            "AND (entitaorganizzativa.sigla ilike ? OR entitaorganizzativa.denominazione ilike ?)";

    @Deprecated
    private static final String DENOMINAZIONE_STRUTTURA = "Select entitaorganizzativa.denominazione, entitaorganizzativa.sigla, entitaorganizzativa.denominazionebreve "
            + "from ace_old.entitaorganizzativa "
            + "where entitaorganizzativa.id = ?";

    @Deprecated
    private static final String DENOMINAZIONE_RUOLO = "Select ruolo.descr, ruolo.sigla, ruolo.id "
            + "from ace_old.ruolo "
            + "where ruolo.sigla = ?";


    public List<String> getAceGroupsForUser(String loginUsername) {

        final AceJwt token = getAceJwtToken();

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


        final AceJwt token = getAceJwtToken();

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

        int idRuolo = aceService.getIdRuoloBySigla(sigla);

        return ace.utentiInRuoloEo(idRuolo, idEo)
                .stream()
                .map(p -> p.getUsername())
                .collect(Collectors.toList());

    }

    public List<Pair<Integer, String>> getUoLike(String uoName) {
        uoName = "%" + uoName + "%";
        Object[] args = new Object[] {uoName, uoName};

        return aceJdbcTemplate.query(UO_LIKE, args, new RowMapper<Pair<Integer, String>>() {
            @Override
            public Pair<Integer, String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Integer idUo = rs.getInt("id");

                String sigla = rs.getString("sigla");
                if(sigla == null){
                    sigla = " ";
                }
                String denominazione = rs.getString("denominazione");
                String cdsuo = rs.getString("cdsuo");
                return Pair.of(idUo, cdsuo + "-" + sigla + "-" + denominazione );
            }
        });
    }

    @Cacheable("idRuoloBySigla")
    public int getIdRuoloBySigla(String sigla) {

        final AceJwt token = getAceJwtToken();

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

        return ace.ruoloBySigla(sigla).getId();
    }

    @Cacheable("nomiStrutture")
    public String getNomeStruturaById(Integer id) {
        return aceJdbcTemplate.query(DENOMINAZIONE_STRUTTURA, new Object[]{id}, new ResultSetExtractor<String>() {
            public String extractData(ResultSet rs) throws SQLException {
                rs.next();
                return rs.getString("denominazione");
            }
        });
    }

    @Cacheable("nomiEstesiGruppiRuoloStruttura")
    public String getExtendedGroupNome(String groupRuoloStrutturaName) {
        if (groupRuoloStrutturaName == null)
            return null;

        String[] splitGroupRuoloStrutturaName = groupRuoloStrutturaName.split("@");
        String ruoloName = splitGroupRuoloStrutturaName[0];
        Integer strutturaId = Integer.parseInt(splitGroupRuoloStrutturaName[1]) ;

        String descrizioneRuolo = aceJdbcTemplate.query(DENOMINAZIONE_RUOLO, new Object[]{ruoloName}, new ResultSetExtractor<String>() {
            public String extractData(ResultSet rs) throws SQLException {
                rs.next();
                return rs.getString("descr");
            }
        });
        String descrizioneStruttura = aceJdbcTemplate.query(DENOMINAZIONE_STRUTTURA, new Object[]{strutturaId}, new ResultSetExtractor<String>() {
            public String extractData(ResultSet rs) throws SQLException {
                rs.next();
                return rs.getString("sigla");
            }
        });
        return (descrizioneRuolo + "@" + descrizioneStruttura);
    }


    // todo: fare il refresh anzicchè il login?
    private AceJwt getAceJwtToken() {
        final AceAuthService service = Feign.builder()
                .decoder(new GsonDecoder())
                .encoder(new FormEncoder(new GsonEncoder()))
                .target(AceAuthService.class, aceUrl + "api");

        // final AceJwt refreshed = service.getRefreshedToken(token.getRefresh_token());
        return service.getToken(aceUsername, acePassword);
    }

}
