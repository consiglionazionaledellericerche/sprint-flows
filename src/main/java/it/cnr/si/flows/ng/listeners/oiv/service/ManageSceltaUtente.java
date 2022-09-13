package it.cnr.si.flows.ng.listeners.oiv.service;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.listeners.oiv.FaseEsecuzioneEnum;
import it.cnr.si.flows.ng.listeners.oiv.ManageProcessIscrizioneElencoOiv;
import it.cnr.si.flows.ng.listeners.oiv.SceltaUtenteEnum;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;
import it.cnr.si.flows.ng.service.FlowsControlService;
import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;

import static it.cnr.si.flows.ng.utils.Enum.PdfType.*;

@Service
public class ManageSceltaUtente {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageSceltaUtente.class);
    public static final String VALUTAZIONE_ISTRUTTORE = "valutazioneIstruttore";
    public static final String DOMANDA_DA_APPROVARE = "domanda_da_approvare";
    public static final String POSITIVA = "positiva";
    public static final String NEGATIVA = "negativa";
    public static final String PDF_PREAVVISO_RIGETTO_FLAG = "pdfPreavvisoRigettoFlag";
    public static final String TIPOLOGIA_RICHIESTA = "tipologiaRichiesta";
    public static final String PREAVVISO_RIGETTO = "preavvisoRigetto";
    public static final String PREAVVISO_RIGETTO_CAMBIO_FASCIA = "preavvisoRigettoCambioFascia";
    public static final String SOCCORSO_ISTRUTTORIA_FLAG = "soccorsoIstruttoriaFlag";
    public static final String PDF_RIGETTO_FLAG = "pdfRigettoFlag";
    public static final String TEMPI_PREAVVISO_RIGETTO = "tempiPreavvisoRigetto";
    public static final String SCADUTI = "SCADUTI";
    public static final String RIGETTO_MOTIVATO = "rigettoMotivato";
    public static final String RIGETTO_DEF_10_GIORNI = "RigettoDef10Giorni";

    @Autowired
    private FlowsAttachmentService attachmentService;
    @Inject
    private CreateOivPdf createOivPdf;
    @Inject
    private ManageControlli manageControlli;
    @Inject
    private DeterminaAttore determinaAttore;
    @Inject
    private FlowsControlService flowsControlService;

    public void azioneScelta(DelegateExecution execution, String faseEsecuzioneValue, SceltaUtenteEnum sceltaUtente) throws IOException, ParseException {
        String processInstanceId = execution.getProcessInstanceId();
        LOGGER.info("-- azioneScelta: {} con sceltaUtente: {}", faseEsecuzioneValue, sceltaUtente);
        FaseEsecuzioneEnum faseEsecuzione = FaseEsecuzioneEnum.fromValue(faseEsecuzioneValue);
        if (sceltaUtente != null) {
            switch (faseEsecuzione) {
                case SMISTAMENTO_END: {
                    if (sceltaUtente.equals(SceltaUtenteEnum.PRENDO_IN_CARICO_LA_DOMANDA))
                        determinaAttore.determinaIstruttore(execution);
                    if (sceltaUtente.equals(SceltaUtenteEnum.RICHIESTA_SOCCORSO_ISTRUTTORIO))
                        manageControlli.verificaPuntiSoccorso(execution);
                }
                ;
                break;
                case ISTRUTTORIA_END: {
                    if (sceltaUtente.equals(SceltaUtenteEnum.INVIO_VALUTAZIONE)) {
                        manageControlli.valutazioneEsperienze(execution,
                                Optional.ofNullable(execution.getVariable(VALUTAZIONE_ISTRUTTORE))
                                    .filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .filter(s -> s.equals(DOMANDA_DA_APPROVARE))
                                    .map(s -> POSITIVA)
                                    .orElse(NEGATIVA)
                        );
                    }
                    if (sceltaUtente.equals(SceltaUtenteEnum.RICHIESTA_SOCCORSO_ISTRUTTORIO))
                        manageControlli.verificaPuntiSoccorso(execution);
                }
                ;
                break;
                case VALUTAZIONE_END: {
                    if (sceltaUtente.equals(SceltaUtenteEnum.GENERA_PDF_PREAVVISO_DI_RIGETTO)) {
                        manageControlli.valutazioneEsperienzeGenerazionePdf(execution);
                        execution.setVariable(PDF_PREAVVISO_RIGETTO_FLAG, "1");
                        switch (execution.getVariable(TIPOLOGIA_RICHIESTA).toString()) {
                            case "Iscrizione":
                                createOivPdf.creaPdfOiv(execution, preavvisoRigetto.name());
                                break;
                            case "rinnovo":
                                createOivPdf.creaPdfOiv(execution, preavvisoRigetto.name());
                                break;
                            case "modifica_fascia":
                                createOivPdf.creaPdfOiv(execution, preavvisoRigetto.name());
                                break;
                        }
                    }
                    if (sceltaUtente.equals(SceltaUtenteEnum.RICHIESTA_SOCCORSO_ISTRUTTORIO)) {
                        manageControlli.verificaPuntiSoccorso(execution);
                    }
                    if (sceltaUtente.equals(SceltaUtenteEnum.INVIA_PREAVVISO_DI_RIGETTO)) {
                        String nomeFilePreavviso = PREAVVISO_RIGETTO;
                        FlowsAttachment fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get(PREAVVISO_RIGETTO);
                        if (fileRecuperato != null) {
                            nomeFilePreavviso = fileRecuperato.getName();
                        } else {
                            fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get(PREAVVISO_RIGETTO_CAMBIO_FASCIA);
                            if (fileRecuperato != null) {
                                nomeFilePreavviso = fileRecuperato.getName();
                            }
                        }
                        LOGGER.info("-- verificaFileFirmatoP7m: nomeFilePreavviso: {}", nomeFilePreavviso);
                        flowsControlService.verificaFileFirmato_Cades_Pades(execution, nomeFilePreavviso);
                    }
                    if (sceltaUtente.equals(SceltaUtenteEnum.APPROVA)) {
                        manageControlli.valutazioneEsperienze(execution, POSITIVA);
                    }
                }
                ;
                break;
                case SOCCORSO_ISTRUTTORIO_START: {
                    execution.setVariable(SOCCORSO_ISTRUTTORIA_FLAG, "1");
                }
                ;
                break;
                case ISTRUTTORIA_SU_PREAVVISO_END: {
                    if (sceltaUtente.equals(SceltaUtenteEnum.INVIA_ALLA_VALUTAZIONE)) {
                        manageControlli.valutazioneEsperienze(execution, Optional.ofNullable(execution.getVariable(VALUTAZIONE_ISTRUTTORE))
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .filter(s -> s.equalsIgnoreCase(DOMANDA_DA_APPROVARE))
                                .map(s -> POSITIVA)
                                .orElse(NEGATIVA));
                    }
                }
                ;
                break;
                case VALUTAZIONE_PREAVVISO_END: {
                    if (sceltaUtente.equals(SceltaUtenteEnum.GENERA_PDF_RIGETTO)) {
                        manageControlli.valutazioneEsperienzeGenerazionePdf(execution);
                        execution.setVariable(PDF_RIGETTO_FLAG, "1");
                        if (((execution.getVariable(TEMPI_PREAVVISO_RIGETTO) != null) && (execution.getVariable(TEMPI_PREAVVISO_RIGETTO).toString().equals(SCADUTI)))) {
                            createOivPdf.creaPdfOiv(execution, RigettoDef10Giorni.name());
                        } else {
                            createOivPdf.creaPdfOiv(execution, rigettoMotivato.name());
                        }
                    }
                    if (sceltaUtente.equals(SceltaUtenteEnum.APPROVA)) {
                        manageControlli.valutazioneEsperienze(execution, POSITIVA);
                    }
                }
                ;
                break;
                case FIRMA_DG_RIGETTO_END: {
                    if (sceltaUtente.equals(SceltaUtenteEnum.INVIA_RIGETTO_FIRMATO)) {
                        String nomeFileRigetto = RIGETTO_MOTIVATO;
                        FlowsAttachment fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get(RIGETTO_MOTIVATO);
                        if (fileRecuperato != null) {
                            nomeFileRigetto = fileRecuperato.getName();
                        } else {
                            fileRecuperato = attachmentService.getAttachementsForProcessInstance(processInstanceId).get(RIGETTO_DEF_10_GIORNI);
                            if (fileRecuperato != null) {
                                nomeFileRigetto = fileRecuperato.getName();
                            }
                        }
                        LOGGER.info("-- verificaFileFirmatoP7m: nomeFileRigetto: {}", nomeFileRigetto);
                        flowsControlService.verificaFileFirmato_Cades_Pades(execution, nomeFileRigetto);
                    }
                }
                ;
                break;
                default: {
                    LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                }
                ;
                break;
            }
        }
    }

}
