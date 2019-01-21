package it.cnr.si.domain;

import lombok.*;
import java.io.Serializable;


@Getter
@Setter
@ToString
public class ExternalProblem implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final int PRIORITY = 2;
    private String firstName;
    private String familyName;
    private String email;
    private String login;

    // problem
	private Long idSegnalazione;
    private String titolo;
    private String descrizione;
    private Integer categoria;
    private String categoriaDescrizione;
    private boolean confirmRequested;

    // note
    private String nota;
    private Integer stato;
    
	public static final String ALLEGATO_STRING = "allegato";

    // allegato bas64
    private String allegato;
}
