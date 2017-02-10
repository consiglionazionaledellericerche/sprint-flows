package it.cnr.si.flows.ng.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;



@Configuration
public class FlowsConfigurations {

    private static final Logger log = LoggerFactory.getLogger(FlowsConfigurations.class);

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private HikariDataSource dataSource;
    @Autowired
    private ApplicationContext appContext;

    @Bean
    public SpringProcessEngineConfiguration getProcessEngineConfiguration(
            ActivitiLoggingEventListener loggingListener
            ) {
        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();

        conf.setApplicationContext(appContext);
        conf.setDataSource(dataSource);
        conf.setTransactionManager(transactionManager);
        conf.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        conf.setEventListeners(new ArrayList<ActivitiEventListener>() {{
            add(loggingListener);
            add(new FlowsVisibilitySetter());
        }});

        Map<Object, Object> beans = new HashMap<>();
        TestExecutionListener bean = appContext.getBean(TestExecutionListener.class);
        beans.put("testExecutionListener", bean);
        conf.setBeans(beans);

//        conf.setDbIdentityUsed(false);

        conf.setHistoryLevel(HistoryLevel.AUDIT);

        return conf;
    }

//    @Bean(name= {"processEngine", "engine", "pluto"})
    @Bean
//    @Primary
    public ProcessEngine getProcessEngine(
            SpringProcessEngineConfiguration conf) throws Exception {
        ProcessEngineFactoryBean bean = new ProcessEngineFactoryBean();
        bean.setApplicationContext(appContext);
        bean.setProcessEngineConfiguration(conf);

        return bean.getObject();
//        return processEngineConfiguration.buildProcessEngine();
    }

    //    @Bean(name= {"processEngine", "engine"})
    //    public ProcessEngine processEngine throws Exception {
    //        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();
    //
    //        conf.setDataSource(dataSource);
    //        conf.setTransactionManager(transactionManager);
    //        conf.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    //        conf.setEventListeners(new ArrayList<ActivitiEventListener>() {{
    //            add(new ActivitiLoggingEventListener());
    //            add(new FlowsVisibilitySetter());
    //        }});
    //
    //        conf.setHistoryLevel(HistoryLevel.AUDIT);
    //
    //        conf.getApplicationContext();
    //        return conf.buildProcessEngine();
    //    }

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
    public InitializingBean usersAndGroupsInitializer(final IdentityService identityService) {

        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                Group gruppo = identityService.createGroupQuery().groupId("user").singleResult();
                if (gruppo == null) {
                    Group group = identityService.newGroup("user");

                    group.setName("users");
                    group.setType("security-role");
                    identityService.saveGroup(group);
                }

                User giulio = identityService.createUserQuery().userId("giulio").singleResult();
                if(giulio == null) {
                    User admin = identityService.newUser("giulio");
                    admin.setPassword("giulio");
                    identityService.saveUser(admin);
                }
            }
        };
    }

    @Bean
    public RestResponseFactory getRestResponseFactory() {
        return new RestResponseFactory();
    }

    @PostConstruct
    public void createDeployments() throws Exception {

        log.info("pippo");
        log.info(""+ appContext.getBeanDefinitionCount());
        log.info(""+ Arrays.asList(appContext.getBeanDefinitionNames()));
        log.info(""+ Arrays.asList(appContext.getBeanDefinitionNames()));
//        appContext.getBeanNamesForType();
//        appContext.getBeansOfType();
//        appContext.getBean();

//        DeploymentBuilder builder = getRepositoryService().createDeployment();
//        builder.addClasspathResource("processes/PermessiFerieProcess.bpmn20.xml");
//        builder.deploy();
    }

}
