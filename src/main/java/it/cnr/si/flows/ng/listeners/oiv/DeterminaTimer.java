package it.cnr.si.flows.ng.listeners.oiv;


import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;

@Component
public class DeterminaTimer implements ExecutionListener {
    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DeterminaTimer.class);



    @Override
    public void notify(DelegateExecution execution) throws Exception {


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
            if (timerName.equals("boundarytimer3")) {
                LOGGER.info("--- DATA FINE PROCEDURA: {}, getId: {}, timerName: {}", job.getDuedate(), job.getId(), timerName);
                execution.setVariable("dataScadenzaTerminiDomanda", job.getDuedate());
            }
        }
        LOGGER.info("------ DATA FINE PROCEDURA: " + execution.getVariable("dataScadenzaTerminiDomanda"));

    }
}
