package it.cnr.si;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.identity.UserResource;
import org.activiti.spring.boot.AbstractProcessEngineAutoConfiguration;
import org.activiti.spring.boot.DataSourceProcessEngineAutoConfiguration;
import org.activiti.spring.boot.EndpointAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration.JpaConfiguration;
import org.activiti.spring.boot.RestApiAutoConfiguration;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;

import it.cnr.si.config.Constants;
import it.cnr.si.config.DefaultProfileUtil;
import it.cnr.si.config.JHipsterProperties;

@ComponentScan
(
//        basePackages = "it.cnr.si"
        //(// ci assicuriamo che activiti rest non carichi le sue classi (in conflitto)
        //
//        includeFilters = {@Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {RestResponseFactory.class}
//                )},
//
//        excludeFilters = {@ComponentScan.Filter(
//                type = FilterType.ASPECTJ,
//                pattern = "org.activiti.rest.service.api.runtime.*"
//                )}
        //
        )
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
        MetricFilterAutoConfiguration.class,
        MetricRepositoryAutoConfiguration.class,
                RestApiAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityAutoConfiguration.UserDetailsServiceConfiguration.class,
        JpaProcessEngineAutoConfiguration.class,
        EndpointAutoConfiguration.class,
        DataSourceProcessEngineAutoConfiguration.class,
        AbstractProcessEngineAutoConfiguration.class,
        JpaProcessEngineAutoConfiguration.class,
        JpaConfiguration.class,
        SprintApp.class
})
@EnableConfigurationProperties({ JHipsterProperties.class, LiquibaseProperties.class })
public class FlowsApp {

    private static final Logger log = LoggerFactory.getLogger(FlowsApp.class);

    @Inject
    private Environment env;
    @Inject
    private ApplicationContext appContext;

    /**
     * Initializes sprint.
     * <p>
     * Spring profiles can be configured with a program arguments --spring.profiles.active=your-active-profile
     * <p>
     * You can find more information on how profiles work with JHipster on <a href="http://jhipster.github.io/profiles/">http://jhipster.github.io/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        log.info("Running with Spring profile(s) : {}", Arrays.toString(env.getActiveProfiles()));
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains(Constants.SPRING_PROFILE_DEVELOPMENT) && activeProfiles.contains(Constants.SPRING_PROFILE_PRODUCTION)) {
            log.error("You have misconfigured your application! It should not run " +
                    "with both the 'dev' and 'prod' profiles at the same time.");
        }
        if (activeProfiles.contains(Constants.SPRING_PROFILE_DEVELOPMENT) && activeProfiles.contains(Constants.SPRING_PROFILE_CLOUD)) {
            log.error("You have misconfigured your application! It should not" +
                    "run with both the 'dev' and 'cloud' profiles at the same time.");
        }

    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments
     * @throws UnknownHostException if the local host name could not be resolved into an address
     */
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplication(FlowsApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        log.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running! Access URLs:\n\t" +
                "Local: \t\thttp://127.0.0.1:{}\n\t" +
                "External: \thttp://{}:{}\n----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                env.getProperty("server.port"),
                InetAddress.getLocalHost().getHostAddress(),
                env.getProperty("server.port"));

    }

}

