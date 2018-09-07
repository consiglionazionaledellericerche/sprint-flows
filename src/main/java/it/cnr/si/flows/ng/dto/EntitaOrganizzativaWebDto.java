package it.cnr.si.flows.ng.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntitaOrganizzativaWebDto extends WebDto {

  private String denominazione;

  private String denominazioneBreve;

  private String sigla;

  private String idnsip;

  private String cdsuo;

  private String carattere;

  private TipoEntitaOrganizzativaWebDto tipo;

  private Integer entitaLocale;

  private IndirizzoWebDto indirizzoPrincipale;
}
