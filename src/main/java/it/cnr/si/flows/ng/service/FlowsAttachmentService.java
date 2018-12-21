package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.security.SecurityUtils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.cnr.si.flows.ng.utils.Enum.Azione.*;
import static it.cnr.si.flows.ng.utils.Enum.Stato.PubblicatoTrasparenza;
import static it.cnr.si.flows.ng.utils.Enum.Stato.PubblicatoUrp;
import static it.cnr.si.flows.ng.utils.MimetypeUtils.getMimetype;

@Service
public class FlowsAttachmentService {

	public static final String USER_SUFFIX = "_username";
	public static final String STATO_SUFFIX = "_stato";
	public static final String FILENAME_SUFFIX = "_filename";
	public static final String MIMETYPE_SUFFIX = "_mimetype";
	public static final String ARRAY_SUFFIX_REGEX = "\\[\\d+\\]";
    public static final String NUMERI_PROTOCOLLO = "numeriProtocollo";
    public static final String NUMERI_PROTOCOLLO_SEPARATOR = "::";


    public static final String[] SUFFIXES = new String[] {USER_SUFFIX, STATO_SUFFIX, FILENAME_SUFFIX, MIMETYPE_SUFFIX};

	private static final Logger LOGGER = LoggerFactory.getLogger(FlowsAttachmentService.class);

	@Autowired
	private TaskService taskService;
	@Autowired
	private RuntimeService runtimeService;
	@Inject
	private HistoryService historyService;
	@Inject
	private FlowsAttachmentService attachmentService;

	/**
	 * Servizio che trasforma i multipart file in FlowsAttachment
	 * per il successivo salvataggio sul db
	 *
	 */
	public void extractAttachmentVariables(MultipartHttpServletRequest req, Map<String, Object> data) throws IOException {
		Map<String, FlowsAttachment> attachments = new HashMap<>();
		String taskId, taskName;

		if (req.getParameter("taskId") != null) {
			taskId = (String) req.getParameter("taskId");
			taskName = taskService.createTaskQuery().taskId(taskId).singleResult().getName();
		} else {
			taskId = "start";
			taskName = "Avvio del flusso";
		}

		List<String> nomiFileDaInserire = Collections.list(req.getParameterNames()).stream()
				.filter(name -> name.endsWith("_aggiorna"))
                .filter(name -> "true".equals(req.getParameter(name)) )
				.map(name -> name.replace("_aggiorna", ""))
				.collect(Collectors.toList());

		for (String fileName : nomiFileDaInserire ) {
			FlowsAttachment att = extractSingleAttachment(req, taskId, taskName, fileName, data);
			attachments.put(fileName, att);
		}

		data.putAll(attachments);

        String protocolliUniti = mergeProtocolli(attachments, taskId);
        data.put(NUMERI_PROTOCOLLO, protocolliUniti);
	}

    public FlowsAttachment extractSingleAttachment(MultipartHttpServletRequest req, String taskId, String taskName, String fileName, Map<String, Object> data) throws IOException {

		LOGGER.info("inserisco come variabile il file {}", fileName);
		boolean nuovo = taskId.equals("start") || taskService.getVariable(taskId, fileName) == null;
        String username = SecurityUtils.getCurrentUserLogin();
        FlowsAttachment att = null;

        if (nuovo)
            att = new FlowsAttachment();
        else
		    att = taskService.getVariable(taskId, fileName, FlowsAttachment.class);

		MultipartFile file = req.getFile(fileName + "_data");

		setAttachmentProperties(file, taskId, taskName, fileName, data, nuovo, username, att);

		return att;
	}

	public static void setAttachmentProperties(MultipartFile file, String taskId, String taskName, String fileName, Map<String, Object> data, boolean nuovo, String username, FlowsAttachment att) throws IOException {

		att.setName(fileName);
		att.setTime(new Date());
		att.setTaskId(taskId);
		att.setTaskName(taskName);
		att.setUsername(username);
		if (file != null) {
			att.setFilename(file.getOriginalFilename());
			att.setMimetype(getMimetype(file));
			att.setBytes(file.getBytes());
		}

		att.setLabel(                  String.valueOf(data.remove(fileName+"_label")));
		att.setPubblicazioneUrp(		"true".equals(data.remove(fileName+"_pubblicazioneUrp")));
		att.setPubblicazioneTrasparenza("true".equals(data.remove(fileName+"_pubblicazioneTrasparenza")));
		att.setProtocollo(				"true".equals(data.remove(fileName+"_protocollo")));

		if (att.isProtocollo()) {
			att.setDataProtocollo(  String.valueOf(data.remove(fileName+"_dataProtocollo")));
			att.setNumeroProtocollo(String.valueOf(data.remove(fileName+"_numeroProtocollo")));
		} else {
			att.setDataProtocollo(null);
			att.setNumeroProtocollo(null);
		}

		if (nuovo) {
			att.setAzione(Caricamento);
		} else {
			att.setAzione(Aggiornamento);
		}
	}

