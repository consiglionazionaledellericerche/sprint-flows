package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

/**
 * A Cnrgroup.
 */
@Entity
@Table(name = "cnrgroup")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Cnrgroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Pattern(regexp = "^[a-zA-Z0-9_]*$")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @ManyToMany
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JoinTable(name = "cnrgroup_parents",
               joinColumns = @JoinColumn(name="cnrgroups_id", referencedColumnName="ID"),
               inverseJoinColumns = @JoinColumn(name="parents_id", referencedColumnName="ID"))
    private Set<Cnrgroup> parents = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Cnrgroup name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Cnrgroup displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<Cnrgroup> getParents() {
        return parents;
    }

    public Cnrgroup parents(Set<Cnrgroup> cnrgroups) {
        this.parents = cnrgroups;
        return this;
    }

//    public Cnrgroup addCnrgroup(Cnrgroup cnrgroup) {
//        parents.add(cnrgroup);
//        cnrgroup.getMemberGroups().add(this);
//        return this;
//    }
//
//    public Cnrgroup removeCnrgroup(Cnrgroup cnrgroup) {
//        parents.remove(cnrgroup);
//        cnrgroup.getMemberGroups().remove(this);
//        return this;
//    }

    public void setParents(Set<Cnrgroup> cnrgroups) {
        this.parents = cnrgroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Cnrgroup cnrgroup = (Cnrgroup) o;
        if(cnrgroup.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, cnrgroup.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Cnrgroup{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", displayName='" + displayName + "'" +
            '}';
    }
}
