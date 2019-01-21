package it.cnr.si.flows.ng.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AwesomeException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6063577040643273974L;

	private final int code;

	final static String[] text = {
			"OK",
			"ERRGEN",
			"ERRSQL",
			"DATI_NON_TROVATI",
			"SEDE_NON_TROVATA",
			"DIPENDENTE_NON_TROVATO",
			"ERRDIPNABILASS",
			"ERRASSSOVRAPP",
			"ERRASSNONCONS31IV_X",
			"ERRINASSFUORILIMASSSEDE",
			"ERRFINEASSFUORILIMASSSEDE",
			"ERRASSNONCONS31",
			"ERRASSNONCONS37I_III",
			"ERRASSNONCONS37IV_X",
			"ERRDECORRENZA",
			"ERRDECORRENZANULLA",
			"ERRSTRAORFUORILIM",
			"ERRDIPNABILCOMPET",
			"ERRSTRAORCOMPLFUORILIM",
			"ERRSEDEDISAGIATA",
			"ERRDUPLICAZIONESTRAO",
			"ERRREPERIBILITAINDIVIDUALE",
			"ERRCODNONVAL",
			"ERRSUPERATOTETTOREPERMENS",
			"ERRSUPERATOTETTOSTRAOSEDE",
			"ERRDIPENDENTINONELABORATI",
			"ATTESTATONONVALIDATO",
			"ATTESTATOGIAVALIDATO",
			"ERRPASSWORDVALIDAZIONEMANCANTE",
			"ERRPASSWORDLOGINMANCANTE",
			"ERRPASSWORDVALIDAZIONESCORRETTA",
			"ERRINSTRALCIODAHOST",
			"ERRINREGISTRASUHOST",
			"WARNINSTRALCIODAHOST",
			"WARNINREGISTRASUHOST",
			"ATTESTATO GIA VALIDATO",
			"SUCCESSO",
			"DATI_SQL_INESISTENTI",
			"SEDE_SQL_INESISTENTE",
			"ORARIO_SEDE_INESISTENTE",
			"ERRREGISTRAZIONEDUPLICATA",
			"NONEXISTENTERRCODE",
			"ERROVERFLOWTABELLA",
			"ERRGENERICOSQL",
			"ERRSQLNONDISPONIBILE",
			"ERRTERMINAZANOMALA",
			"WARNING-TIMEOUT",
			"ERRASSNONCONS31CL9801",
			"ERRASSNONCONS37CL9801",
			"ERRASSCONSSOLOLIVIAIIICL9801",
			"ERRASSCONSSOLOLIVIVAIXCL9801",
			"ERRFAREPRIMALOSTRALCIO",
			"ATTENZIONESTRALCIOGIAESEGUITO",
			"ATTENZIONE_REGISTRAZIONE_GIA_ESEGUITA_IN_PRECEDENZA",
			"ERRORE_COMPETENZA_NULLA",
			"ATTENZIONE: GIORNI COMPETENZE SUPERIORI A GIORNI PRESENZE <BR> (assegnati giorni presenza effettivi)",
			"ATTENZIONE: ESEGUIRE CONTROLLO TETTO", "ERRSTRALCIOINCORSO",
			"ERRREGISTRAZIONEINCORSO",
			"ERRORESISTEMASOVRACCARICO_RIPROVARE_PIU_TARDI", "ERRORE_SQL_TCAS",
			"ERRORE_SQL_ORACLE", "DB_SOVRACCARICO",
			"ERR_SQLDS_CANNOT_ROLLBACK", "OPERAZIONE_DISABILITATA",
			"REGISTRAZIONE_INCOMPLETA", "LDAP_NON_DISPONIBILE",
			"ERRORE NOME UTENTE O PASSWORD ERRATI",
			"ERRORE UTENTE NON ABILITATO", "ERRASSNONCONS203",
			"ERRORE NEL REPORT", "ERRORE PASSWORD SCADUTA",
			"ERRORE QUANTITA' BUONI PASTO",
			"ATTESTATO BUONI PASTO NON MODIFICABILE" };

	public AwesomeException(int code) {
		this.code = code;
	}

	public AwesomeException(String message) {
		super(message);
		this.code = 1;
	}

	public AwesomeException(int code, String message) {
		super(message);
		this.code = code;
	}

	public AwesomeException(int code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public String  getMessage() {
		return super.getMessage()!=null?super.getMessage():(text[code] + " [" + code + "]");
	}

	public ResponseEntity<?> getResponse() {
		
		ResponseEntity<?> response = null;		
		if (this.getCode()==0)
			response = new ResponseEntity<>(HttpStatus.CREATED);
		else
			response = new ResponseEntity<String>(this.getMessage(), HttpStatus.BAD_REQUEST);
		return response;
	}
}
