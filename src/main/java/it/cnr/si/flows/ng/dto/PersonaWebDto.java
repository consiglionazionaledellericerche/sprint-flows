package it.cnr.si.flows.ng.dto;

import it.cnr.si.flows.ng.utils.TipoContratto;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PersonaWebDto extends WebDto {

    private String nome;

    private String cognome;

    private Integer matricola;

    private String sesso;

    private LocalDate dataNascita;

    private String codiceFiscale;

    private String username;

    private LocalDate dataCessazione;

    private LocalDate dataPrevistaCessazione;

    private TipoContratto tipoContratto;

    private EntitaOrganizzativaWebDto sede;
}
