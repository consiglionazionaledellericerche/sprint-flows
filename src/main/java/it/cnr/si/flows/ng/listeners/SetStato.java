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

		ProcessInstance processInstance = null;
		String stato = "";
		if(runtimeService.hasVariable(event.getProcessInstanceId(), "statoFinaleDomanda"))
			stato = (String) runtimeService.getVariable(event.getProcessInstanceId(), "statoFinaleDomanda");

		if (event.getType() == ActivitiEventType.TASK_CREATED) {
			processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(event.getProcessInstanceId())
					.singleResult();
			// se la Process Instance sta partendo (Non la vedo con la query da runtimeService), ancora devo settare il "name" (lo faccio in FlowsTaskService)
			if (processInstance != null)
				stato = ((TaskEntity) ((ActivitiEntityEvent) event).getEntity()).getName();
		} else if(((HistoricActivityInstanceEntity)((ActivitiEntityEventImpl) event).getEntity()).getActivityId().contains("end-")){
			processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(event.getProcessInstanceId())
					.singleResult();
			//sono in un'activity di tipo "finale" (quelle che hanno il prefisso "end-" nel name)
			stato = ((HistoricActivityInstanceEntity)((ActivitiEntityEventImpl) event).getEntity()).getActivityName();
			LOGGER.info("Setto lo stato finale ({}) della Process Instance {}", stato, processInstance.getId());
		}

		//todo: aggiorna il name del flusso (json con stato, descrizione, ecc.)
		utils.updateJsonSearchTerms(event.getExecutionId(), event.getProcessInstanceId(), stato);
	}


	@Override
	public boolean isFailOnException() {
		return false;
	}
}
