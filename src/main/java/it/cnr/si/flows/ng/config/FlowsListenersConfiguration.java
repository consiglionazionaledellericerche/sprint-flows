package it.cnr.si.flows.ng.config;

import it.cnr.si.flows.ng.listeners.*;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
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
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.activiti.engine.delegate.event.ActivitiEventType.*;

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
    public void init() throws IOException {
        createDeployments();
        addGlobalListeners();
    }

    private void createDeployments() throws IOException {

        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains("dev") || activeProfiles.contains("unittests") || activeProfiles.contains("showcase")) {

            String dir = null;
            if (activeProfiles.contains("cnr"))
                dir = "cnr";
            else if (activeProfiles.contains("oiv"))
                dir = "oiv";
            else if (activeProfiles.contains("showcase"))
                dir = "showcase";
            else
                System.exit(1);

            for (Resource resource : appContext.getResources("classpath:processes/" + dir + "/*.bpmn*")) {
                LOGGER.info("\n ------- definition {}", resource.getFilename());
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
    }

    /**
     * ATTENZIONE: L'ordine dei listener e' importante
     */
    private void addGlobalListeners() {
        LOGGER.info("Adding Flows Listeners");

        SaveSummaryAtProcessCompletion processEndListener = (SaveSummaryAtProcessCompletion)
                appContext.getAutowireCapableBeanFactory().createBean(SaveSummaryAtProcessCompletion.class,
                        AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        SetStato beanSetStato = (SetStato) appContext.getAutowireCapableBeanFactory()
                .createBean(SetStato.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        MailNotificationListener mailSender = (MailNotificationListener)
                appContext.getAutowireCapableBeanFactory().createBean(MailNotificationListener.class,
                        AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
        VisibilitySetter visibilitySetter = (VisibilitySetter)
                appContext.getAutowireCapableBeanFactory().createBean(VisibilitySetter.class,
                        AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

        AddFlowsAttachmentsListener addAttachments = (AddFlowsAttachmentsListener)
                appContext.getAutowireCapableBeanFactory().createBean(AddFlowsAttachmentsListener.class,
                        AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);

        //quando viene "iniziato un task" e quando viene svolta una qualsiasi attività (serve per la "fine della Process Instance)
        runtimeService.addEventListener(mailSender);
        runtimeService.addEventListener(visibilitySetter);
        runtimeService.addEventListener(addAttachments, PROCESS_STARTED, TASK_COMPLETED);
        runtimeService.addEventListener(processEndListener, PROCESS_COMPLETED);
        runtimeService.addEventListener(beanSetStato, TASK_CREATED, HISTORIC_ACTIVITY_INSTANCE_ENDED);



    }
}