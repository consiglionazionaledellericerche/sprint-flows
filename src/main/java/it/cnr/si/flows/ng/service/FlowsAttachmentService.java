package it.cnr.si.flows.ng.service;

import static it.cnr.si.flows.ng.utils.MimetypeUtils.getMimetype;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.activiti.engine.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.dto.FlowsAttachment.Azione;
import it.cnr.si.flows.ng.dto.FlowsAttachment.Stato;
import it.cnr.si.security.SecurityUtils;

@Service
public class FlowsAttachmentService {

    public static final String USER_SUFFIX = "_username";
    public static final String STATO_SUFFIX = "_stato";
    public static final String FILENAME_SUFFIX = "_filename";
    public static final String MIMETYPE_SUFFIX = "_mimetype";

    public static final String[] SUFFIXES = new String[] {USER_SUFFIX, STATO_SUFFIX, FILENAME_SUFFIX, MIMETYPE_SUFFIX};

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsAttachmentService.class);

    @Autowired
    private TaskService taskService;

    public Map<String, Object> extractAttachmentsVariables(MultipartHttpServletRequest req) throws IOException {
        Map<String, Object> attachments = new HashMap<>();
        String taskId, taskName;

        String username = SecurityUtils.getCurrentUserLogin();
        if (req.getParameter("taskId") != null) {
            taskId = (String) req.getParameter("taskId");
            taskName = taskService.createTaskQuery().taskId(taskId).singleResult().getName();
        } else {
            taskId = "start";
            taskName = "Avvio del flusso";
        }

        Iterator<String> i = req.getFileNames();
        while (i.hasNext()) {
            String fileName = i.next();

            LOGGER.trace("inserisco come variabile il file "+ fileName);
            FlowsAttachment att = new FlowsAttachment();

            MultipartFile file = req.getFile(fileName);
            att.setName(fileName);
            att.setFilename(file.getOriginalFilename());
            att.setTime(new Date());
            att.setTaskId(taskId);
            att.setTaskName(taskName);
            att.setUsername(username);
            att.setMimetype(getMimetype(file));
            att.setBytes(file.getBytes());

            if (taskId.equals("start"))
                att.setAzione(Azione.Caricamento);
            else {
                att.setAzione(Azione.Aggiornamento);
                att.addStato(Stato.Protocollato);
            }

            attachments.put(fileName, att);
        }

        return attachments;
    }
}
