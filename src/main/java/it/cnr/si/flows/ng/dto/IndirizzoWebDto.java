package it.cnr.si.flows.ng.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndirizzoWebDto extends WebDto {

  private String via;

  private String civico;

  private String localita;

  private String comune;

  private String provincia;

  private String siglaProvincia;

  private String nazione;

  private String cap;

  private Integer geoId;

}
