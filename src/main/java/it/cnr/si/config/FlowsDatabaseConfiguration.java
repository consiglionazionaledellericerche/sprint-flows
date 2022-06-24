package it.cnr.si.config;

import it.cnr.si.config.liquibase.AsyncSpringLiquibase;
import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;


@Configuration
public class FlowsDatabaseConfiguration {

    private final Logger log = LoggerFactory.getLogger(FlowsDatabaseConfiguration.class);

    @Inject
    private Environment env;


    public SpringLiquibase liquibase(DataSource dataSource, LiquibaseProperties liquibaseProperties) {

        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());

        // Use liquibase.integration.spring.SpringLiquibase if you don't want Liquibase to start asynchronously
        SpringLiquibase liquibase = new AsyncSpringLiquibase();

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
}