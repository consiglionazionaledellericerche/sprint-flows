package it.cnr.si.flows.ng.listeners;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEntityWithVariablesEvent;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.ActivitiSequenceFlowTakenEvent;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.delegate.event.impl.ActivitiProcessCancelledEventImpl;
import org.activiti.engine.delegate.event.impl.ActivitiSequenceFlowTakenEventImpl;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.task.IdentityLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.cnr.si.domain.NotificationRule;
import it.cnr.si.flows.ng.config.MailConfguration;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.FlowsMailService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.repository.NotificationRuleRepository;
import it.cnr.si.service.MembershipService;

@Component
public class MailNotificationListener  implements ActivitiEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MailNotificationListener.class);
	public static final List<ActivitiEventType> ACCEPTED_EVENTS = Arrays.asList(
			ActivitiEventType.TASK_CREATED,
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
	private MailConfguration mailConfguration;


	@Override
	public void onEvent(ActivitiEvent event) {
		ActivitiEventType type = event.getType();

		if (type == ActivitiEventType.TASK_CREATED )
			sendStandardNotification(event);

		if (ACCEPTED_EVENTS.contains(type) )
			sendRuleNotification(event);
	}


	private Map<String, Object> integrateVariables(ActivitiEvent event,  Map<String, Object> variables) {
		switch (event.getType()){
		case PROCESS_STARTED:
			variables.put("stato", ((ExecutionEntity) ((ActivitiEntityWithVariablesEvent) event).getEntity()).getCurrentActivityName());
			break;
		case SEQUENCEFLOW_TAKEN:
			variables.put("stato", ((ActivitiSequenceFlowTakenEventImpl)event).getTargetActivityName());
			variables.put("processInstanceId", ((ActivitiSequenceFlowTakenEventImpl)event).getProcessInstanceId());
			break;
		case TASK_COMPLETED:
		case TASK_ASSIGNED:
		case TASK_CREATED:
			variables.put("stato", ((TaskEntity)((ActivitiEntityEvent)event).getEntity()).getName());
			variables.put("nextTaskId", ((TaskEntity)((ActivitiEntityEvent)event).getEntity()).getId());
			variables.put("processInstanceId", ((TaskEntity)((ActivitiEntityEvent)event).getEntity()).getProcessInstanceId());
			break;
		case PROCESS_CANCELLED:
			variables.put("stato", ((ActivitiEventType)event.getType()).name()  + " - con causa: " + ((ActivitiProcessCancelledEventImpl) event).getCause());				
			break;
		case PROCESS_COMPLETED:
			variables.put("stato", ((ExecutionEntity)((ActivitiEntityEventImpl) event).getEntity()).getActivity().getProperty("name"));
			break;
		default:
			break;
		}

		variables.put("serverUrl", mailConfguration.getMailUrl());

		return variables;
	}

	/**
	 * Questo metodo esegue l'invio delle mail per eventi predefiniti:
	 * 1. E' stato creato un compito di gruppo per uno dei gruppi dell'utente
	 * 2. E' stato creato un compito assegnato direttamente all'utente
	 * 
	 * @param event
	 * @return
	 */
	private void sendStandardNotification(ActivitiEvent event) {

		String executionId = event.getExecutionId();
		Map<String, Object> variables = runtimeService.getVariables(executionId);

		//integro le variabili con quelle conservate nel name del processo
		Map<String, Object> integratedVariables = integrateVariables(event, variables);
		ActivitiEntityEvent taskEvent = (ActivitiEntityEvent) event;
		TaskEntity task = (TaskEntity) taskEvent.getEntity();

		Set<IdentityLink> candidates = ((TaskEntity)taskEvent.getEntity()).getCandidates();

		candidates.forEach(c -> {
			if (c.getGroupId() != null) {
				String[] idStruttura = c.getGroupId().split("@", 2);
				if (!aceBridgeService.getStrutturaById(Integer.parseInt(idStruttura[1])).getCdsuo().equals("000999")) {
					Set<String> members = membershipService.getAllUsersInGroup(c.getGroupId());
					LOGGER.info("Sto inviando mail standard a {} del gruppo {} per il task", members, c.getGroupId(), task.getName());
					members.forEach(m -> {
						mailService.sendFlowEventNotification(FlowsMailService.TASK_ASSEGNATO_AL_GRUPPO, integratedVariables, task.getName(), m, c.getGroupId(), false);
					});
				}
			} else {
                LOGGER.info("Non Sto inviando mail standard  del gruppo {} per il task perché non in GERARCHIA ACE",  c.getGroupId(), task.getName());
			}
		});

		String assignee = ((TaskEntity)taskEvent.getEntity()).getAssignee();

		if (Utils.isNotEmpty(assignee)) {
			LOGGER.info("Sto inviando mail standard all'assegnatario {} per il task",assignee, task.getName());
			mailService.sendFlowEventNotification(FlowsMailService.TASK_IN_CARICO_ALL_UTENTE, integratedVariables, task.getName(), assignee, null, false);
		}
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
			notificationRules = notificationService.findRulesByProcessIdEventTypeTaskName(processDefinitionKey, type.toString(), task.getTaskDefinitionKey());
			send(integratedVariables, notificationRules, FlowsMailService.TASK_NOTIFICATION, task.getName());
			break;

		case SEQUENCEFLOW_TAKEN:
			ActivitiSequenceFlowTakenEvent seqTaken = (ActivitiSequenceFlowTakenEvent) event;
			notificationRules = notificationService.findRulesByProcessIdEventTypeTaskName(processDefinitionKey, type.toString(), seqTaken.getId());
			send(integratedVariables, notificationRules, FlowsMailService.FLOW_NOTIFICATION, null);
			break;

		case PROCESS_STARTED:
		case PROCESS_CANCELLED:
			notificationRules = notificationService.findRulesByProcessIdEventType(processDefinitionKey, type.toString());
			send(integratedVariables, notificationRules, FlowsMailService.PROCESS_NOTIFICATION, null);
			break;

		case PROCESS_COMPLETED:
			notificationRules = notificationService.findRulesByProcessIdEventType(processDefinitionKey, type.toString());
			send(integratedVariables, notificationRules, FlowsMailService.PROCESS_COMPLETED_NOTIFICATION, null);
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

		LOGGER.info("Sto inviando secondo le notification rule :{} ({}, {})", notificationRules, nt, tn);
		notificationRules.stream()
		.forEach(rule -> {
			LOGGER.debug("rule.getRecipients(): {}", rule.getRecipients());

			if (rule.isPersona()) {
				Stream.of(rule.getRecipients().split(","))
				.map(s -> s.trim())
				.forEach(personVariableName -> {
					LOGGER.debug("personVariableName: {}", personVariableName);
					String person = (String) variables.get(personVariableName);
					LOGGER.info("Invio la mail {} all'utente {}", nt, person);
					mailService.sendFlowEventNotification(nt, variables, tn, person, null, true);
				});
			} else {
				if (Arrays.asList(env.getActiveProfiles()).contains("cnr")) {

					if (Optional.ofNullable(aceBridgeService).isPresent()) {
						Stream.of(rule.getRecipients().split(","))
						.map(s -> s.trim())
						.forEach(groupVariableName -> {
							LOGGER.debug("variables.get(groupVariableName): {}", variables.get(groupVariableName));
							String groupName = (String) variables.get(groupVariableName);

							Set<String> members = membershipService.getAllUsersInGroup(groupName);

							LOGGER.info("Invio la mail {} al gruppo {} con utenti {}", nt, groupName, members);
							members.forEach(member -> {
								mailService.sendFlowEventNotification(nt, variables, tn, member, groupName, true);
							});
						});
					}
				} else {
					Stream.of(rule.getRecipients().split(","))
					.map(s -> s.trim())
					.forEach(groupVariableName -> {
						LOGGER.debug("groupVariableName: {}", groupVariableName);
						String groupName = (String) groupVariableName;
						Set<String> members = membershipService.getAllUsersInGroup(groupName);
						LOGGER.info("Invio la mail {} al gruppo {} con utenti {}", nt, groupName, members);
						members.forEach(member -> {
							mailService.sendFlowEventNotification(nt, variables, tn, member, groupName, true);
						});
					});
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
