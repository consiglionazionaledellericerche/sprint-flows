package it.cnr.si.flows.ng.batch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.time.LocalDate;

import org.activiti.spring.boot.AbstractProcessEngineAutoConfiguration;
import org.activiti.spring.boot.DataSourceProcessEngineAutoConfiguration;
import org.activiti.spring.boot.EndpointAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration;
import org.activiti.spring.boot.RestApiAutoConfiguration;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration.JpaConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.mvc.method.annotation.CompletionStageReturnValueHandler;

import com.opencsv.CSVParser;

import it.cnr.si.SprintApp;
import it.cnr.si.config.Constants;
import it.cnr.si.config.DefaultProfileUtil;
import it.cnr.si.config.JHipsterProperties;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.base.PageDto;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;

@ComponentScan
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
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class PopolazioneProfiliAcquistiBatch {

    private static final Logger log = LoggerFactory.getLogger(PopolazioneProfiliAcquistiBatch.class);

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
        
        if (activeProfiles.contains("cnr") && activeProfiles.contains("oiv")) {
            log.error("Non e' possibile eseguire l'applicazione con entrambi i profili 'cnr' e 'oiv'");
            System.exit(1);
        }
        if (!activeProfiles.contains("cnr") && !activeProfiles.contains("oiv")) {
            log.error("Selezionare esattamente un profilo tra 'cnr' e 'oiv'");
            System.exit(1);
        }
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        SpringApplication app = new SpringApplication(PopolazioneProfiliAcquistiBatch.class);
        DefaultProfileUtil.addDefaultProfile(app);
        ConfigurableApplicationContext run = app.run(args);
		Environment env = run.getEnvironment();
        log.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running! Access URLs:\n\t" +
                "Local: \t\thttp://127.0.0.1:{}\n\t" +
                "External: \thttp://{}:{}\n----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                env.getProperty("server.port"),
                InetAddress.getLocalHost().getHostAddress(),
                env.getProperty("server.port"));

        Map<String, String> persone = getPersoneDaFile();
        
        persone.forEach( (username, siglaRuolo) -> {
        	
        	inserisciRuolo(run, username, siglaRuolo);
        	
        	
        });
        

    }

	private static void inserisciRuolo(ConfigurableApplicationContext run, String username, String siglaRuolo) {

		AceService aceSearvice = run.getBean(AceService.class);
		AceBridgeService aceBridgeService = run.getBean(AceBridgeService.class);
		EntitaOrganizzativaWebDto afferenzaUtente = aceBridgeService.getAfferenzaUtente(username);
		String cdsuo = afferenzaUtente.getCdsuo();
		
		List<EntitaOrganizzativaWebDto> eos = aceSearvice.entitaOrganizzativaFind(null, null, cdsuo, LocalDate.now(), null).getItems();
		PersonaWebDto persona = aceSearvice.personaByUsername(username);
		
		eos.forEach(eo -> {
			
			Integer idRuolo = aceSearvice.getRuoloBySigla(siglaRuolo).getId();
			Integer idEo = eo.getId();
			Integer idPersona = persona.getId();

			
			
			
		});
		
		
	}

	private static Map<String, String> getPersoneDaFile() throws IOException {
		
		CSVParser parser = new CSVParser(',');
		
		Stream<String> lines = Files.lines(Paths.get("batch/190627 associazione utenze ruolo per ACE 07 maggio.csv"));
		
		Map<String, String> associazioni = new HashMap<>();
		
		lines.forEach(l -> {
			try {
			
				String[] values = parser.parseLine(l);
				associazioni.put(values[0], values[1]);
			
			} catch (IOException e) {e.printStackTrace();}
		});


		return associazioni;
	}
}





















