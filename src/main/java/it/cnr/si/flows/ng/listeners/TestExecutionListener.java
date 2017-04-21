package it.cnr.si.flows.ng.listeners;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.cnr.si.service.UserService;

@Service("testExecutionListener")
public class TestExecutionListener implements ExecutionListener {

    @Autowired
    private UserService userService;

  @Override
  public void notify(DelegateExecution execution) {
      String initiator = (String) execution.getVariable("initiator");
//      Optional<User> user = userService.getUserWithAuthoritiesByLogin(initiator);
//      user.get().getAuthorities();

  }

}