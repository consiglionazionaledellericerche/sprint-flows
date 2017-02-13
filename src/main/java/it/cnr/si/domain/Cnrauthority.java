package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Cnrauthority.
 */
@Entity
@Table(name = "cnrauthority")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Cnrauthority implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "parentid")
    private String parentid;

    @Column(name = "display_name")
    private String display_name;

    @Column(name = "name")
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParentid() {
        return parentid;
    }

    public Cnrauthority parentid(String parentid) {
        this.parentid = parentid;
        return this;
    }

    public void setParentid(String parentid) {
        this.parentid = parentid;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public Cnrauthority display_name(String display_name) {
        this.display_name = display_name;
        return this;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getName() {
        return name;
    }

    public Cnrauthority name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Cnrauthority cnrauthority = (Cnrauthority) o;
        if(cnrauthority.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, cnrauthority.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Cnrauthority{" +
            "id=" + id +
            ", parentid='" + parentid + "'" +
            ", display_name='" + display_name + "'" +
            ", name='" + name + "'" +
            '}';
    }
}
