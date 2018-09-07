package it.cnr.si.flows.ng.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RuoloUtenteWebDto extends WebDto {

    private Boolean admin;

    private Boolean attivo;

    private Boolean delega;

    private String delegante;

    private EntitaOrganizzativaWebDto entitaOrganizzativa;

    private String note;

    private String provvedimento;

    private RuoloWebDto ruolo;

    private UtenteWebDto utente;
}
