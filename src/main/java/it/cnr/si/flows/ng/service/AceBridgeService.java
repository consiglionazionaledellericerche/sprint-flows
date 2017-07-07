package it.cnr.si.flows.ng.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class AceBridgeService {

    @Resource(name = "aceJdbcTemplate")
    private JdbcTemplate aceJdbcTemplate;

    private static final String testQuery = "SELECT persona.nome, persona.cognome, persona.userid, persona.id, ruolo.sigla, ruolo.descr, ruolo.id,entitaorganizzativa.sigla, entitaorganizzativa.denominazione, entitaorganizzativa.id as eoid "+
            "FROM ace.assegnazioneruolo "+
            "INNER JOIN ace.persona ON persona.id = assegnazioneruolo.persona_id "+
            "INNER JOIN ace.ruolo ON ruolo.id = assegnazioneruolo.ruolo_id "+
            "INNER JOIN ace.entitaorganizzativa ON entitaorganizzativa.id = assegnazioneruolo.entitaorganizzativa_id "+
            "where assegnazioneruolo.persona_id = (SELECT id FROM ace.persona WHERE persona.userid = ?)";


    public List<String> getAceGroupsForUser(String username) {

        List<String> authorities = aceJdbcTemplate.query(testQuery, new Object[] {username}, new RowMapper<String>() {

            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                // TODO Auto-generated method stub
                return rs.getString("sigla") +"@"+ rs.getString("eoid");
            }
        });


        return authorities;
    }

}
