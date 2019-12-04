package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Faq.
 */
@Entity
@Table(name = "draft")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Draft implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @NotNull
    @Column(name = "json", nullable = false)
    private String json;

    @Column(name = "username")
    private String username;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Draft draft = (Draft) o;
        return Objects.equals(id, draft.id) &&
                Objects.equals(taskId, draft.taskId) &&
                Objects.equals(json, draft.json) &&
                Objects.equals(username, draft.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, taskId, json);
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getJson() {
        return json;
    }

    public String getUsername() {
        return username;
    }

    public Draft taskId(Long taskId) {
        this.taskId = taskId;
        return this;
    }

    public Draft json(String json) {
        this.json = json;
        return this;
    }


    @Override
    public String toString() {
        return "Draft{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", json='" + json + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
