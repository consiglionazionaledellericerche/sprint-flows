package it.cnr.si.flows.ng.listeners;

import it.cnr.si.domain.NotificationRule;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsMailService;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import it.cnr.si.repository.NotificationRuleRepository;
import it.cnr.si.service.MembershipService;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Stream;

@Component
public class MailNotificationListener  implements ActivitiEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailNotificationListener.class);
	public static final List<ActivitiEventType> ACCEPTED_EVENTS = Arrays.asList(ActivitiEventType.TASK_CREATED,
			ActivitiEventType.TASK_ASSIGNED,
			ActivitiEventType.TASK_COMPLETED,
			ActivitiEventType.SEQUENCEFLOW_TAKEN,
			ActivitiEventType.PROCESS_STARTED,
			ActivitiEventType.PROCESS_COMPLETED,
			ActivitiEventType.PROCESS_CANCELLED);

	@Inject
	private FlowsMailService mailService;
	@Inject
	private RuntimeService runtimeService;
	@Autowired(required = false)
	private AceBridgeService aceBridgeService;
	@Inject
	private NotificationRuleRepository notificationService;
	@Inject
	private Environment env;
	@Inject
	private MembershipService membershipService;
	@Inject
	private FlowsProcessInstanceService flowsProcessInstanceService;
	
	

	@Override
	public void onEvent(ActivitiEvent event) {
		ActivitiEventType type = event.getType();

		if (type == ActivitiEventType.TASK_CREATED )
			sendStandardCandidateNotification(event);

		if (ACCEPTED_EVENTS.contains(type) )
			sendRuleNotification(event);
	}

	
	private Map<String, Object> integrateVariables(ActivitiEvent event,  Map<String, Object> variables) {
		String processInstanceId = event.getProcessInstanceId();
		Map<String, Object> map = flowsProcessInstanceService.getProcessInstanceWithDetails(processInstanceId);
        HistoricProcessInstanceResponse processInstance = (HistoricProcessInstanceResponse) map.get("entity");
        String nameVariables = processInstance.getName();
        String businessKey = processInstance.getBusinessKey();
        JSONObject jsonObj = new JSONObject(nameVariables);
        jsonObj.get("fase");

        variables.put("businessKey", businessKey);
        variables.put("stato", jsonObj.get("stato"));
        variables.put("descrizione", jsonObj.get("descrizione"));
        variables.put("titolo", jsonObj.get("titolo"));
        variables.put("fase", jsonObj.get("fase"));
        variables.put("initiator", jsonObj.get("initiator"));
		return variables;
	}
	
	private Map<String, Object> sendStandardCandidateNotification(ActivitiEvent event) {

		String executionId = event.getExecutionId();
		Map<String, Object> variables = runtimeService.getVariables(executionId);
		
		//integro le variabili con quelle conservate nel name del processo
		Map<String, Object> integratedVariables = integrateVariables(event, variables);
		ActivitiEntityEvent taskEvent = (ActivitiEntityEvent) event;
		TaskEntity task = (TaskEntity) taskEvent.getEntity();
		//        String fase = ((TaskEntity)taskEvent.getEntity()).getTaskDefinitionKey();
		//        variables.put("faseUltima", fase);
		//        runtimeService.setVariable(executionId, "faseUltima", fase);

		Set<IdentityLink> candidates = ((TaskEntity)taskEvent.getEntity()).getCandidates();
		if (Arrays.asList(env.getActiveProfiles()).contains("oiv")) {
			candidates.forEach(c -> {
				if (c.getGroupId() != null) {
					List<String> members = membershipService.findMembersInGroup(c.getGroupId());
					members.forEach(m -> {
						mailService.sendFlowEventNotification(FlowsMailService.TASK_ASSEGNATO_AL_GRUPPO_HTML, integratedVariables, task.getName(), m, c.getGroupId());
					});
				}
			});
		} else {

			if (Optional.ofNullable(aceBridgeService).isPresent()) {
				candidates.forEach(c -> {
					if (c.getGroupId() != null) {
						List<String> members = aceBridgeService.getUsersinAceGroup(c.getGroupId());
						members.forEach(m -> {
							mailService.sendFlowEventNotification(FlowsMailService.TASK_ASSEGNATO_AL_GRUPPO_HTML, integratedVariables, task.getName(), m, c.getGroupId());
						});
					}
				});
			}
		}
		return integratedVariables;
	}

	private void sendRuleNotification(ActivitiEvent event) {

		ActivitiEventType type = event.getType();
		String executionId = event.getExecutionId();

		Map<String, Object> variables = runtimeService.getVariables(executionId);
		
		
		//integro le variabili con quelle conservate nel name del processo
		Map<String, Object> integratedVariables = integrateVariables(event, variables);

		// Notifiche personalizzate
		List<NotificationRule> notificationRules;

		String processDefinitionId = event.getProcessDefinitionId();
		String processDefinitionKey = processDefinitionId.split(":")[0];

		switch (type) {
		case TASK_CREATED:
		case TASK_ASSIGNED:
		case TASK_COMPLETED:
			ActivitiEntityEvent taskEvent = (ActivitiEntityEvent) event;
			TaskEntity task = (TaskEntity) taskEvent.getEntity();
			notificationRules = notificationService.findGroupsByProcessIdEventTypeTaskName(processDefinitionKey, type.toString(), task.getTaskDefinitionKey());
			send(integratedVariables, notificationRules, FlowsMailService.TASK_NOTIFICATION, task.getName());
			break;

		case SEQUENCEFLOW_TAKEN:
			notificationRules = notificationService.findGroupsByProcessIdEventType(processDefinitionKey, type.toString());
			send(integratedVariables, notificationRules, FlowsMailService.FLOW_NOTIFICATION, null);
			break;

		case PROCESS_STARTED:
		case PROCESS_COMPLETED:
		case PROCESS_CANCELLED:
			notificationRules = notificationService.findGroupsByProcessIdEventType(processDefinitionKey, type.toString());
			send(integratedVariables, notificationRules, FlowsMailService.PROCESS_NOTIFICATION, null);
			break;

		default:
			// no action
			break;
		}

	}

	/**
	 * Per ogni notification rule invia delle mail
	 * Se la notification rule e' riferita a una persona, manda la mail alla persona contenuta nella variabile recipients
	 * Se la notification rule e' riferita a un gruppo, manda la mail alle persone member dei gruppi contenti nella variabile recipients
	 * @param variables
	 * @param notificationRules
	 * @param nt
	 * @param tn
	 */
	private void send(Map<String, Object> variables, List<NotificationRule> notificationRules, String nt, String tn) {


		notificationRules.stream()
		.forEach(rule -> {
			LOGGER.debug("rule.getRecipients(): {}", rule.getRecipients());

			if (rule.isPersona()) {
				Stream.of(rule.getRecipients().split(","))
				.map(s -> s.trim())
				.forEach(personVariableName -> {
					LOGGER.debug("personVariableName: {}", personVariableName);
					String person = (String) personVariableName;
					mailService.sendFlowEventNotification(nt, variables, tn, person, null);
				});
			} else {
				if (Arrays.asList(env.getActiveProfiles()).contains("oiv")) {
					Stream.of(rule.getRecipients().split(","))
					.map(s -> s.trim())
					.forEach(groupVariableName -> {
						LOGGER.debug("groupVariableName: {}", groupVariableName);
						String groupName = (String) groupVariableName;
						List<String> members = membershipService.findMembersInGroup(groupName);
						members.forEach(member -> {
							mailService.sendFlowEventNotification(nt, variables, tn, member, groupName);
						});
					});
				} else {

					if (Optional.ofNullable(aceBridgeService).isPresent()) {
						Stream.of(rule.getRecipients().split(","))
						.map(s -> s.trim())
						.forEach(groupVariableName -> {
							LOGGER.debug("variables.get(groupVariableName): {}", variables.get(groupVariableName));
							String groupName = (String) variables.get(groupVariableName);
							List<String> members = aceBridgeService.getUsersinAceGroup(groupName);
							members.forEach(member -> {
								mailService.sendFlowEventNotification(nt, variables, tn, member, groupName);
							});
						});
					}
				}

			}
		});
	}


	@Override
	public boolean isFailOnException() {
		// TODO Auto-generated method stub
		return false;
	}
}
