package it.cnr.si.flows.ng.converter;

import org.activiti.engine.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rende disponibili i convertitori all'applicazione, 
 * in particolare per il binding dei paramentri nei controller.
 * 
 * @author cristian
 * @author daniele
 */
@Configuration
public class ConverterRegistration {

  @Autowired
  TaskService taskService;
 
  @Bean
  public TaskConverter getTaskConversionService() {
    return new TaskConverter(taskService);
  }
}
