package it.cnr.si.flows.ng.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;

/**
 * Per poter creare un datasource secondario, non posso piu' affidarmi all'autoconfigurazione
 * ma creare manualmente sia il datasource primario che il secondario.
 *
 * Il codice di creazione del datasource primario e' copia-incollato dall'autoconfigurazione di Spring Boot
 *
 * Il datasource secondario attualmente serve per connettersi ad ACE e ricercare appartenenza a gruppi (in readonly)
 *
 * @author mtrycz
 *
 */

@Configuration
public class DatasourcesConfiguration {

    @Inject
    private Environment env;


    @Bean(name = {"dataSourceProperties"})
    @ConfigurationProperties(prefix="spring.datasource")
    @Primary
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }


    @Bean(name = {"dataSource"})
    @Primary
    @ConfigurationProperties(prefix="spring.datasource")
    public HikariDataSource cataSource(DataSourceProperties properties) {
        return (HikariDataSource) DataSourceBuilder.create(properties.getClassLoader())
                .type(HikariDataSource.class)
                .driverClassName(properties.determineDriverClassName())
                .url(getDbUrl(properties.determineUrl(), Arrays.asList(env.getActiveProfiles())))
                .username(properties.determineUsername())
                .password(properties.determinePassword()).build();
    }


    //serve per far puntare db diversi se uso il profilo "oiv" o il profilo "cnr"
    private String getDbUrl(String dbUrl, Collection<String> springActiveProfiles) {

        //serve per far puntare db diversi se uso il profilo oiv o il profilo cnr
        if (springActiveProfiles.contains("cnr")) {
            dbUrl = dbUrl.replace("flows", "flows-cnr");
        } else if (springActiveProfiles.contains("oiv")) {
            dbUrl = dbUrl.replace("flows", "flows-oiv");
        }
        return dbUrl;
    }
}
