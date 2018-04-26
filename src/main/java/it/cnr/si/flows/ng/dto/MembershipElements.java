package it.cnr.si.flows.ng.dto;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A timer.
 */
@Entity
//@Table(name = "timer")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class MembershipElements implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @NotNull
    @Column(name = "userName", nullable = false)
    private String userName;

    @NotNull
    @Column(name = "groupName", nullable = false)
    private String groupName;

    @NotNull
    @Column(name = "groupRole", nullable = false)
    private String groupRole;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getuserName() {
        return userName;
    }

    public MembershipElements userName(String userName) {
        this.userName = userName;
        return this;
    }

    public void setuserName(String userName) {
        this.userName = userName;
    }

    public String getgroupName() {
        return groupName;
    }

    public MembershipElements groupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public void setgroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getgroupRole() {
        return groupRole;
    }

    public MembershipElements groupRole(String groupRole) {
        this.groupRole = groupRole;
        return this;
    }

    public void setgroupRole(String groupRole) {
        this.groupRole = groupRole;
    }
   

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    @Override
    public String toString() {
        return "timer{" +
            ", userName='" + userName + "'" +
            ", groupRole='" + groupRole + "'" +
            ", groupName='" + groupName + "'" +
            '}';
    }
}
