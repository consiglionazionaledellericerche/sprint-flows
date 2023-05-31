package it.cnr.si.flows.ng.service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.listeners.AddFlowsAttachmentsListener;
import it.cnr.si.flows.ng.utils.Enum.Stato;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.service.SecurityService;
import it.cnr.si.spring.storage.*;
import it.cnr.si.spring.storage.bulk.StorageFile;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.io.IOUtils;
import org.jfree.util.Log;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	public static final String NUMERI_PROTOCOLLO = "numeriProtocollo";
	public static final String NUMERI_PROTOCOLLO_SEPARATOR = "::";
	@Deprecated
	public static final String ARRAY_SUFFIX_REGEX = "\\[\\d+\\]";


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
	@Inject
	private StoreService storeService;
	@Inject
	private Environment env;
	@Inject
	private AddFlowsAttachmentsListener addFlowsAttachmentsListener;
	@Inject
	private SecurityService securityService;

	/**
	 *
	 * Quando ricevo una MultiPartHTTPRequest e ne ho estratto una Map(String, Object) data
	 * Estraggo i singoli parametri dalla mappa in un {@link FlowsAttachment} att aggiornato, secondo il fileName
	 *
	 * Se passo un att nullo, verra' creato un att nuovo
	 * Se non passo ne' bytes ne' nodeRef, verra' restituito null
	 *
	 * @param att           att
	 * @param data          att
	 * @param taskId        att
	 * @param taskName      att
	 * @param processKey    att
	 * @param fileName      att
	 * @param path          att
	 * @return att nuovo o aggiornato, o null
	 */
	public FlowsAttachment extractSingleAttachment(FlowsAttachment att, Map<String, Object> data, String taskId, String taskName, String processKey, String fileName, String path) {

		LOGGER.info("inserisco come variabile il file {}", fileName);
		byte[] filebytes = (byte[]) data.get(fileName + "_data");
		String originalFilename  = (String) data.get(fileName + "_filename");
		String nodeRef  = (String) data.get(fileName + "_nodeRef");
		boolean nuovoFile = false;
		if (att == null) {
			att = new FlowsAttachment();
			nuovoFile = true;
		}

		setAttachmentProperties(att, taskId, taskName, fileName, data);
		att.setFilename(originalFilename);
		if (filebytes != null) {
			att.setAzione(nuovoFile ? Caricamento : Aggiornamento);
			att.setUrl(saveOrUpdateBytes(filebytes, fileName, originalFilename, processKey, path));
			att.setMimetype(getMimetype(filebytes));
			att.setPath(path);
		} else if (nodeRef != null) {
		    verificaPath(nodeRef, path);
			att.setAzione(linkDaAltraApplicazione);
			att.setUrl(nodeRef);
			att.setMimetype( (String) data.get(fileName + "_mimetype") );
			att.setPath( (String) data.get(fileName + "_path") );
		} 
		else if (!nuovoFile) {
		    att.setAzione(Protocollo);
		} else {
		    LOGGER.warn("File Vuoto: "+ fileName);
            return null;
		}

		return att;
	}


    private void setAttachmentProperties(FlowsAttachment att, String taskId, String taskName, String fileName, Map<String, Object> data) {

		att.setName(fileName);
		att.setTime(new Date());
		att.setTaskId(taskId);
		att.setTaskName(taskName);
		att.setUsername(securityService.getCurrentUserLogin());

		att.setLabel(                  String.valueOf(data.get(fileName+"_label")));
		att.setPubblicazioneUrp(		"true".equals(data.get(fileName+"_pubblicazioneUrp")));
		att.setPubblicazioneTrasparenza("true".equals(data.get(fileName+"_pubblicazioneTrasparenza")));
		att.setProtocollo(				"true".equals(data.get(fileName+"_protocollo")));

		// Questo sovrascriverà il set degli stati già presenti.
		// L'alternativa è quella di aggiungere soltanto
		// ma in quel caso non c'è la possibilità di togliere uno stato.
		// martin 10/12/2020
		if (data.get(fileName+"_stati_json") != null) {
			Set<Stato> statiSet = extractStati((String)data.get(fileName+"_stati_json"));
			att.setStati(statiSet);
		}

		if (att.isProtocollo()) {
			att.setDataProtocollo(      String.valueOf(data.get(fileName+"_dataProtocollo")));
			att.setNumeroProtocollo(    String.valueOf(data.get(fileName+"_numeroProtocollo")));
		} else {
			att.setDataProtocollo(null);
			att.setNumeroProtocollo(null);
		}

	}


	/**
	 * Salva gli attachment di un Process Instance dai listners (e non dai service)
	 * (HA BISOGNO del DelegateExecution).
	 * *
	 * @param execution    l'execution (processInstance) in cui inserire l'allegato
	 * @param variableName Nome della "tipologia" dell'allegato ("rigetto", "carta d'identità", cv", ecc.)
	 * @param att          l'attachment vero e proprio
	 * @param content      il contenuto binario dell'attachment o null per aggiornare solo i metadati
	 */
	public void saveAttachment(DelegateExecution execution, String variableName, FlowsAttachment att, byte[] content) {

		att.setUsername(securityService.getCurrentUserLogin());
		att.setTime(new Date());
		att.setTaskId((String) execution.getVariable("taskId"));
		att.setTaskName(execution.getCurrentActivityName());

		if (content != null) {
			String key = execution.getVariable("key", String.class);
			att.setUrl(saveOrUpdateBytes(content, variableName, att.getFilename(), key, att.getPath()));
		}

		runtimeService.setVariable(execution.getId(), variableName, att);
	}


	/**
	 * Salva gli attachment di un Process Instance NON dai listners ma dai service
	 * (NON ha bisogno del DelegateExecution).
	 * *
	 * @param taskId       l'id del task in cui viene "allegato" il documento
	 * @param variableName Nome della "tipologia" dell'allegato ("rigetto", "carta d'identità", cv", ecc.)
	 * @param att          l'attachment vero e proprio
	 * @param content      il contenuto binario dell'attachment o null per aggiornare solo i metadati
	 */
	public void saveAttachment(String taskId, String variableName, FlowsAttachment att, byte[] content) {

		att.setUsername(securityService.getCurrentUserLogin());
		att.setTime(new Date());
		att.setTaskId(taskId);
		Task task = taskService.createTaskQuery().active().taskId(taskId).singleResult();
		att.setTaskName(task.getName());

		if (content != null) {
			String key = runtimeService.getVariable(task.getExecutionId(), "key", String.class);
			att.setUrl(saveOrUpdateBytes(content, variableName, att.getFilename(), key, att.getPath()));
		}

		runtimeService.setVariable(task.getExecutionId(), variableName, att);
	}

	public void saveAttachmentFuoriTask(String executionId, String variableName, FlowsAttachment att, byte[] content) {

		att.setUsername(securityService.getCurrentUserLogin());
		att.setTime(new Date());
		att.setTaskName("Fuori task");
		if(att.getPath() == null) {
			att.setPath(addFlowsAttachmentsListener.getDefaultPathFascicoloDocumenti(executionId));
		}

		if (content != null) {
			String key = runtimeService.getVariable(executionId, "key", String.class);
			att.setUrl(saveOrUpdateBytes(content, variableName, att.getFilename(), key, att.getPath()));
		}

		runtimeService.setVariable(executionId, variableName, att);
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
	 *
	 * @param fileName          nome File
	 * @param processInstanceId id
	 * @return next calculated index
	 */
	public int getNextIndexByProcessInstanceId(String processInstanceId, String fileName) {
		int index = 0;
		String variableName = fileName + index;
		while ( runtimeService.hasVariable(processInstanceId, variableName) == true ) {
			variableName = fileName + (++index);
		}
		return index;
	}
	
	public List<FlowsAttachment> getAttachmentArray(String processInstanceId, String fileName) {
	    List<FlowsAttachment> result = new ArrayList<>();
	    String regex = fileName+"\\d+";
	    
	    runtimeService.getVariables(processInstanceId).forEach((name, value) -> {
	        if (name.matches(regex) && value instanceof FlowsAttachment)
	            result.add( (FlowsAttachment)value );
	    });
	    
	    return result;
	}

	public Map<String, FlowsAttachment> getCurrentAttachments(DelegateExecution execution) {

		Map<String, FlowsAttachment> attachments = new HashMap<>();

		for (Entry<String, Object> entry : runtimeService.getVariables(execution.getId()).entrySet() )
			if (entry.getValue() instanceof FlowsAttachment)
				attachments.put(entry.getKey(), (FlowsAttachment) entry.getValue());

		return attachments;

	}


	public Map<String, FlowsAttachment> getAttachementsForProcessInstance(String processInstanceId) {
		Map<String, FlowsAttachment> response;
		try {
			Map<String, Object> processVariables = historyService.createHistoricProcessInstanceQuery()
					.processInstanceId(processInstanceId)
					.includeProcessVariables()
					.singleResult()
					.getProcessVariables();

			response = processVariables.entrySet().stream()
					.filter(e -> e.getValue() instanceof FlowsAttachment)
					//                .peek(e -> ((FlowsAttachment) e.getValue()).setBytes(null))
					.collect(Collectors.toMap(k -> k.getKey(), v -> ((FlowsAttachment) v.getValue())));
		}catch (NullPointerException e){
			//se il flusso deve ancora iniziare la query restituisce una NullPointerException quindi restituisco una hashMap vuota
			response = new HashMap<>();
		}
		return response;
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
			saveAttachmentFuoriTask(execution.getProcessInstanceId(), nomeFile, att, null);
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
			saveAttachmentFuoriTask(execution.getProcessInstanceId(), nomeFile, att, null);
		}
	}

	public void setPubblicabileTrasparenza(String processInstanceId, String nomeVariabileFile, Boolean flagPubblicazione) {

		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		setPubblicabileTrasparenza((ExecutionEntity) processInstance, nomeVariabileFile, flagPubblicazione);
	}

	public void setPubblicabileUrp(String processInstanceId, String nomeVariabileFile, Boolean flagPubblicazione) {

		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		setPubblicabileUrp((ExecutionEntity) processInstance, nomeVariabileFile, flagPubblicazione);
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

	public String saveOrUpdateBytes(byte[] bytes, String attachmentName, String fileName, String processKey, String path) {

		if(Utils.isFullPath(path)) {
			attachmentName = path.substring(path.lastIndexOf('/')+1);
			path = path.substring(0, path.lastIndexOf('/'));
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

		StorageFile storageFile = new StorageFile(bais,
				getMimetype(bais),
				attachmentName);

		storageFile.setTitle(fileName);
		storageFile.setDescription(fileName);

		StorageObject so = storeService.restoreSimpleDocument(
				storageFile,
				new ByteArrayInputStream(storageFile.getBytes()),
				storageFile.getContentType(),
				attachmentName,
				path,
				true);

		//        StorageObject updatedSo = storeService.getStorageObjectBykey(
		//                so.<String>getPropertyValue("cmis:objectId").split(";")[0]);

		return so.getPropertyValue("cmis:objectId");
	}

	public InputStream getAttachmentContent(String key) {
		return storeService.getResource(key);
	}

	public byte[] getAttachmentContentBytes(String key) {
		try {
			return IOUtils.toByteArray(getAttachmentContent(key));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream getAttachmentContent(FlowsAttachment att) {
		return getAttachmentContent(att.getUrl());
	}

	public byte[] getAttachmentContentBytes(FlowsAttachment att) {
		return getAttachmentContentBytes(att.getUrl());
	}

	private static Set<Stato> extractStati(String stati) {
		Set<Stato> result = new HashSet<>();
		// Rimuovo '[' e ']'
		if (stati.charAt(0) == '[') stati = stati.substring(1);
		if (stati.charAt(stati.length()-1) == ']') stati = stati.substring(0, stati.length()-1);
		if (stati.length() != 0) {
			String[] statiArray = stati.split(",");
			for (String stato : statiArray) {
				stato = stato.trim();
				if (stato.charAt(0) == '"') stato = stato.substring(1);
				if (stato.charAt(stato.length()-1) == '"') stato = stato.substring(0, stato.length()-1);
				result.add(Stato.valueOf(stato));
			}
		}

		return result;
	}



	public void updateAttachmentPath(String processInstanceId, String nomeFile, String valorePath) {
		Map<String, FlowsAttachment> attachmentList =  attachmentService.getAttachementsForProcessInstance(processInstanceId);
		FlowsAttachment att = attachmentList.get(nomeFile);
		if (att != null) {
			att.setPath(valorePath);
			saveAttachmentFuoriTask(processInstanceId, nomeFile, att, null);
		}
	}
	
	public void updateAttachmentUrl(String processInstanceId, String nomeFile, String valoreUrl) {
		Map<String, FlowsAttachment> attachmentList =  attachmentService.getAttachementsForProcessInstance(processInstanceId);
		FlowsAttachment att = attachmentList.get(nomeFile);
		if (att != null) {
			att.setUrl(valoreUrl);
			saveAttachmentFuoriTask(processInstanceId, nomeFile, att, null);
		}
	}

	
	public void updateAttachmentMimetypeToPDF(String processInstanceId, String nomeFile) {
		Map<String, FlowsAttachment> attachmentList =  attachmentService.getAttachementsForProcessInstance(processInstanceId);
		FlowsAttachment att = attachmentList.get(nomeFile);
		if (att != null) {
			att.setMimetype(com.google.common.net.MediaType.PDF.toString());
			saveAttachmentFuoriTask(processInstanceId, nomeFile, att, null);
		}
	}

	/**
	 * 
	 * @param nodeRef notNull
	 * @param path NotNull
	 * @throws IllegalArgumentException
	 */
    private void verificaPath(String nodeRef, String path) {
        StorageObject so = storeService.getStorageObjectBykey(nodeRef);
        if (so.getPath() != path) {
            LOGGER.warn("Il path dell'allegato ("+ so.getPath() +") non coincide con quello trasmesso("+ path +")");
            // throw new IllegalArgumentException();
        }   
    }
}
