package it.cnr.si.flows.ng.listeners.oiv;

import java.util.Arrays;

public enum SceltaUtenteEnum {
    START("start"),
    PRENDO_IN_CARICO_LA_DOMANDA("prendo_in_carico_la_domanda"),
    ICHIESTA_SOCCORSO_ISTRUTTORIO("richiesta_soccorso_istruttorio"),
    INVIO_VALUTAZIONE("invio_valutazione"),
    RICHIESTA_SOCCORSO_ISTRUTTORIO("richiesta_soccorso_istruttorio"),
    GENERA_PDF_PREAVVISO_DI_RIGETTO("genera_PDF_preavviso_di_rigetto"),
    INVIA_PREAVVISO_DI_RIGETTO("invia_preavviso_di_rigetto"),
    APPROVA("approva"),
    INVIA_ALLA_VALUTAZIONE("invia_alla_valutazione"),
    GENERA_PDF_RIGETTO("genera_PDF_rigetto"),
    INVIA_RIGETTO_FIRMATO("invia_rigetto_firmato"),
    NOT_DEFINED("not_defined");

    private String value;

    SceltaUtenteEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SceltaUtenteEnum fromValue(String v) {
        return Arrays.asList(SceltaUtenteEnum.values()).stream()
                .filter(s -> s.getValue().equals(v))
                .findAny()
                .orElse(NOT_DEFINED);
    }

}
