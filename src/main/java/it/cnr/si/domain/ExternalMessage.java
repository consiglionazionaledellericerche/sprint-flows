package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

import it.cnr.si.domain.enumeration.ExternalMessageVerb;

import it.cnr.si.domain.enumeration.ExternalMessageStatus;

/**
 * A ExternalMessage.
 */
@Entity
@Table(name = "external_message")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ExternalMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "url", nullable = false)
    private String url;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verb", nullable = false)
    private ExternalMessageVerb verb;

    @NotNull
    @Column(name = "payload", nullable = false)
    private String payload;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExternalMessageStatus status;

    @Column(name = "retries")
    private Integer retries;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public ExternalMessage url(String url) {
        this.url = url;
        return this;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ExternalMessageVerb getVerb() {
        return verb;
    }

    public ExternalMessage verb(ExternalMessageVerb verb) {
        this.verb = verb;
        return this;
    }

    public void setVerb(ExternalMessageVerb verb) {
        this.verb = verb;
    }

    public String getPayload() {
        return payload;
    }

    public ExternalMessage payload(String payload) {
        this.payload = payload;
        return this;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public ExternalMessageStatus getStatus() {
        return status;
    }

    public ExternalMessage status(ExternalMessageStatus status) {
        this.status = status;
        return this;
    }

    public void setStatus(ExternalMessageStatus status) {
        this.status = status;
    }

    public Integer getRetries() {
        return retries;
    }

    public ExternalMessage retries(Integer retries) {
        this.retries = retries;
        return this;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public ExternalMessage lastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
        return this;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExternalMessage externalMessage = (ExternalMessage) o;
        if(externalMessage.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, externalMessage.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ExternalMessage{" +
            "id=" + id +
            ", url='" + url + "'" +
            ", verb='" + verb + "'" +
            ", payload='" + payload + "'" +
            ", status='" + status + "'" +
            ", retries='" + retries + "'" +
            ", lastErrorMessage='" + lastErrorMessage + "'" +
            '}';
    }
}
