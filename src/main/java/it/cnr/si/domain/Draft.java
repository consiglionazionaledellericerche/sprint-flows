package it.cnr.si.domain;


import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Draft.
 */
@Entity
@Table(name = "draft")
public class Draft implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "username")
    private String username;

    @NotNull
    @Column(name = "json", nullable = false)
    private String json;

    @Column(name = "process_definition_id")
    private String processDefinitionId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Draft taskId(Long taskId) {
        this.taskId = taskId;
        return this;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getUsername() {
        return username;
    }

    public Draft username(String username) {
        this.username = username;
        return this;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJson() {
        return json;
    }

    public Draft json(String json) {
        this.json = json;
        return this;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public Draft processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Draft draft = (Draft) o;
        if(draft.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, draft.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Draft{" +
            "id=" + id +
            ", taskId='" + taskId + "'" +
            ", username='" + username + "'" +
            ", json='" + json + "'" +
            ", processDefinitionId='" + processDefinitionId + "'" +
            '}';
    }
}
