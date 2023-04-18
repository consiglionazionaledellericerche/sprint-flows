package it.cnr.si.flows.ng.service;

import it.cnr.si.firmadigitale.firma.arss.ArubaSignServiceException;
import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;
import it.cnr.si.firmadigitale.firma.arss.stub.SignReturnV2;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.exception.FileFormatException;
import it.cnr.si.flows.ng.exception.TaskFailedException;
import it.cnr.si.flows.ng.service.FlowsFirmaService.FileAllaFirma;

import it.cnr.si.service.SecurityService;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

import static it.cnr.si.flows.ng.service.FlowsFirmaService.NOME_FILE_FIRMA;
import static it.cnr.si.flows.ng.utils.Enum.Azione.Firma;
import static it.cnr.si.flows.ng.utils.Enum.Stato.Firmato;

@Service
public class FlowsFirmaMultiplaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsFirmaMultiplaService.class);

    @Inject
    private TaskService taskService;
    @Inject
    private FlowsTaskService flowsTaskService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private ApplicationContext context;
    @Inject
    private FlowsFirmaService flowsFirmaService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private SecurityService securityService;


    public ResponseEntity<Map<String, List<String>>> signMany(String username, String password, String otp, List<String> taskIds) 
            throws ArubaSignServiceException, FileFormatException {

        List<String> succesfulTasks = new ArrayList<>();
        List<String> failedTasks = new ArrayList<>();

        // la mappa LinkedHashMap preserva l'ordine di inserimento
        LinkedHashMap<Task, List<FileAllaFirma>> tasks = getTaskFilesForFirma(taskIds);

        List<byte[]> fileContents = getBytesForFiles(tasks, failedTasks);

        // se c'e' un solo tipo di file, tento di dargli un'apparence
        PdfSignApparence pdfSignApparence = getApparenceIfOneTypeOfFile(tasks);

        // firma
        List<SignReturnV2> signResponses = flowsFirmaService.firmaMultipla(username, password, otp, fileContents, pdfSignApparence);

        // processo i file firmati nell'ordine iniziale
        Map<String, List<String>> result = aggiornaAllegati(succesfulTasks, failedTasks, tasks, signResponses);

        return ResponseEntity.ok(result);

    }

    private Map<String, List<String>> aggiornaAllegati(List<String> succesfulTasks, List<String> failedTasks,
            Map<Task, List<FileAllaFirma>> tasks, List<SignReturnV2> signResponses) {
        for (Task task : tasks.keySet()) {

            String taskId = task.getId();
            List<FileAllaFirma> filesPerQuestoTask = tasks.get(task);
            List<SignReturnV2> responsesPerQuestoTask = new ArrayList<SignReturnV2>();
            for (int i = 0; i < filesPerQuestoTask.size(); i++)
                responsesPerQuestoTask.add(signResponses.remove(0));
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("sceltaUtente", "Firma Multipla");

            if (allResponsesOk(signResponses)) {

                for (FileAllaFirma file : filesPerQuestoTask) {
                    SignReturnV2 signResponse = responsesPerQuestoTask.remove(0);
                    String nomeFile = file.nome;
                    FlowsAttachment att = taskService.getVariable(taskId, file.nome, FlowsAttachment.class);

                    String key = taskService.getVariable(taskId, "key", String.class);
                    String path = att.getPath();
                    String signedFileName = FirmaDocumentoService.getSignedFilename(att.getFilename());
                    String uid = flowsAttachmentService.saveOrUpdateBytes(signResponse.getBinaryoutput(), nomeFile, signedFileName, key, path);

                    att.setUrl(uid); // qui bisogna gestire i files esterni
                    att.setFilename(signedFileName);
                    att.setAzione(Firma);
                    att.addStato(Firmato);
                    att.setUsername(securityService.getCurrentUserLogin());
                    att.setTime(new Date());
                    att.setTaskId(taskId);
                    att.setTaskName(task.getName());

                    data.put(nomeFile, att);
                }
                succesfulTasks.add(taskId);
                flowsTaskService.completeTask(taskId, data);

            } else {
                String taskError = getFirstErrorCode(signResponses);
                String key = taskService.getVariable(taskId, "key", String.class);
                failedTasks.add(taskId +":"+ key +" - "+ taskError);
            }
        }
        
        return new HashMap<String, List<String>>() {{
            put("success", succesfulTasks);
            put("failure", failedTasks);
        }};
    }

    // restituisce il primo errore incontrato
    private String getFirstErrorCode(List<SignReturnV2> signResponses) {
        for (SignReturnV2 resp : signResponses) {
            if (!resp.getStatus().equals("OK"))
                return resp.getReturnCode();
        }
        return "Errore sconosciuto";
    }

    private PdfSignApparence getApparenceIfOneTypeOfFile(Map<Task, List<FileAllaFirma>> tasks) {
        PdfSignApparence pdfSignApparence = null;
        if (tasks.values().stream().flatMap(List::stream).distinct().count() == 1) {
            final FileAllaFirma f = tasks.values().stream().flatMap(List::stream).findFirst().get();
            try {
                pdfSignApparence = context.getBean(f.nome, PdfSignApparence.class);
            } catch (BeansException _ex) {
                LOGGER.warn("Cannot find bean for pdfSignApparence {}", f.nome);
            }
        }
        return pdfSignApparence;
    }

    /**
     * Data una lista di taskIds, verra' costruita una mappa di Task con i file da firmare.
     * La mappa ha come chiave il Task e come valore una lista di FileAllaFirma
     * La mappa e' una LinkedHashMap, quindi preserva l'ordine di inserimento
     * 
     * Alcuni file sono indicati come opzionali, se mancano verranno rimossi dalla mappa
     * Se mancano file non opzionali, verra' lanciata un'eccezione dal metodo getBytesForFiles
     * 
     * @param taskIds
     * @return
     */
    private LinkedHashMap<Task, List<FileAllaFirma>> getTaskFilesForFirma(List<String> taskIds) {
        LinkedHashMap<Task, List<FileAllaFirma>> tasks = new LinkedHashMap<Task, List<FileAllaFirma>>();
        for (int i = 0; i < taskIds.size(); i++) {
            String id = taskIds.get(i);
            Task task = taskService.createTaskQuery().taskId(id).singleResult();
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String key = pi.getProcessDefinitionKey() +"#"+ task.getTaskDefinitionKey();
            List<FileAllaFirma> filesDaFirmare = new ArrayList<FlowsFirmaService.FileAllaFirma>();
            
            NOME_FILE_FIRMA.get(key).forEach(file -> {
                if (file.array == false) {
                    FlowsAttachment att = taskService.getVariable(id, file.nome, FlowsAttachment.class);
                    if (att == null) {
                        if (file.opzionale)
                            LOGGER.debug("File opzionale non presente {}, salto", file.nome);
                        else
                            throw new TaskFailedException("Attachment non opzionali mancanti: "+file.nome);
                    }
                    else 
                        filesDaFirmare.add(file);
                    
                } else {
                    List<FlowsAttachment> attachments = flowsAttachmentService.getAttachmentArray(pi.getId(), file.nome);
                    if (attachments.size() == 0 && !file.opzionale)
                        throw new TaskFailedException("Attachment non opzionali mancanti: "+file.nome);
                    
                    attachments.forEach(att -> filesDaFirmare.add(new FileAllaFirma(att.getName()))); 
                }
            });

            tasks.put(task, filesDaFirmare);
        }
        return tasks;
    }


    private List<byte[]> getBytesForFiles(Map<Task, List<FileAllaFirma>> tasks, List<String> failedTasks) throws FileFormatException {

        List<byte[]> result = new ArrayList<byte[]>();
        List<Task> tasksToRemove = new ArrayList<Task>(); // per rimuovere i task fuori dal loop in un secondo momento
        
        for (Task task : tasks.keySet()) {

            List<FileAllaFirma> files = tasks.get(task);
            for (FileAllaFirma file : files) {
                FlowsAttachment att = taskService.getVariable(task.getId(), file.nome, FlowsAttachment.class);
                if (att.getMimetype().contains("pdf")) {
                    result.add(flowsAttachmentService.getAttachmentContentBytes(att));
                } else {
                    String key = taskService.getVariable(task.getId(), "key", String.class);
                    String taskError = "Il file \""+ att.getFilename() +"\" non è del tipo pdf";
                    failedTasks.add(task.getId()+":"+ key +" - "+ taskError);
                    tasksToRemove.add(task);
                }
            }
        }
        
        for (Task task : tasksToRemove)
            tasks.remove(task);

        return result;
    }

    private boolean allResponsesOk(List<SignReturnV2> signResponses) {
        for (SignReturnV2 response : signResponses)
            if (!response.getStatus().equals("OK"))
                return false;
        return true;
    }
}
