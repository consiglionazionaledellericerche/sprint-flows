package it.cnr.si.flows.ng.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class AceBridgeService {

	@Resource(name = "aceJdbcTemplate")
	private JdbcTemplate aceJdbcTemplate;

	private static final String groupsForUser = "SELECT persona.nome, persona.cognome, persona.userid, persona.id, ruolo.sigla, ruolo.descr, ruolo.id,entitaorganizzativa.sigla as eosigla, entitaorganizzativa.denominazione, entitaorganizzativa.id as eoid "+
			"FROM ace.assegnazioneruolo "+
			"INNER JOIN ace.persona ON persona.id = assegnazioneruolo.ass_persona_id "+
			"INNER JOIN ace.ruolo ON ruolo.id = assegnazioneruolo.ruolo_id "+
			"INNER JOIN ace.entitaorganizzativa ON entitaorganizzativa.id = assegnazioneruolo.entitaorganizzativa_id "+
			"where assegnazioneruolo.ass_persona_id = (SELECT id FROM ace.persona WHERE persona.userid = ?)";

	private static final String usersInRole = "SELECT persona.nome, persona.cognome, persona.id, persona.userid, ruolo.sigla, ruolo.descr, ruolo.id,entitaorganizzativa.sigla as eosigla, entitaorganizzativa.denominazione, entitaorganizzativa.id as eoid "+
			"FROM ace.assegnazioneruolo "+
			"INNER JOIN ace.persona ON persona.id = assegnazioneruolo.ass_persona_id "+
			"INNER JOIN ace.ruolo ON ruolo.id = assegnazioneruolo.ruolo_id "+
			"INNER JOIN ace.entitaorganizzativa ON entitaorganizzativa.id = assegnazioneruolo.entitaorganizzativa_id "+
			"where ruolo.sigla = ?  "+
			"and entitaorganizzativa.id = ?";

	private static final String uoLike = "select distinct entitaorganizzativa.id, entitaorganizzativa.sigla, entitaorganizzativa.denominazione, entitaorganizzativa.cdsuo " +
			"from ace.entitaorganizzativa " +
			"INNER JOIN ace.tipoentitaorganizzativa ON tipoentitaorganizzativa.id = entitaorganizzativa.tipo_id " +
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

	private static final String denominazioneStruttura = "Select entitaorganizzativa.denominazione, entitaorganizzativa.sigla, entitaorganizzativa.denominazionebreve "
			+ "from ace.entitaorganizzativa "
			+ "where entitaorganizzativa.id = ?";

	private static final String denominazioneRuolo = "Select ruolo.descr, ruolo.sigla, ruolo.id "
			+ "from ace.ruolo "
			+ "where ruolo.sigla = ?";


	public List<String> getAceGroupsForUser(String username) {

		return aceJdbcTemplate.query(groupsForUser, new Object[] {username}, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getString("sigla") +"@"+ rs.getString("eoid");
			}
		});
	}

	public List<String> getUsersinAceGroup(String groupName) {

		if (!groupName.contains("@"))
			return null;

		String[] split = groupName.split("@");
		String sigla = split[0];
		Integer eo = Integer.parseInt(split[1]);
		Object[] args = new Object[] {sigla, eo};

		return aceJdbcTemplate.query(usersInRole, args, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				//return rs.getString("sigla") +"@"+ rs.getString("eoid");
				return rs.getString("userid");
			}
		});
	}

	public List<Pair<Integer, String>> getUoLike(String uoName) {
		uoName = "%" + uoName + "%";
		Object[] args = new Object[] {uoName, uoName};

		return aceJdbcTemplate.query(uoLike, args, new RowMapper<Pair<Integer, String>>() {
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
		return aceJdbcTemplate.query(denominazioneStruttura, new Object[] {id}, new ResultSetExtractor<String>() {
			public String extractData(ResultSet rs) throws SQLException, DataAccessException {
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

		String descrizioneRuolo = aceJdbcTemplate.query(denominazioneRuolo, new Object[] {ruoloName}, new ResultSetExtractor<String>() {
			public String extractData(ResultSet rs) throws SQLException, DataAccessException {
				rs.next();
				return rs.getString("descr");
			}
		});
		String descrizioneStruttura = aceJdbcTemplate.query(denominazioneStruttura, new Object[] {strutturaId}, new ResultSetExtractor<String>() {
			public String extractData(ResultSet rs) throws SQLException, DataAccessException {
				rs.next();
				return rs.getString("sigla");
			}
		});
		return (descrizioneRuolo + "@" + descrizioneStruttura);
	}

}
