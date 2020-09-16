package it.cnr.si.flows.ng.config;

import com.zaxxer.hikari.HikariDataSource;
import it.cnr.si.flows.ng.service.FlowsRuntimeService;
import org.activiti.engine.*;
import org.activiti.engine.impl.bpmn.data.ItemInstance;
import org.activiti.engine.impl.bpmn.webservice.MessageInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.impl.variable.*;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.PlatformTransactionManager;

import javax.naming.ConfigurationException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl.DEFAULT_GENERIC_MAX_LENGTH_STRING;

@Configuration
public class FlowsProcessEngineConfigurations {

    private static final String ACTIVITI_VERSION = "5.22.0";

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsProcessEngineConfigurations.class);
    private static final int VARIABLE_LIMIT = 200000;


    @Value("${cnr.activiti.diagram-font}")
    private String diagramFont;

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private HikariDataSource dataSource;
    @Autowired
    private ApplicationContext appContext;

    @Bean
    public SpringProcessEngineConfiguration getProcessEngineConfiguration() throws ConfigurationException {

        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();

        // ci assicuriamo che l'engine sia nel contesto giusto (senno' se ne crea uno suo)
        conf.setApplicationContext(appContext);

        // il DataSource configurato da JHipster/Sprint
        int oldPoolSize = dataSource.getMaximumPoolSize();
        dataSource.setMaximumPoolSize(50);
        LOGGER.info("Imposto il dataSource con massimo numero di connessioni: "+ dataSource.getMaximumPoolSize() + "(era: "+ oldPoolSize +")");
        conf.setDataSource(dataSource);
        conf.setTransactionManager(transactionManager);
        conf.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        //         configurare il font in cnr.activiti.diagram-font, solo se e' installato
        if ( Arrays.asList(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
                .stream()
                .anyMatch(f -> f.equals(diagramFont))) {
            LOGGER.info("Font {} trovato, imposto per i diagrammi", diagramFont);
            try {
                conf.setActivityFontName(diagramFont);
                conf.setAnnotationFontName(diagramFont);
                conf.setLabelFontName(diagramFont);
            } catch (Exception e) {
                LOGGER.warn("Settaggio del Font {} ha dato errore", diagramFont);
            }
        } else {
            LOGGER.warn("Font {} non trovato, torno al default", diagramFont);
        }

        // async migliora le prestazioni, in particolare con tanti utenti
        // conf.setAsyncExecutorActivate(true);
        // migliorano un poco le prestazioni della search  (0,5 secondi)
        // conf.setAsyncExecutorEnabled(true);
//        il default è 10
        // conf.setAsyncExecutorMaxPoolSize(200);

        // FULL serve per la storia dei documenti
        conf.setHistoryLevel(HistoryLevel.FULL);
        //Serve per recuperare molte process istances/task nelle search dell'app (si riferisce al numero di variabili recuperabili nelle query (default 20000))
        conf.setHistoricProcessInstancesQueryLimit(VARIABLE_LIMIT);
        conf.setHistoricTaskQueryLimit(VARIABLE_LIMIT);

        // abbiamo implementato delle query custom
        // @See it.cnr.si.flows.ng.repository.FlowsHistoricProcessInstanceQuery.java

        // Check di sicurezza.
        // se abbiamo aggiornato la versione di activiti,
        // vanno aggiornati anche il mapper e la classe Query e bumpata la versione qui a mano
        if (!conf.getClass().getPackage().getImplementationVersion().equals(ACTIVITI_VERSION))
            throw new ConfigurationException("La versione di Activiti non e' supportata, aggiornare le classi che estendono Historic Query "
                                                     + "e bumpare la versione");

        Set<String> customXmlBatisMappers = new HashSet<>();
        customXmlBatisMappers.add("mapper/FlowsHistoricProcessInstanceMapper.xml");
        conf.setCustomMybatisXMLMappers(customXmlBatisMappers);

        FlowsRuntimeService runtimeService = new FlowsRuntimeService();
        conf.setRuntimeService(runtimeService);

        return conf;
    }

    @Bean(name = "processEngine")
    @Primary
    public ProcessEngine getProcessEngine(
            SpringProcessEngineConfiguration conf) throws Exception {
        //modifica per il flusso test-timer
        conf.setJobExecutorActivate(false);
        conf.setMaxLengthStringVariableType(10000000);

        //IMPORTANTE: aggiungo un nuovo tipo di dato specifico SOLO SE è NON VERRA' MODIFICATO (per non creare problemi al DB)
        // https://blog.progs.be/628/activiti-variables-json
        VariableTypes variableTypes = new DefaultVariableTypes();

        //Aggiungo il tipo json nel db (per le variabili di tipo json, se si riesce ad usare dovrebbe gestirle meglio delle stringhe e degli stream)
        variableTypes.addType(new JsonType(DEFAULT_GENERIC_MAX_LENGTH_STRING, conf.getObjectMapper()));
        variableTypes.addType(new LongJsonType(DEFAULT_GENERIC_MAX_LENGTH_STRING + 1, conf.getObjectMapper()));
        variableTypes.addType(new LongStringType(DEFAULT_GENERIC_MAX_LENGTH_STRING));

        variableTypes.addType(new NullType());
        variableTypes.addType(new StringType(DEFAULT_GENERIC_MAX_LENGTH_STRING));
        variableTypes.addType(new BooleanType());
        variableTypes.addType(new ShortType());
        variableTypes.addType(new IntegerType());
        variableTypes.addType(new LongType());
        variableTypes.addType(new DateType());
        variableTypes.addType(new DoubleType());
        variableTypes.addType(new UUIDType());
        // Risolvono il problema delle variabili "troppo" lunghe (ad es.: "valutazioneEsperienze_json")
        variableTypes.addType(new ByteArrayType());
        variableTypes.addType(new SerializableType());
        variableTypes.addType(new CustomObjectType("item", ItemInstance.class));
        variableTypes.addType(new CustomObjectType("message", MessageInstance.class));

        conf.setVariableTypes(variableTypes);

        ProcessEngineFactoryBean factory = new ProcessEngineFactoryBean();
        factory.setApplicationContext(appContext);
        factory.setProcessEngineConfiguration(conf);

        return factory.getObject();
    }
    
    @Bean(name = "archiveProcessEngine")
    public ProcessEngine getArchiveProcessEngine() throws Exception {
        
        StandaloneProcessEngineConfiguration conf = new StandaloneProcessEngineConfiguration();

        // ci assicuriamo che l'engine sia nel contesto giusto (senno' se ne crea uno suo)
        // conf.setApplicationContext(appContext);

        // il DataSource configurato da JHipster/Sprint
//        conf.setDataSource(dataSource);
        conf.setJdbcDriver("org.postgresql.Driver");
        conf.setJdbcUrl("jdbc:postgresql://localhost:5432/alfresco");
        conf.setJdbcUsername("alfprod");
        conf.setJdbcPassword("alfprodpw");
        conf.setDatabaseType(ProcessEngineConfigurationImpl.DATABASE_TYPE_POSTGRES);        
        
        conf.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        //Serve per recuperare molte process istances/task nelle search dell'app (si riferisce al numero di variabili recuperabili nelle query (default 20000))
        conf.setHistoricProcessInstancesQueryLimit(VARIABLE_LIMIT);
        conf.setHistoricTaskQueryLimit(VARIABLE_LIMIT);

        ProcessEngineFactoryBean factory = new ProcessEngineFactoryBean();
        factory.setApplicationContext(appContext);
        factory.setProcessEngineConfiguration(conf);

        return factory.getObject();
    }

    @Bean
    public RepositoryService getRepositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService getRuntimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public FormService getFormService(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }

    @Bean
    public HistoryService getHistoryService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public TaskService getTaskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public IdentityService getIdentityService(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }

    @Bean
    public ManagementService getManagementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    @Bean
    public RestResponseFactory getRestResponseFactory() {
        return new RestResponseFactory();
    }

    @Bean
    public ProcessDiagramGenerator getProcessDiagramGenerator(ProcessEngine processEingine) {
        return processEingine.getProcessEngineConfiguration().getProcessDiagramGenerator();
    }

    /*
     * Voglio che il JobExecutor parta soltanto dopo l'avvio di tutto l'ambaradam
     */
    @EventListener
    public void onApplicationEvent(ContextStartedEvent event) {

        LOGGER.info("Increment counter "+ event);
        ProcessEngines.getDefaultProcessEngine().getProcessEngineConfiguration().getJobExecutor().start();
    }
}