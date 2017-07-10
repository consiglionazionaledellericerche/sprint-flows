package it.cnr.si.flows.ng.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class AceBridgeService {

    @Resource(name = "aceJdbcTemplate")
    private JdbcTemplate aceJdbcTemplate;

    private static final String groupsForUser = "SELECT persona.nome, persona.cognome, persona.userid, persona.id, ruolo.sigla, ruolo.descr, ruolo.id,entitaorganizzativa.sigla, entitaorganizzativa.denominazione, entitaorganizzativa.id as eoid "+
            "FROM ace.assegnazioneruolo "+
            "INNER JOIN ace.persona ON persona.id = assegnazioneruolo.persona_id "+
            "INNER JOIN ace.ruolo ON ruolo.id = assegnazioneruolo.ruolo_id "+
            "INNER JOIN ace.entitaorganizzativa ON entitaorganizzativa.id = assegnazioneruolo.entitaorganizzativa_id "+
            "where assegnazioneruolo.persona_id = (SELECT id FROM ace.persona WHERE persona.userid = ?)";

    private static final String usersInRole = "SELECT persona.nome, persona.cognome, persona.id, persona.userid, ruolo.sigla, ruolo.descr, ruolo.id,entitaorganizzativa.sigla, entitaorganizzativa.denominazione, entitaorganizzativa.id "+
        "FROM ace.assegnazioneruolo "+
        "INNER JOIN ace.persona ON persona.id = assegnazioneruolo.persona_id "+
        "INNER JOIN ace.ruolo ON ruolo.id = assegnazioneruolo.ruolo_id "+
        "INNER JOIN ace.entitaorganizzativa ON entitaorganizzativa.id = assegnazioneruolo.entitaorganizzativa_id "+
        "where ruolo.sigla = ?  "+
        "and entitaorganizzativa.id = ?";

    private static final String denominazioneStruttura = "Select entitaorganizzativa.denominazione, entitaorganizzativa.sigla, entitaorganizzativa.denominazionebreve "
            + "from ace.entitaorganizzativa "
            + "where entitaorganizzativa.id = ?";

    public List<String> getAceGroupsForUser(String username) {

        return aceJdbcTemplate.query(groupsForUser, new Object[] {username}, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("sigla") +"@"+ rs.getString("eoid");
            }
        });
    }

    public List<String> getUsersinAceGroup(String groupName) {

        String[] split = groupName.split("@");
        String sigla = split[0];
        Integer eo = Integer.parseInt(split[1]);
        Object[] args = new Object[] {sigla, eo};

        return aceJdbcTemplate.query(usersInRole, args, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString("userid");
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

}
