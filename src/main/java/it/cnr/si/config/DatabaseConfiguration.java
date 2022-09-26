package it.cnr.si.config;

import it.cnr.si.config.liquibase.AsyncSpringLiquibase;
import liquibase.integration.spring.SpringLiquibase;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.auditing.DateTimeProvider;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Overrrides DatabaseConfiguration in sprint-core
 */

@Configuration
public class DatabaseConfiguration {

    private final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

    @Inject
    private Environment env;

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
    
    /**
     * Open the TCP port for the H2 database, so it is available remotely.
     *
     * @return the H2 database TCP server
     * @throws SQLException if the server failed to start
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Profile(Constants.SPRING_PROFILE_DEVELOPMENT)
    public Server h2TCPServer() throws SQLException {
        return Server.createTcpServer("-tcp","-tcpAllowOthers");
    }
    
    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties liquibaseProperties) {

        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

        // Use liquibase.integration.spring.SpringLiquibase if you don't want Liquibase to start asynchronously
        SpringLiquibase liquibase = new AsyncSpringLiquibase(new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, env);
        
        if(activeProfiles.contains("oiv"))
            liquibase.setChangeLog("classpath:config/liquibase/oiv/master.xml");
        else if(activeProfiles.contains("cnr"))
            liquibase.setChangeLog("classpath:config/liquibase/cnr/master.xml");
        else if(activeProfiles.contains("iss"))
            liquibase.setChangeLog("classpath:config/liquibase/iss/master.xml");
        else if(activeProfiles.contains("showcase"))
            liquibase.setChangeLog("classpath:config/liquibase/showcase/master.xml");

        liquibase.setDataSource(dataSource);
        liquibase.setContexts(liquibaseProperties.getContexts());
        liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
        liquibase.setDropFirst(liquibaseProperties.isDropFirst());
        if (env.acceptsProfiles(Constants.SPRING_PROFILE_NO_LIQUIBASE)) {
            liquibase.setShouldRun(false);
        } else {
            liquibase.setShouldRun(liquibaseProperties.isEnabled());
            log.debug("Configuring Liquibase");
        }
        return liquibase;
    }
    
    @Bean
    public Hibernate5Module hibernate5Module() {
        return new Hibernate5Module();
    }
}