package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Dynamiclist.
 */
@Entity
@Table(name = "dynamiclist")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Dynamiclist implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "listjson", nullable = false)
    private String listjson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Dynamiclist name(String name) {
        this.name = name;
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getListjson() {
        return listjson;
    }

    public Dynamiclist listjson(String listjson) {
        this.listjson = listjson;
        return this;
    }

    public void setListjson(String listjson) {
        this.listjson = listjson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dynamiclist dynamiclist = (Dynamiclist) o;
        if(dynamiclist.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, dynamiclist.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Dynamiclist{" +
            "id=" + id +
            ", name='" + name + "'" +
            ", listjson='" + listjson + "'" +
            '}';
    }
}
