package it.cnr.si.flows.ng.listeners.oiv.service;


import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;

import java.util.List;


@Service
public class DeterminaTimer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeterminaTimer.class);



    public void getTimer(DelegateExecution execution, String timerId) throws IOException, ParseException {


//      TIMER
        String processInstanceId = execution.getProcessInstanceId();
        List<Job> timerJobs = execution.getEngineServices().getManagementService().createJobQuery()
             	.processInstanceId(processInstanceId)
             	.timers()
             	.list();
        LOGGER.info("TIMERS" + timerJobs);
        for(Job job : timerJobs)
        {
            String timerName = ((TimerEntity) job).getJobHandlerConfiguration()
                    .split(":")[1]
                    .replace("\"", "")
                    .replace("}", "");
            LOGGER.info("getDuedate {}, getId {}, TimerDeclarationImpl {}", job.getDuedate(), job.getId(), timerName);
            if (timerName.equals(timerId)) {
                LOGGER.info("--- DATA FINE PROCEDURA: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
                execution.setVariable("dataScadenzaTerminiDomanda", job.getDuedate());
            }
        }
        LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));

    }
}
