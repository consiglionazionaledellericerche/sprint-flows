package it.cnr.si.flows.ng.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

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
        return (HikariDataSource) DataSourceBuilder.create(properties.getClassLoader()).type(HikariDataSource.class)
                .driverClassName(properties.determineDriverClassName())
                .url(properties.determineUrl()).username(properties.determineUsername())
                .password(properties.determinePassword()).build();
    }


    // TODO preferirei affidarmi all'autoconfigurazione di JHipster e non dover creare un dataSource @Primary per poter creare questo secondo... - Martin
    @Bean(name = {"aceDataSourceProperties"})
    @ConfigurationProperties(prefix="spring.datasource.ace")
    public DataSourceProperties aceDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = {"aceDataSource"})
    public HikariDataSource aceDataSource(@Qualifier("aceDataSourceProperties") DataSourceProperties properties) {

        return (HikariDataSource) DataSourceBuilder
                .create(properties.getClassLoader())
                .type(HikariDataSource.class)
                .driverClassName(properties.determineDriverClassName())
                .url(properties.determineUrl())
                .username(properties.determineUsername())
                .password(properties.determinePassword())
                .build();
    }

    @Bean(name= {"aceJdbcTemplate"})
    public JdbcTemplate aceJdbcTemplate(@Qualifier("aceDataSource") DataSource aceDataSource) {
        return new JdbcTemplate(aceDataSource);
    }

}
