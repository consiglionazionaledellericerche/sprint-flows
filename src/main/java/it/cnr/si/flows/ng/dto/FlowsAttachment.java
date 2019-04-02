package it.cnr.si.flows.ng.dto;

import it.cnr.si.flows.ng.utils.Enum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class FlowsAttachment implements Serializable {
    public static final String PUBBLICAZIONE_FLAG = null;

    private static final long serialVersionUID = -1794306306586001492L;

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsAttachment.class);
    private String name;

    public Map<String, Object> getMetadati() {
        if (this.metadati == null)
            this.metadati = new HashMap<>();
        return this.metadati;
    }

    private String label;
    private String filename;
    private Date   time;
    private String taskId;
    private String taskName;
    private String userName;
    private String mimetype;
    private String path;
    private String url;
    private byte[] bytes;
    private Map<String, Object> metadati;
    private boolean pubblicazioneUrp;
    private boolean pubblicazioneTrasparenza;
    private boolean protocollo;
    private String numeroProtocollo;
    private String dataProtocollo;

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


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setMetadato(String nome, Object valore) {
        getMetadati().put(nome, valore);
    }

    public void setMetadati(Map<String, Object> metadati) {
        this.metadati = metadati;
    }

    @SuppressWarnings("unchecked")
    public void addStato(Enum.Stato s) {
        getStati().add(s);
    }

    @SuppressWarnings("unchecked")
    public Set<Enum.Stato> getStati() {
        getMetadati().putIfAbsent("stati",new HashSet<Enum.Stato>());
        return (Set<Enum.Stato>) this.getMetadati().get("stati");
    }

    public void setStati(Set<Enum.Stato> stati) {
        getMetadati().put("stati", stati);
    }

    @SuppressWarnings("unchecked")
    public void removeStato(Enum.Stato s) {
        Set<Enum.Stato> stati = getStati();
        stati.remove(s);
    }
    @SuppressWarnings("unchecked")
    public void removeAzione(Enum.Azione a) {
        if (this.getMetadati().get("azioni") == null)
            this.getMetadati().put("azioni", new ArrayList<Enum.Stato>());
        List<Enum.Stato> azione = (List<Enum.Stato>) this.getMetadati().get("azioni");
        azione.remove(a);
    }

    public void clearStato() {
        this.getMetadati().put("stati", new HashSet<Enum.Stato>());
    }

    public void setAzione(Enum.Azione a) {
        this.getMetadati().put("azione", a);
    }

    public boolean isPubblicazioneUrp() {
        return pubblicazioneUrp;
    }

    public void setPubblicazioneUrp(boolean pubblicazioneUrp) {
        this.pubblicazioneUrp = pubblicazioneUrp;
    }

    public boolean isPubblicazioneTrasparenza() {
        return pubblicazioneTrasparenza;
    }

    public void setPubblicazioneTrasparenza(boolean pubblicazioneTrasparenza) {
        this.pubblicazioneTrasparenza = pubblicazioneTrasparenza;
    }

    public boolean isProtocollo() {
        return protocollo;
    }

    public void setProtocollo(boolean protocollo) {
        this.protocollo = protocollo;
    }

    public String getNumeroProtocollo() {
        return numeroProtocollo;
    }

    public void setNumeroProtocollo(String numeroProtocollo) {
        this.numeroProtocollo = numeroProtocollo;
    }

    public String getDataProtocollo() {
        return dataProtocollo;
    }

    public void setDataProtocollo(String dataProtocollo) {
        this.dataProtocollo = dataProtocollo;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
