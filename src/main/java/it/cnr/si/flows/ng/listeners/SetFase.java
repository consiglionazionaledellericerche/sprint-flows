package it.cnr.si.flows.ng.listeners;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.fase;



@Component
public class SetFase implements ActivitiEventListener {
	private static final long serialVersionUID = -56001764662303256L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SetFase.class);
	@Inject
	private RuntimeService runtimeService;
	@Inject
	private HistoryService historyService;




	@Override
	public void onEvent(ActivitiEvent event) {

		ProcessInstance processInstance = null;
		String nomeFase = "";

		if (event.getType() == ActivitiEventType.TASK_CREATED) {//quando il listener viene richiamato la Processi Instances deve ancora finire definitivamente (è nel Task finale)
			processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(event.getProcessInstanceId())
					.singleResult();
			// se la Process Instance sta partendo (Non la vedo con la query da runtimeService), ancora devo settare il "name" (lo faccio in FlowsTaskService)
			if (processInstance != null)
				nomeFase = ((TaskEntity) ((ActivitiEntityEvent) event).getEntity()).getName();
		} else if(((HistoricActivityInstanceEntity)((ActivitiEntityEventImpl) event).getEntity()).getActivityId().contains("end-")){
			processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(event.getProcessInstanceId())
					.singleResult();
			//sono in un'activity di tipo "finale" (quelle che hanno il prefisso "end-" nel name)
			String appo = ((HistoricActivityInstanceEntity)((ActivitiEntityEventImpl) event).getEntity()).getActivityId();
			nomeFase = appo.contains("end-") ? appo.substring(4) : appo;
		}

		//All'avvio del flusso ancora non ho settato il name della Process Instances ==> NON LO SETTO QUI ma in FlowsTaskService
		if(processInstance != null) {
			JSONObject json = new JSONObject(processInstance.getName());
			//Rimuovo la VECCHIA fase
			json.remove(fase.name());
			//Aggiungo la NUOVA fase
			json.put(fase.name(), nomeFase);

			LOGGER.info("Setto il nuovo json nel \"nome\" della Process Instance: {}", json);
			runtimeService.setProcessInstanceName(event.getProcessInstanceId(), json.toString());
		}
	}


	@Override
	public boolean isFailOnException() {
		// TODO Auto-generated method stub
		return false;
	}
}
