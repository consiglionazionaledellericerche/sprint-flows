package it.cnr.si.flows.ng.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class WebPeriodDto extends WebDto {

  private LocalDate inizioValidita;

  private LocalDate fineValidita;

}
