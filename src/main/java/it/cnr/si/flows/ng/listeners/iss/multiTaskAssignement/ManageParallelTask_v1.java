package it.cnr.si.flows.ng.listeners.iss.multiTaskAssignement;


import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.naming.directory.InvalidAttributesException;

import org.activiti.engine.delegate.Expression;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import it.cnr.si.flows.ng.utils.SecurityUtils;

@Component
@Profile("iss")
public class ManageParallelTask_v1 implements TaskListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ManageParallelTask_v1.class);
	private static final String TASK_PROPOSES = "taskProposes_json";
	private static final String FULL_PROPOSES = "richieste_json";
	private static final String SCOPE_TASK = "scope";
	private static final String ADMIN_PROCESS = "AdminMACCISS";
	private static final Boolean _UNMATCH = false;
	private static final Boolean _MATCH = true;
	

	private Expression faseEsecuzione;

	@Override
	public void notify(DelegateTask execution) {
		
		String currentUser = SecurityUtils.getCurrentUserLogin();
		String processInstanceId =  execution.getProcessInstanceId();
		String executionId =  execution.getId();
		String stato =  execution.getName();
		String assignee = execution.getAssignee();
		
		String faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
		
		LOGGER.info("ProcessInstanceId: "+processInstanceId+" Current User: "+currentUser+" executionId: "+executionId+" stato: "+stato+" fase esecuzione: "+faseEsecuzioneValue+" assignee: "+assignee);
		
		switch(faseEsecuzioneValue){  
			// prepare list at the end of the previously task
			case "task-create": {
				LOGGER.info("task-create phase started at the CREATE of the task");
				
				// OGNI VOLTA che un task dedicato ai dipartimenti per l'inserimento delle proposte di acquisto, 
				// recupero la eventuale lista totale delle richieste, copio nella variabile di lavoro del task solo quelle relative
				// allo stesso dipartimento e aggiungo la variabile nel task. In questo modo sono sicuro che il dipartimento lavora
				// il proprio task solo con le proprie proposte aggiornate all'ultima modifica
				
				String currentTaskCandidate;
				try {
					currentTaskCandidate = getTaskCandidate(execution);
					
					JSONArray ownProposes = filterTaskProposes((String) execution.getVariable(FULL_PROPOSES),currentTaskCandidate,_MATCH);
					if ( !(ownProposes.length()>0)) {
						execution.setVariableLocal(TASK_PROPOSES,"[{}]");
					}else {
						execution.setVariableLocal(TASK_PROPOSES,ownProposes.toString());
					}

				} catch (InvalidAttributesException iae) {
					LOGGER.error("Error during process JSON for proposes: structure malformed",iae.getMessage());
				}
	
			}
			break;
			case "task-complete": {
				
				// OGNI VOLTA CHE COMPLETO UN TASK di inserimento proposte dedicato ad un dipartimento
				// Recupero la lista full delle proposte, elimino quelle inserite precedentemente dallo stesso dipartimento
				// Aggiungo quelle lavorate dal task relativo al dipartimento
				// In questo modo sono sicuro che le richieste totali sono sempre aggiornato e lavoro sempre con la variabile di task relativa al dipartimento
				// evitando di inquinare gli inserimenti degli altri dipartimenti
				
				String currentTaskCandidate;
				try {
					currentTaskCandidate = getTaskCandidate(execution);
					String fullProposes = (String) execution.getVariable(FULL_PROPOSES);
					String taskProposes  = (String) execution.getVariableLocal(TASK_PROPOSES);
					
					JSONArray notOwnProposes = filterTaskProposes(fullProposes,currentTaskCandidate,_UNMATCH);
					
					notOwnProposes    = addingJsonKey(notOwnProposes,SCOPE_TASK,ADMIN_PROCESS);
					JSONArray candidateProposes = addingJsonKey(new JSONArray(taskProposes),SCOPE_TASK,currentTaskCandidate);

					//update taskProposes with scope added
					execution.setVariableLocal(TASK_PROPOSES,candidateProposes.toString());
					
					//recreate FULL propose list with all element up to date
					JSONArray fullProposesUpdated = concatJson(notOwnProposes,candidateProposes);
					execution.setVariable(FULL_PROPOSES,fullProposesUpdated.toString());

				} catch (InvalidAttributesException iae) {
					LOGGER.error("Error during process JSON for proposes: structure malformed",iae.getMessage());
				}
			}
			break;
			default: 
				LOGGER.info("fase esecuzione non gestita: "+faseEsecuzioneValue);
			break;
		} 
	}
	
	private	String getTaskCandidate(DelegateTask execution)  throws InvalidAttributesException {
		Set<IdentityLink> candidates = execution.getCandidates();
		String currentTaskCandidate = null; 
		for(IdentityLink s : candidates){
			if ( s.getUserId()!=null) {
            	currentTaskCandidate = s.getUserId(); 
            	break;
            }
		}
		if(currentTaskCandidate == null) throw new InvalidAttributesException("Not found Candidates for this taskId :"+execution.getTaskDefinitionKey());
		return currentTaskCandidate;
	}
	
	private JSONArray filterTaskProposes(String fullProposes, String currentTaskCandidate,Boolean check) {
		// rinnovo sempre la lista delle proposte da quella full per il proprio dipartimento		
		JSONArray proposesList = new JSONArray();

		if(fullProposes != null) {
			JSONArray fullProposes_json = new JSONArray(fullProposes);
			LOGGER.info(" fullProposes: ",fullProposes_json.toString());
			// per ogni proposta seleziono solo quella appartenente al dipartimento relativo al task in esame
			for ( int i = 0; i < fullProposes_json.length(); i++) {
				try {
					JSONObject element = fullProposes_json.getJSONObject(i);
					if(check == _MATCH) {
						if(element.getString(SCOPE_TASK).equals(currentTaskCandidate)) {
							proposesList.put(element);
						}						
					}
					if(check == _UNMATCH) {
						if(!element.getString(SCOPE_TASK).equals(currentTaskCandidate)) {
							proposesList.put(element);
						}												
					}
				}catch (JSONException e) {
					LOGGER.error("Error during process JSON for proposes: structure malformed - ",e.getMessage(),fullProposes,currentTaskCandidate);
				}
			}	
		}
		return proposesList;
	}
	
	private JSONArray concatJson(JSONArray destination, JSONArray input) {
		if(destination == null) {
			destination = new JSONArray();
		}	
		if(input != null) {
			for(int i = 0; i < input.length(); i++) {
				try {
					JSONObject element = input.getJSONObject(i);
					destination.put(element);
				}catch (JSONException e) {
					LOGGER.error("Errore imprevisto: "+e.getMessage());
				}
			}
		}
		return destination;
	}

	private JSONArray addingJsonKey(JSONArray input,String key,String value) {
		for(int i = 0; i < input.length(); i++) {
			JSONObject element = input.getJSONObject(i);
			try {
				element.get(key);
			}catch (JSONException e) {
                // manage the proposes without scope key (new element from forntend form)
            	element.put(key, value);
			}
		}
		return input;
	}

}

