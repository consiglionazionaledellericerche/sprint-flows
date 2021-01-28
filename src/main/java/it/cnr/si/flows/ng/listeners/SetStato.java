package it.cnr.si.flows.ng.listeners;

import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;


@Component
public class SetStato implements ActivitiEventListener {
	private static final long serialVersionUID = -56001764662303256L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SetStato.class);
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private Utils utils;



	@Override
	public void onEvent(ActivitiEvent event) {

		String stato;
		if (event.getType() == ActivitiEventType.PROCESS_COMPLETED) {
			stato = (String) runtimeService.getVariable(event.getProcessInstanceId(), "statoFinale");

			if(stato != null)
				utils.updateJsonSearchTerms(event.getExecutionId(), event.getProcessInstanceId(), stato);
			else
				LOGGER.error("Errore nel recupero dello Stato Finale della Pi {} da mettere nel Json nel name al suo completamento", event.getProcessInstanceId());
		} else {
			if (event.getType() == ActivitiEventType.TASK_CREATED) {
				//se la Process Instance non è stata ancora creata (non la vedo con la query da runtimeService) ==> non posso inserire il "name" (lo faccio in FlowsTaskService)
				ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
						.processInstanceId(event.getProcessInstanceId())
						.singleResult();

				if (processInstance != null) {
					stato = ((TaskEntity) ((ActivitiEntityEvent) event).getEntity()).getName();
					utils.updateJsonSearchTerms(event.getExecutionId(), event.getProcessInstanceId(), stato);
				}
			} else if (((HistoricActivityInstanceEntity) ((ActivitiEntityEventImpl) event).getEntity()).getActivityId().contains("end-") ||
					((HistoricActivityInstanceEntity) ((ActivitiEntityEventImpl) event).getEntity()).getActivityId().contains("start")) {
				//sono in un'activity di tipo "finale" (quelle che hanno il prefisso "end-" nel name) o "iniziale"
				stato = ((HistoricActivityInstanceEntity) ((ActivitiEntityEventImpl) event).getEntity()).getActivityName();
				//sono gli aggiornamenti più a rischio di "perdersi" lo stato ==> lo loggo
				LOGGER.info("Setto lo stato ({}) della Process Instance {}", stato, event.getProcessInstanceId());

				utils.updateJsonSearchTerms(event.getExecutionId(), event.getProcessInstanceId(), stato);
			}
		}
	}


	@Override
	public boolean isFailOnException() {
		return false;
	}
}
