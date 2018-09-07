package it.cnr.si.flows.ng.dto;

import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class UtenteWebDto extends WebDto {

    private String email;

    private Boolean inattivo;

    private PersonaWebDto persona;

    private String username;
}
