package it.cnr.si.flows.ng.listeners.oiv;

import java.util.Arrays;

public enum FaseEsecuzioneEnum {
    PROCESS_START("process-start"),
    SMISTAMENTO_START("smistamento-start"),
    SMISTAMENTO_END("smistamento-end"),
    ISTRUTTORIA_START("istruttoria-start"),
    ISTRUTTORIA_END("istruttoria-end"),
    SOCCORSO_ISTRUTTORIO_START("soccorso-istruttorio-start"),
    SOCCORSO_ISTRUTTORIO_END("soccorso-istruttorio-end"),
    CAMBIO_ISTRUTTORE_START("cambio-istruttore-start"),
    CAMBIO_ISTRUTTORE_END("cambio-istruttore-end"),
    VALUTAZIONE_START("valutazione-start"),
    VALUTAZIONE_END("valutazione-end"),
    PREAVVISO_RIGETTO_START("preavviso-rigetto-start"),
    PREAVVISO_RIGETTO_END("preavviso-rigetto-end"),
    ISTRUTTORIA_SU_PREAVVISO_START("istruttoria-su-preavviso-start"),
    ISTRUTTORIA_SU_PREAVVISO_END("istruttoria-su-preavviso-end"),
    VALUTAZIONE_PREAVVISO_START("valutazione-preavviso-start"),
    VALUTAZIONE_PREAVVISO_END("valutazione-preavviso-end"),
    FIRMA_DG_RIGETTO_START("firma-dg-rigetto-start"),
    FIRMA_DG_RIGETTO_END("firma-dg-rigetto-end"),
    END_IMPROCEDIBILE("end-improcedibile"),
    END_APPROVATA("end-approvata"),
    END_RESPINTA("end-respinta"),
    AVVISO_SCADENZA_TEMPI_PROCEDURALI_START("avviso-scadenza-tempi-procedurali-start"),
    SCADENZA_TEMPI_PROCEDURALI_START("scadenza-tempi-procedurali-start"),
    SCADENZA_TEMPI_SOCCORSO_ISTRUTTORIO("scadenza-tempi-soccorso-istruttorio"),
    SCADENZA_TEMPI_PREAVVISO_RIGETTO("scadenza-tempi-preavviso-rigetto"),
    PROCESS_END("process-end");

    private String value;

    FaseEsecuzioneEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FaseEsecuzioneEnum fromValue(String v) {
        return Arrays.asList(FaseEsecuzioneEnum.values()).stream()
                .filter(s -> s.getValue().equals(v))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(v));
    }

}