	public void saveAttachment(DelegateExecution execution, String variableName, FlowsAttachment att) {

        att.setTime(new Date());
        att.setTaskName(execution.getCurrentActivityName());
        att.setTaskId((String) execution.getVariable("taskId"));

        execution.setVariable(variableName, att);
    }


    /**
     * Salva gli attachment di un Process Instance NON dai listners ma dai service
     * (NON ha bisogno del DelegateExecution).
     * *
     *
     * @param variableName Nome della "tipologia" dell'allegato ("rigetto", "carta d'identità", cv", ecc.)
     * @param att          l'attachment vero e proprio
     * @param taskId       l'id del task in cui viene "allegato" il documento
     */
    public void saveAttachment(String variableName, FlowsAttachment att, String taskId) {
        att.setTime(new Date());
        att.setTaskId(taskId);
        Task task = taskService.createTaskQuery().active().taskId(taskId).singleResult();
        att.setTaskName(task.getName());

        runtimeService.setVariable(task.getExecutionId(), variableName, att);
    }

	@Deprecated // TODO
	public void saveAttachmentInArray(DelegateExecution execution, String arrayName, FlowsAttachment att) {

		att.setTime(new Date());
		att.setTaskName(execution.getCurrentActivityName());
		att.setTaskId( (String) execution.getVariable("taskId"));

		int nextIndex = getNextIndexByProcessInstanceId(execution.getId(), arrayName);

		execution.setVariable(arrayName + nextIndex, att);
	}

	/**
	 * Se ho degli attachments multipli (per esempio allegati[0])
	 * Ho bisogno di salvarli con nomi univoci
	 * (per poter aggiornare gli allegati gia' presenti (es. allegato[0] e allegato[1]) e caricarne di nuovi (es. allegato[2])
	 * Per cui, se sto aggiornando un file, vado dritto col nomefile (es. allegato[1])
	 * invece se ne sto caricando uno nuovo, ho bisogno di sapere l'ultimo indice non ancora utilizzato
	 */


	public int getNextIndexByProcessInstanceId(String processInstanceId, String fileName) {
		int index = 0;
		String variableName = fileName + index;
		while ( runtimeService.hasVariable(processInstanceId, variableName) == true ) {
			variableName = fileName + (++index);
		}
		return index;
	}

	public Map<String, FlowsAttachment> getAttachementsForProcessInstance(@PathVariable("processInstanceId") String processInstanceId) {
		Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInstanceId)
				.includeProcessVariables()
				.singleResult()
				.getProcessVariables();

