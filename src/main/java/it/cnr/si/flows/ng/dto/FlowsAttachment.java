package it.cnr.si.flows.ng.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowsAttachment implements Serializable {

    public enum Stato {
        Validato,
        Protocollato,
        Firmato,
        Controfirmato
    }

    public enum Azione {
        Caricamento,
        Aggiornamento,
        Firma,
        Controfirma,
        Pubblicazione,
        Validazione
    }

    private static final long serialVersionUID = -1794306306586001492L;

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsAttachment.class);

    public static final String PUBBLICAZIONE_FLAG = "Pubblicazione";

private String name;
    private String filename;
    private Date   time;
    private String taskId;
    private String taskName;
    private String userName;
    private String mimetype;
    private String url;
    private byte[] bytes;
    private Map<String, Object> metadati;

    public FlowsAttachment() {}

    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getUsername() {
        return userName;
    }
    public void setUsername(String username) {
        this.userName = username;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getMimetype() {
        return mimetype;
    }
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Map<String, Object> getMetadati() {
        if (this.metadati == null)
                this.metadati = new HashMap<>();
        return this.metadati;
    }

    public void setMetadati(Map<String, Object> metadati) {
        this.metadati = metadati;
    }

    @SuppressWarnings("unchecked")
    public void addStato(Stato s) {
        if (this.getMetadati().get("stati") == null)
            this.getMetadati().put("stati", new ArrayList<Stato>());

        ((List<Stato>) this.getMetadati().get("stati")).add(s);
    }

    public void setAzione(Azione a) {
        this.getMetadati().put("azione", a);
    }

    public void setMetadato(String nome, Object valore) {
    	getMetadati().put(nome, valore);
    }

}
