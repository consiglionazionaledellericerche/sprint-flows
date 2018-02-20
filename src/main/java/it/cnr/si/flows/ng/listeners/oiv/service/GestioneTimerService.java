package it.cnr.si.flows.ng.listeners.oiv.service;


import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import it.cnr.si.flows.ng.resource.FlowsUserResource.SearchResult;

import static it.cnr.si.flows.ng.utils.Utils.TASK_EXECUTOR;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;



@Service
public class GestioneTimerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GestioneTimerService.class);

    @Autowired
    protected ManagementService managementService;
    @Autowired
    private RestResponseFactory restResponseFactory;
    
    @Inject
    private RepositoryService repositoryService;
    
    public List getTimer(String processInstanceId, String timerId) throws IOException, ParseException {

    	//ManagementService managementService = new ManagementService();
    	
    	
//      TIMER
        List<Job> timerJobs = managementService.createJobQuery()
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
            }
        }


        return timerJobs;
    }
    
    
	public void setTimer(String processInstanceId, String timerId) throws IOException, ParseException  {


				LOGGER.debug("--- processInstanceId: {} timerId: {}", processInstanceId, timerId);
			}

	}
