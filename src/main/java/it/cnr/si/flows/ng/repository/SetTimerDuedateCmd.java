package it.cnr.si.flows.ng.repository;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Job;

/**
 *
 * @author mtrycz + cironepa
 *
 * Questa classe ci serve come estensione della questy che offre Activity
 * Abbiamo voluto usare questo approccio per questioni di performance, ma activiti non forniva tutte le query che ci piaceva avere
 * in particolare per quel che riguarda la visibilita' sui flussi (che salviamo negli IdentityLinks)
 *
 * Un esempio di utilizzo di questa classe e' in {@link it.cnr.si.flows.ng.service.FlowsProcessInstanceService}
 *
 *
 *
 * Questo file e' appaiato con src/main/resources/mapper/FlowsHistoricProcessInstanceMapper.xml
 *
 */

public class SetTimerDuedateCmd implements Command<Void>, Serializable {

	  private static final long serialVersionUID = 1L;

	  Calendar newDate = Calendar.getInstance();
	  private String jobId = null;
	  private Date duedate = newDate.getTime();

	  public SetTimerDuedateCmd(String jobId, Date duedate) {
	    if (jobId == null || jobId.length() < 1) {
	      throw new ActivitiIllegalArgumentException("The job id is mandatory, but '" + jobId + "' has been provided.");
	    }
	    try {
	    	duedate.getTime();
	    }
	    catch (Exception e) {
		      throw new ActivitiIllegalArgumentException("The number of job duedate must be a non-negative Integer, but '" + duedate + "' has been provided.");
	    }
	    this.jobId = jobId;
	    this.duedate = duedate;
	  }

	  public Void execute(CommandContext commandContext) {
	    JobEntity job = commandContext
	            .getJobEntityManager()
	            .findJobById(jobId);
	    if (job != null) {
	      job.setDuedate(duedate);
	      
	      if(commandContext.getEventDispatcher().isEnabled()) {
	      	commandContext.getEventDispatcher().dispatchEvent(
	      			ActivitiEventBuilder.createEntityEvent(ActivitiEventType.ENTITY_UPDATED, job));
	      }
	    } else {
	      throw new ActivitiObjectNotFoundException("No job found with id '" + jobId + "'.", Job.class);
	    }
	    return null;
	  }
}
