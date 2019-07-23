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
@Table(name = "faq")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Faq implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "testo", nullable = false)
    private String testo;

    @NotNull
    @Column(name = "is_readable", nullable = false)
    private Boolean isReadable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTesto() {
        return testo;
    }

    public Faq testo(String testo) {
        this.testo = testo;
        return this;
    }

    public void setTesto(String testo) {
        this.testo = testo;
    }

    public Boolean isIsReadable() {
        return isReadable;
    }

    public Faq isReadable(Boolean isReadable) {
        this.isReadable = isReadable;
        return this;
    }

    public void setIsReadable(Boolean isReadable) {
        this.isReadable = isReadable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Faq faq = (Faq) o;
        if(faq.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, faq.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Faq{" +
            "id=" + id +
            ", testo='" + testo + "'" +
            ", isReadable='" + isReadable + "'" +
            '}';
    }
}
