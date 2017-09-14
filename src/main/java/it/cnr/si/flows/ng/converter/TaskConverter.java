package it.cnr.si.flows.ng.converter;

import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Viene utilizzato nel binding dei parametri del controller rest
 * per ottenere direttamente l'istanza del task. 
 * 
 * @author cristian
 * @author daniele
 * @since 2017/09/14
 */
@Component
public class TaskConverter implements Converter<String, Task>{

  private TaskService taskService;
  
  @Autowired
  public TaskConverter(TaskService taskService) {
    this.taskService = taskService; 
  }
  
  @Override
  public Task convert(String source) {
    return taskService.createTaskQuery().taskId(source).includeProcessVariables().singleResult();
  }

}
