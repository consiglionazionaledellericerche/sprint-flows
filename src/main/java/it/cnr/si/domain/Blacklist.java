package it.cnr.si.domain;


import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Blacklist.
 */
@Entity
@Table(name = "blacklist")
public class Blacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "email", nullable = false)
    private String email;

    @NotNull
    @Column(name = "process_definition_key", nullable = false)
    private String processDefinitionKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public Blacklist email(String email) {
        this.email = email;
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public Blacklist processDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Blacklist blacklist = (Blacklist) o;
        if(blacklist.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, blacklist.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Blacklist{" +
            "id=" + id +
            ", email='" + email + "'" +
            ", processDefinitionKey='" + processDefinitionKey + "'" +
            '}';
    }
}
