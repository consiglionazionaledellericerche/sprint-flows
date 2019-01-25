package it.cnr.si.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Avviso.
 */
@Entity
@Table(name = "avviso")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Avviso implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "contenuto", nullable = false)
    private String contenuto;

    @Column(name = "attivo")
    private Boolean attivo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContenuto() {
        return contenuto;
    }

    public Avviso contenuto(String contenuto) {
        this.contenuto = contenuto;
        return this;
    }

    public void setContenuto(String contenuto) {
        this.contenuto = contenuto;
    }

    public Boolean isAttivo() {
        return attivo;
    }

    public Avviso attivo(Boolean attivo) {
        this.attivo = attivo;
        return this;
    }

    public void setAttivo(Boolean attivo) {
        this.attivo = attivo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Avviso avviso = (Avviso) o;
        if(avviso.id == null || id == null) {
            return false;
        }
        return Objects.equals(id, avviso.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Avviso{" +
            "id=" + id +
            ", contenuto='" + contenuto + "'" +
            ", attivo='" + attivo + "'" +
            '}';
    }
}
