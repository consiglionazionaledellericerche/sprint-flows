package it.cnr.si.flows.ng.config;

import it.cnr.si.flows.ng.listeners.MailNotificationListener;
import it.cnr.si.flows.ng.listeners.SaveSummaryAtProcessCompletion;
import it.cnr.si.flows.ng.listeners.VisibilitySetter;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by cirone on 15/06/17.
 */
@Configuration
@AutoConfigureAfter(FlowsProcessEngineConfigurations.class)
public class FlowsListenersConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsListenersConfiguration.class);
    @Inject
    private ApplicationContext appContext;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private Environment env;
    
    @PostConstruct
    public void init() throws Exception {
        createDeployments();
        addGlobalListeners();
    }

    private void createDeployments() throws Exception {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        String dir = null;
        if (activeProfiles.contains("cnr"))
            dir = "cnr";
        else if (activeProfiles.contains("oiv"))
            dir = "oiv";
        else
            System.exit(1);
        
        for (Resource resource : appContext.getResources("classpath:processes/"+ dir +"/*.bpmn*")) {
            LOGGER.info("\n ------- definition " + resource.getFilename());
            List<ProcessDefinition> processes = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(resource.getFilename().split("[.]")[0])
                    .list();

            if (processes.size() == 0) {
                DeploymentBuilder builder = repositoryService.createDeployment();
                builder.addInputStream(resource.getFilename(), resource.getInputStream());
                builder.deploy();
            }
        }
    }

    private void addGlobalListeners() {
        LOGGER.info("Adding Flows Listeners");

        SaveSummaryAtProcessCompletion processEndListener = (SaveSummaryAtProcessCompletion)
                appContext.getAutowireCapableBeanFactory().createBean(SaveSummaryAtProcessCompletion.class,
                                                                      AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        runtimeService.addEventListener(processEndListener, ActivitiEventType.PROCESS_COMPLETED);


        MailNotificationListener mailSender = (MailNotificationListener)
                appContext.getAutowireCapableBeanFactory().createBean(MailNotificationListener.class,
                                                                      AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        runtimeService.addEventListener(mailSender);

        VisibilitySetter visibilitySetter = (VisibilitySetter)
                appContext.getAutowireCapableBeanFactory().createBean(VisibilitySetter.class,
                                                                      AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        runtimeService.addEventListener(visibilitySetter);


    }
}