package it.cnr.si.flows.ng.utils;


public class Enum {

    public enum Actions {

        revoca("Revoca"),
        revocaSemplice("RevocaSemplice"),
        RevocaConProvvedimento("RevocaConProvvedimento");

        private String value;

        Actions(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }


    public enum Role {
        responsabile("responsabile"),
        responsabileStruttura("responsabile-struttura"),
        supervisore("supervisore"),
        supervisoreStruttura("supervisore-struttura");


        private String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public enum SiglaList {
        TIPOLOGIA_ACQUISIZIONE;
    }


    public enum Stato {
        Validato,
        Protocollato,
        Firmato,
        Controfirmato,
        Annullato,
        Pubblicato,
        Sostituito
    }


    public enum Azione {
        Caricamento,
        Aggiornamento,
        Firma,
        Controfirma,
        Pubblicazione,
        Protocollo,
        Validazione,
        Annullo,
        Sostituzione,
        RimozioneDaPubblicazione
    }


    public enum ProcessDefinitionEnum {
        acquisti("acquisti"),
        permessiFerie("permessi-ferie"),
        iscrizioneElencoOiv("iscrizione-elenco-oiv");

        private String value;

        ProcessDefinitionEnum(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    public enum RoleOiv {
        coordinator,
        member;
    }
    public enum VariableEnum {
        initiator,
        titolo,
        stato,
        descrizione,
        idStruttura,
        startDate,
        endDate,
        gruppoRA;
    }


    public enum PdfType {
        rigetto,
        rigettoMotivato,
        rigettoDopoPreavviso,
        rigettoDopo10Giorni,
        RigettoDef10Giorni,
        improcedibile,
        preavvisoRigetto,
        soccorsoIstruttorio,
        preavvisoRigettoDef10Giorni,
        preavvisoRigettoCambioFascia;

        PdfType() {
        }
    }

    public enum TipiEOPerAutocomplete {
        Istituto(1),
        Gruppi(7),
        Dipartimento(21),
        Ufficio(41),
        Ufficionondirigenziale(42),
        Strutturadiparticolarerilievo(43);

        private int value;

        TipiEOPerAutocomplete(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static boolean contains(int id) {
            for (TipiEOPerAutocomplete t : TipiEOPerAutocomplete.values()) {
                if (t.value == id)
                    return true;
            }
            return false;
        }
    }

}