package it.cnr.si.flows.ng.config;

import it.cnr.si.flows.ng.listeners.TaskMailNotificationListener;
import it.cnr.si.flows.ng.listeners.SaveSummaryAtProcessCompletion;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.util.ArrayList;
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

    @PostConstruct
    public void init() throws Exception {
        createDeployments();
        addGlobalListeners();
    }

    private void createDeployments() throws Exception {

        for (Resource resource : appContext.getResources("classpath:processes/*.bpmn*")) {
            LOGGER.info("\n ------- definition " + resource.getFilename());
            List<ProcessDefinition> processes = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKeyLike("%" + resource.getFilename().split("[.]")[0] + "%")
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


        TaskMailNotificationListener mailSender = (TaskMailNotificationListener)
                appContext.getAutowireCapableBeanFactory().createBean(TaskMailNotificationListener.class,
                                                                      AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        runtimeService.addEventListener(mailSender);


    }
}