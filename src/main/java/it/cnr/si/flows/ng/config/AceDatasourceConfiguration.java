package it.cnr.si.flows.ng.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class AceDatasourceConfiguration {

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
    @Bean(name = {"aceDataSource"})
    @ConfigurationProperties(prefix="datasource.secondary")
    public HikariDataSource aceDataSource(DataSourceProperties properties) {

        return (HikariDataSource) DataSourceBuilder.create(properties.getClassLoader()).type(HikariDataSource.class)
                .driverClassName(com.zaxxer.hikari.HikariDataSource.class.getName())
                .url("jdbc:h2:file:./DB-H3/flows;DB_CLOSE_DELAY=-1")
                .username("flows")
                .build();
    }
}
