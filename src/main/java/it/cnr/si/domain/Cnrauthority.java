package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
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

    @Column(name = "display_name")
    private String display_name;

    @Column(name = "name")
    private String name;

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "cnrauthority_cnrauthorityparent",
               joinColumns = @JoinColumn(name="cnrauthorities_id", referencedColumnName="ID"),
               inverseJoinColumns = @JoinColumn(name="cnrauthorityparents_id", referencedColumnName="ID"))
    private Set<Cnrauthority> cnrauthorityparents = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<Cnrauthority> getCnrauthorityparents() {
        return cnrauthorityparents;
    }

    public Cnrauthority cnrauthorityparents(Set<Cnrauthority> cnrauthorities) {
        this.cnrauthorityparents = cnrauthorities;
        return this;
    }

    public Cnrauthority addCnrauthority(Cnrauthority cnrauthority) {
        cnrauthorityparents.add(cnrauthority);
        cnrauthority.getCnrauthorityparents().add(this);
        return this;
    }

    public Cnrauthority removeCnrauthority(Cnrauthority cnrauthority) {
        cnrauthorityparents.remove(cnrauthority);
        cnrauthority.getCnrauthorityparents().remove(this);
        return this;
    }

    public void setCnrauthorityparents(Set<Cnrauthority> cnrauthorities) {
        this.cnrauthorityparents = cnrauthorities;
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
            ", display_name='" + display_name + "'" +
            ", name='" + name + "'" +
            '}';
    }
}
