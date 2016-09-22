package it.cnr.si.flows.ng.config;

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
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.history.HistoryLevel;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.rest.common.application.ContentTypeResolver;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;


@Configuration
@ComponentScan(basePackages = {"org.activiti.rest",}, 
//    excludeFilters = {@ComponentScan.Filter(
//        type = FilterType.ASSIGNABLE_TYPE,
//        value = {UserResource.class}  
//        )}
//    ,
    includeFilters = {@ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = {RestResponseFactory.class, ContentTypeResolver.class}
        )
    },
    nameGenerator = ActivitiBeanNameGenerator.class)
public class FlowsConfigurations {

    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private HikariDataSource dataSource;

    @Bean
    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        SpringProcessEngineConfiguration conf = new SpringProcessEngineConfiguration();
        conf.setDataSource(dataSource);
        conf.setTransactionManager(transactionManager);
        conf.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        conf.setHistoryLevel(HistoryLevel.FULL);
        
        return conf;
    }

    @Bean(name= {"processEngine", "engine"})
    public ProcessEngine getProcessEngine() throws Exception {
         ProcessEngineFactoryBean bean = new ProcessEngineFactoryBean();
         bean.setProcessEngineConfiguration(getProcessEngineConfiguration());
         return bean.getObject();
    }

    @Bean
    public RepositoryService getRepositoryService() throws Exception {
        return getProcessEngine().getRepositoryService();

    }


    @Bean
    public RuntimeService getRuntimeService() throws Exception {
        return getProcessEngine().getRuntimeService();
    }
    
    @Bean FormService getFormService() throws Exception {
        return getProcessEngine().getFormService();
    }
    
    @Bean HistoryService getHistoryService() throws Exception {
        return getProcessEngine().getHistoryService();
    }
    
    @Bean TaskService getTaskService() throws Exception {
        return getProcessEngine().getTaskService();
    }
    
    @Bean IdentityService getIdentityService() throws Exception {
        return getProcessEngine().getIdentityService();
    }
    
    @Bean ManagementService getManagementService() throws Exception {
        return getProcessEngine().getManagementService();
    }
    
    @PostConstruct
    public void createDeployments() throws Exception {
        DeploymentBuilder builder = getRepositoryService().createDeployment();
        builder.addClasspathResource("processes/PermessiFerieProcess.bpmn20.xml");
        builder.deploy();
    }

}
