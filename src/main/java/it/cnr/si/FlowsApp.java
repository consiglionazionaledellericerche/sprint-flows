package it.cnr.si;

import it.cnr.si.config.Constants;
import it.cnr.si.config.DefaultProfileUtil;
import it.cnr.si.config.JHipsterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
//import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
//import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

@ComponentScan
@EnableCaching
@SpringBootApplication
@EnableAutoConfiguration(exclude = {
//        MetricFilterAutoConfiguration.class,
//        MetricRepositoryAutoConfiguration.class,
//        SprintApp.class
        WebMvcMetricsAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
})
@EnableConfigurationProperties({ JHipsterProperties.class, LiquibaseProperties.class })
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class FlowsApp {

    private static final Logger log = LoggerFactory.getLogger(FlowsApp.class);

    @Inject
    private Environment env;

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

        long profiles = activeProfiles.stream()
                .filter(p -> p.equals("cnr") || p.equals("oiv") || p.equals("iss") || p.equals("showcase"))
                .count();
        if (profiles != 1) {
            log.error("Selezionare esattamente un profilo tra 'cnr', 'oiv', 'iss' e 'showcase'");
            System.exit(1);
        }
    }

    /**
     * Main method, used to run the application.Refreshed
     *
     * @param args the command line arguments
     * @throws UnknownHostException if the local host name could not be resolved into an address
     */
    public static void main(String[] args) throws UnknownHostException {
        SpringApplication app = new SpringApplication(FlowsApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        ConfigurableApplicationContext appContext = app.run(args);
        appContext.start();

        Environment env = appContext.getEnvironment();
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