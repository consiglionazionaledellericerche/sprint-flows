package it.cnr.si.flows.ng.utils;


public class Enum {

    public enum Actions {

        revoca("Revoca"),
        annulla("Annulla"),
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
        PubblicatoUrp,
        PubblicatoTrasparenza,
        Sostituito,
        Revocato
    }


    public enum Azione {
        Caricamento,
        Aggiornamento,
        Firma,
        Controfirma,
        PubblicazioneUrp,
        PubblicazioneTrasparenza,
        Protocollo,
        Validazione,
        Annullo,
        Sostituzione,
        RimozioneDaPubblicazioneTrasparenza,
        RimozioneDaPubblicazioneUrp,
        SostituzioneProtocollo,
        RettificaProtocollo,
        linkDaAltraApplicazione,
        GenerazioneDaSistema
    }

    public enum StatoDomandeMissioniEnum {
        RESPINTO_UO,
        RESPINTO_UO_SPESA,
        FIRMATO_UO,
        FIRMATO,
        ANNULLATO;
    }
    
    public enum TipologieeMissioniEnum {
    	ordine,
    	rimborso,
    	revoca;
    }
    
    

    public enum StatoDomandeAccordiInternazionaliEnum {
        APERTA,
        CHIUSA,
        INVIATA,
        RESPINTA,
        VALUTATA_SCIENTIFICAMENTE,
        ACCETATA,
        CANCELLATA;
    }

    public enum StatoFirmaDocumentiEnum {
        FIRMATO,
        ANNULLATO,
        PREDISPOSTO;
    }

    public enum StatoDomandeSTMEnum {
        APERTA,
        CHIUSA,
        INVIATA,
        VALIDATA,
        NON_VALIDATA,
        IN_MODIFICA,
        RESPINTA,
        VALUTATA_SCIENTIFICAMENTE,
        AUTORIZZATA,
        ANNULLATA,
        ACCETTATA,
        ACCETTAZIONE,
        RESPINTO_UO,
        RESPINTO_UO_SPESA,
        CANCELLATA;
    }

    public enum ProcessDefinitionEnum {
        acquisti("acquisti","acquisti"),
        permessiFerie("permessi-ferie", "permessi-ferie"),
        accordiInternazionaliBandi("accordi-internazionali-bandi", "accordi-internazionali-bandi"),
        accordiInternazionaliDomande("accordi-internazionali-domande", "accordi-internazionali-domande"),
        iscrizioneElencoOiv("iscrizione-elenco-oiv", "iscrizione-elenco-oiv"),
        testAcquistiAvvisi("testAcquistiAvvisi", "acquisti");


        private String value;

        private String processDefinition;

        public String getProcessDefinition() { return processDefinition;  }

        ProcessDefinitionEnum(String value, String processDefinition) {
            this.processDefinition = processDefinition;
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
        gruppoRA,
        gruppoStaffAmministrativo,
        dataScadenzaAvvisoPreDetermina,
        dataScadenzaBando,
        flagIsTrasparenza,
        statoFinaleDomanda;
    }


    public enum PdfType {
        avvisoPostInformazione,
        rigetto,
        rigettoMotivato,
        rigettoDopoPreavviso,
        rigettoDopo10Giorni,
        RigettoDef10Giorni,
        improcedibile,
        preavvisoRigetto,
        soccorsoIstruttorio,
        preavvisoRigettoDef10Giorni,
        valutazioneProgettoAccordiBilaterali,
        domandaAccordiBilaterali,
        valutazioneShortTermMobility,
        preavvisoRigettoCambioFascia,
        monitoraggioAttivitaCovid19;

        PdfType() {
        }
    }

    public enum TipiEOPerAutocomplete {
        Istituto(1),
        Area(3),
        Gruppi(7),
        Dipartimento(21),
        Sedesecondariaistituto(24),
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

        public static TipiEOPerAutocomplete byId(int id){
            for (TipiEOPerAutocomplete t : TipiEOPerAutocomplete.values()) {
                if (t.value == id)
                    return t;
            }
            return null;
        }
    }

}