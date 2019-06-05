package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Membership.
 */
@Entity
@Table(name = "membership")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Membership implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "grouprole", nullable = false)
    private String grouprole;

    @ManyToOne
    @JoinColumn(name = "cnrgroup_id")
    private Cnrgroup cnrgroup;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private FlowsUser user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGrouprole() {
        return grouprole;
    }

    public void setGrouprole(String grouprole) {
        this.grouprole = grouprole;
    }

    public Cnrgroup getCnrgroup() {
        return cnrgroup;
    }

    public void setCnrgroup(Cnrgroup cnrgroup) {
        this.cnrgroup = cnrgroup;
    }

    public FlowsUser getUser() {
        return user;
    }

    public void setUser(FlowsUser user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Membership membership = (Membership) o;
        if(membership.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, membership.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Membership{" +
                "id=" + id +
                ", grouprole='" + grouprole + "'" +
                '}';
    }
}
