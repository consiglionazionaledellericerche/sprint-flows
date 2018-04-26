package it.cnr.si.flows.ng.service;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("!oiv")
public class AceBridgeService {

	@Resource(name = "aceJdbcTemplate")
	private JdbcTemplate aceJdbcTemplate;

    private static final String GROUPS_FOR_USER = "SELECT persona.nome, persona.cognome, persona.userid, persona.id, ruolo.sigla, ruolo.descr, ruolo.id,entitaorganizzativa.sigla as eosigla, entitaorganizzativa.denominazione, entitaorganizzativa.id as eoid " +
			"FROM ace_old.assegnazioneruolo "+
			"INNER JOIN ace_old.persona ON persona.id = assegnazioneruolo.ass_persona_id "+
			"INNER JOIN ace_old.ruolo ON ruolo.id = assegnazioneruolo.ruolo_id "+
			"INNER JOIN ace_old.entitaorganizzativa ON entitaorganizzativa.id = assegnazioneruolo.entitaorganizzativa_id "+
			"where assegnazioneruolo.ass_persona_id = (SELECT id FROM ace_old.persona WHERE persona.userid = ?)";

    private static final String USERS_IN_ROLE = "SELECT persona.nome, persona.cognome, persona.id, persona.userid, ruolo.sigla, ruolo.descr, ruolo.id,entitaorganizzativa.sigla as eosigla, entitaorganizzativa.denominazione, entitaorganizzativa.id as eoid " +
			"FROM ace_old.assegnazioneruolo "+
			"INNER JOIN ace_old.persona ON persona.id = assegnazioneruolo.ass_persona_id "+
			"INNER JOIN ace_old.ruolo ON ruolo.id = assegnazioneruolo.ruolo_id "+
			"INNER JOIN ace_old.entitaorganizzativa ON entitaorganizzativa.id = assegnazioneruolo.entitaorganizzativa_id "+
			"where ruolo.sigla = ?  "+
			"and entitaorganizzativa.id = ?";

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

    private static final String DENOMINAZIONE_STRUTTURA = "Select entitaorganizzativa.denominazione, entitaorganizzativa.sigla, entitaorganizzativa.denominazionebreve "
			+ "from ace_old.entitaorganizzativa "
			+ "where entitaorganizzativa.id = ?";

    private static final String DENOMINAZIONE_RUOLO = "Select ruolo.descr, ruolo.sigla, ruolo.id "
			+ "from ace_old.ruolo "
			+ "where ruolo.sigla = ?";


	public List<String> getAceGroupsForUser(String username) {

        return aceJdbcTemplate.query(GROUPS_FOR_USER, new Object[]{username}, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("sigla") + "@" + rs.getString("eoid");
			}
		});
	}

	public List<String> getUsersinAceGroup(String groupName) {

		if (!groupName.contains("@"))
            return new ArrayList<>();

		String[] split = groupName.split("@");
		String sigla = split[0];
		Integer eo = Integer.parseInt(split[1]);
		Object[] args = new Object[] {sigla, eo};

        return aceJdbcTemplate.query(USERS_IN_ROLE, args, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString("userid");
			}
		});
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

}