		return processVariables.entrySet().stream()
				.filter(e -> e.getValue() instanceof FlowsAttachment)
				.collect(Collectors.toMap(k -> k.getKey(), v -> ((FlowsAttachment) v.getValue())));
	}
	
	public Map<String, FlowsAttachment> getCurrentAttachments(DelegateExecution execution) {

		Map<String, FlowsAttachment> attachments = new HashMap<>();

		for (Entry<String, Object> entry : execution.getVariables().entrySet()) 
			if (entry.getValue() instanceof FlowsAttachment)
				attachments.put(entry.getKey(), (FlowsAttachment) entry.getValue());

		return attachments;

	}

	public Map<String, FlowsAttachment> getAttachementsForProcessDefinitionKey(@PathVariable("processDefinitionKey") String processDefinitionKey) {
		Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processDefinitionKey)
				.includeProcessVariables()
				.singleResult()
				.getProcessVariables();

		return processVariables.entrySet().stream()
				.filter(e -> e.getValue() instanceof FlowsAttachment)
				.collect(Collectors.toMap(k -> k.getKey(), v -> ((FlowsAttachment) v.getValue())));
	}	
	
	
	public void setPubblicabileTrasparenza(DelegateExecution execution, String nomeFile, Boolean flagPubblicazione) {
 		Map<String, FlowsAttachment> attachmentList = getCurrentAttachments(execution);
		FlowsAttachment att = attachmentList.get(nomeFile);
		if (att != null) {
			if (flagPubblicazione) {
                att.setAzione(PubblicazioneTrasparenza);
                att.addStato(PubblicatoTrasparenza);
                att.setPubblicazioneTrasparenza(false);
			} else {
                att.setAzione(RimozioneDaPubblicazioneTrasparenza);
                att.removeStato(PubblicatoTrasparenza);
                att.setPubblicazioneTrasparenza(false);
			}
			saveAttachmentFuoriTask(execution.getProcessInstanceId(), nomeFile, att);
		}
	}	
	
	public void setPubblicabileUrp(DelegateExecution execution, String nomeFile, Boolean flagPubblicazione) {
 		//Map<String, FlowsAttachment> attachmentList = attachmentService.getAttachementsForProcessInstance(processInstanceId);
 		Map<String, FlowsAttachment> attachmentList = getCurrentAttachments(execution);
 		
		FlowsAttachment att = attachmentList.get(nomeFile);
		if (att != null) {
			if (flagPubblicazione) {
                att.setAzione(PubblicazioneUrp);
                att.addStato(PubblicatoUrp);
                att.setPubblicazioneUrp(false);
			} else {
                att.setAzione(RimozioneDaPubblicazioneUrp);
                att.removeStato(PubblicatoUrp);
                att.setPubblicazioneUrp(false);
			}
			saveAttachmentFuoriTask(execution.getProcessInstanceId(), nomeFile, att);
		}
	}
	
	public void setPubblicabileTrasparenzaByProcessInstanceId(String processInstanceId, String nomeVariabileFile, Boolean flagPubblicazione) {
		FlowsAttachment att = (FlowsAttachment) runtimeService.getVariable(processInstanceId, nomeVariabileFile);
		if (att != null) {
			if (flagPubblicazione) {
                att.setAzione(PubblicazioneTrasparenza);
                att.addStato(PubblicatoTrasparenza);
                att.setPubblicazioneTrasparenza(false);
			} else {
                att.setAzione(RimozioneDaPubblicazioneTrasparenza);
                att.removeStato(PubblicatoTrasparenza);
                att.setPubblicazioneTrasparenza(false);
			}
			saveAttachmentFuoriTask(processInstanceId, nomeVariabileFile, att);
		}
	}	
	
	public void setPubblicabileUrpByProcessInstanceId(String processInstanceId, String nomeVariabileFile, Boolean flagPubblicazione) {
		FlowsAttachment att = (FlowsAttachment) runtimeService.getVariable(processInstanceId, nomeVariabileFile);
		if (att != null) {
			if (flagPubblicazione) {
                att.setAzione(PubblicazioneUrp);
                att.addStato(PubblicatoUrp);
                att.setPubblicazioneUrp(false);
			} else {
                att.setAzione(RimozioneDaPubblicazioneUrp);
                att.removeStato(PubblicatoUrp);
                att.setPubblicazioneUrp(false);
			}
			saveAttachmentFuoriTask(processInstanceId, nomeVariabileFile, att);
		}
	}
	
	public void saveAttachmentFuoriTask(String executionId, String nomeVariabileFile, FlowsAttachment att) {
		att.setUsername(SecurityUtils.getCurrentUserLogin());
		att.setTime(new Date());
		att.setTaskName("Fuori task");

		runtimeService.setVariable(executionId, nomeVariabileFile, att);
	}

    public String mergeProtocolli(Map<String, FlowsAttachment> attachments, String taskId) {
        List<String> numeriProtocollo = attachments.entrySet().stream()
                .map(key -> key.getValue())
                .map(FlowsAttachment::getNumeroProtocollo)
                .collect(Collectors.toList());

        String vecchiNumeriProtocollo = null;
        if (!taskId.equals("start")) {
            String processId = taskService.createTaskQuery().taskId(taskId).singleResult().getProcessInstanceId();
            vecchiNumeriProtocollo = runtimeService.getVariable(processId, NUMERI_PROTOCOLLO, String.class);
        }

        return mergeProtocolli(vecchiNumeriProtocollo, numeriProtocollo);
    }

    public static String addProtocollo(String vecchiProtocolli, String nuovoProtocollo) {
	    return mergeProtocolli(vecchiProtocolli, Arrays.asList(nuovoProtocollo));
    }

    public static String mergeProtocolli(String vecchiProtocolli, List<String> nuoviProtocolli) {

	    Stream<String> protocolliStream = nuoviProtocolli.stream();

        if (vecchiProtocolli != null) {
            Stream<String> vecchiProtocolliStream = Arrays.stream(vecchiProtocolli.split(NUMERI_PROTOCOLLO_SEPARATOR));
            protocolliStream = Stream.concat(vecchiProtocolliStream, protocolliStream);
        }

        return protocolliStream
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(NUMERI_PROTOCOLLO_SEPARATOR));
    }
}
