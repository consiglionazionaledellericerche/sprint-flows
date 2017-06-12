package it.cnr.si.flows.ng.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowsAttachment implements Serializable {

    public enum Stato {
        Validato,
        Protocollato,
        Firmato,
        Controfirmato,
        Annullato,
        Sostituito
    }

    public enum Azione {
        Caricamento,
        Aggiornamento,
        Firma,
        Controfirma,
        Pubblicazione,
        Protocollo,
        Validazione,
        Annullo,
        Sostituzione
    }

    private static final long serialVersionUID = -1794306306586001492L;

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsAttachment.class);

	public static final String PUBBLICAZIONE_FLAG = null;


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

    @SuppressWarnings("unchecked")
    public void removeStato(Stato s) {
        if (this.getMetadati().get("stati") == null)
            this.getMetadati().put("stati", new ArrayList<Stato>());
        List<Stato> stato = (List<Stato>) this.getMetadati().get("stati");
        stato.remove(s);
    }

    public void clearStato() {
        this.getMetadati().put("stati", new ArrayList<Stato>());
    }

    public void setAzione(Azione a) {
        this.getMetadati().put("azione", a);
    }

    public void setMetadato(String nome, Object valore) {
    	getMetadati().put(nome, valore);
    }

//    public FlowsAttachment clone() {
//        FlowsAttachment clone = new FlowsAttachment();
//
//        HashMap<String, Object> metadati = (HashMap<String, Object>) getMetadati();
//
//        clone.setFilename(getFilename());
//        clone.setUsername(getUsername());
//        clone.setUrl(getUrl());
//        clone.setMimetype(getMimetype());
//        clone.setTime(getTime());
//        clone.setName(getName());
//        clone.setBytes(getBytes());
//        clone.setTaskName(getTaskName());
//        clone.setMetadati(
//                getMetadati().entrySet().stream().collect(Collectors.toMap(
//                        k -> k.clone(),
//                        v -> v.clone())
//                        )
//                );
//
//
//        return clone;
//    }

}
