package it.cnr.si.flows.ng.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RisultatoRicercaWebDto<C extends WebDto> {

    private int count;
    private List<C> items;

}
