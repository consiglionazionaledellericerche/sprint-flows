package it.cnr.si.flows.ng.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RuoloWebDto extends WebDto {

    private String contesti;

    private String descr;

    private String figli;

    private String padre;

    private String sigla;

    private String tipiEntitaOrganizzativa;
}
