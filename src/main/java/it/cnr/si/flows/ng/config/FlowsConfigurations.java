package it.cnr.si.flows.ng.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
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
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import it.cnr.si.flows.ng.listeners.ActivitiLoggingEventListener;
import it.cnr.si.flows.ng.listeners.FlowsVisibilitySetter;
import it.cnr.si.flows.ng.listeners.TestExecutionListener;
import it.cnr.si.flows.ng.listeners.acquistitrasparenza.StartAcquistiSetGroupsPrototipoSenzaAce;

@Configuration
public class FlowsConfigurations {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsConfigurations.class);

    @Value("${cnr.activiti.diagram-font}")
    private String diagramFont;

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private HikariDataSource dataSource;
    @Autowired
    private ApplicationContext appContext;

    @Bean
    public SpringProcessEngineConfiguration getProcessEngineConfiguration(
            ActivitiLoggingEventListener loggingListener) {

        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();

        // ci assicuriamo che l'engine sia nel contesto giusto (senno' se ne crea uno suo)
        conf.setApplicationContext(appContext);

        // il DataSource configurato da JHipster/Sprint
        conf.setDataSource(dataSource);
        conf.setTransactionManager(transactionManager);
        conf.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        // Event listeners generici
        conf.setEventListeners(Arrays.asList(new FlowsVisibilitySetter()));
        Map<Object, Object> beans = new HashMap<>();
        TestExecutionListener bean = appContext.getBean(TestExecutionListener.class);
        beans.put("testExecutionListener", bean);
        beans.put("startAcquistiSetGroupsPrototipoSenzaAce", getStartAcquistiSetGroupsPrototipoSenzaAce());
        conf.setBeans(beans);

        // configurare il font in cnr.activiti.diagram-font, solo se e' installato
//        if ( Arrays.asList(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
//                .stream()
//                .anyMatch(f -> f.equals(diagramFont))) {
//            LOGGER.info("Font {} trovato, imposto per i diagrammi", diagramFont);
            try {
                conf.setActivityFontName(diagramFont);
                conf.setAnnotationFontName(diagramFont);
                conf.setLabelFontName(diagramFont);
            } catch (Exception e) {
                LOGGER.warn("Settaggio del Font {} ha dato errore", diagramFont);
            }
//        } else {
//            LOGGER.warn("Font {} non trovato, torno al default", diagramFont);
//        }

        // async migliora le prestazioni, in particolare con tanti utenti
        conf.setAsyncExecutorActivate(true);

        // FULL serve per la storia dei documenti
        conf.setHistoryLevel(HistoryLevel.FULL);

        return conf;
    }

    @Bean(name = "startAcquistiSetGroupsPrototipoSenzaAce")
    public StartAcquistiSetGroupsPrototipoSenzaAce getStartAcquistiSetGroupsPrototipoSenzaAce() {
        StartAcquistiSetGroupsPrototipoSenzaAce bean = new StartAcquistiSetGroupsPrototipoSenzaAce();
        appContext.getAutowireCapableBeanFactory().autowireBean(bean);
        return bean;
    }

    @Bean(name = "processEngine")
    public ProcessEngine getProcessEngine(
            SpringProcessEngineConfiguration conf) throws Exception {

        ProcessEngineFactoryBean factory = new ProcessEngineFactoryBean();
        factory.setApplicationContext(appContext);
        factory.setProcessEngineConfiguration(conf);

        return factory.getObject();
    }

    @Bean
    public RepositoryService getRepositoryService(ProcessEngine processEngine) throws Exception {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService getRuntimeService(ProcessEngine processEngine) throws Exception {
        return processEngine.getRuntimeService();
    }

    @Bean FormService getFormService(ProcessEngine processEngine) throws Exception {
        return processEngine.getFormService();
    }

    @Bean HistoryService getHistoryService(ProcessEngine processEngine) throws Exception {
        return processEngine.getHistoryService();
    }

    @Bean TaskService getTaskService(ProcessEngine processEngine) throws Exception {
        return processEngine.getTaskService();
    }

    @Bean IdentityService getIdentityService(ProcessEngine processEngine) throws Exception {
        return processEngine.getIdentityService();
    }

    @Bean ManagementService getManagementService(ProcessEngine processEngine) throws Exception {
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

    @PostConstruct
    public void createDeployments() throws Exception {
        RepositoryService repositoryService = appContext.getBean(RepositoryService.class);

        for (Resource resource : appContext.getResources("classpath:processes/*.bpmn*")) {
            LOGGER.info("\n ------- definition " + resource.getFilename());
            List<ProcessDefinition> processes = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKeyLike("%"+ resource.getFilename().split("[.]")[0] +"%")
                    .list();

            if (processes.size() == 0) {
                DeploymentBuilder builder = repositoryService.createDeployment();
                builder.addInputStream(resource.getFilename(), resource.getInputStream());
                builder.deploy();
            }
        }
    }

}
