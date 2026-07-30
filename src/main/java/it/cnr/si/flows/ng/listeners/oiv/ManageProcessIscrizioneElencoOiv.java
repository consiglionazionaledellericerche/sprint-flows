package it.cnr.si.flows.ng.listeners.oiv;

import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.listeners.oiv.service.*;
import it.cnr.si.flows.ng.service.FlowsAttachmentService;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.util.Pair;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static it.cnr.si.flows.ng.utils.Enum.PdfType.*;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.statoFinaleDomanda;

@Component
@Profile("oiv")
public class ManageProcessIscrizioneElencoOiv implements ExecutionListener {

    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessIscrizioneElencoOiv.class);
    public static final String TEMPI_PROCEDIMENTALI_DOMANDA = "tempiProcedimentaliDomanda";
    public static final String TEMPI_SOCCORSO_ISTRUTTORIO = "tempiSoccorsoIstruttorio";
    public static final String TEMPI_PREAVVISO_RIGETTO = "tempiPreavvisoRigetto";
    public static final String DATA_INIZIO_SOCCORSO_ISTRUTTORIO = "dataInizioSoccorsoIstruttorio";
    public static final String DATA_FINE_SOCCORSO_ISTRUTTORIO = "dataFineSoccorsoIstruttorio";
    public static final String GIORNI_DURATA_SOCCORSO_ISTRUTTORIO = "giorniDurataSoccorsoIstruttorio";
    public static final String BOUNDARYTIMER_3 = "boundarytimer3";
    public static final String BOUNDARYTIMER_6 = "boundarytimer6";
    public static final String SOCCORSO_ISTRUTTORIA_FLAG = "soccorsoIstruttoriaFlag";
    public static final String DATA_INIZIO_PREAVVISO_RIGETTO = "dataInizioPreavvisoRigetto";
    public static final String DATA_FINE_PREAVVISO_RIGETTO = "dataFinePreavvisoRigetto";
    public static final String GIORNI_DURATA_PREAVVISO_RIGETTO = "giorniDurataPreavvisoRigetto";
    public static final String SCELTA_UTENTE = "sceltaUtente";
    public static final String ID_DOMANDA = "idDomanda";
    public static final String OIV = "oiv.";
    public static final String ISCRIVI_INELENCO = "oiv.iscrivi-inelenco";
    public static final String SOCCORSO_ISTRUTTORIO = "oiv.soccorso-istruttorio";
    public static final String PREAVVISO_RIGETTO = "oiv.preavviso-rigetto";
    public static final String COMUNICAZIONI = "oiv.comunicazioni";
    public static final String FILE_NAME = "fileName";

    @Inject
    private OperazioniTimer operazioniTimer;
    @Inject
    private CalcolaPunteggioFascia calcolaPunteggioFascia;
    @Inject
    @Autowired(required = false)
    private OivSetGroupsAndVisibility oivSetGroupsAndVisibility;
    @Inject
    private ManageSceltaUtente manageSceltaUtente;
    @Autowired(required = false)
    private RestTemplate oivRestTemplate;
    @Inject
    private Environment env;
    @Inject
    private ManageControlli manageControlli;
    @Inject
    private CreateOivPdf createOivPdf;
    @Inject
    private HistoryService historyService;
    @Autowired
    private FlowsAttachmentService attachmentService;

    private Expression faseEsecuzione;

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        String processInstanceId = execution.getProcessInstanceId();
        String sceltaUtente = Optional.ofNullable(execution.getVariable(SCELTA_UTENTE)).filter(String.class::isInstance)
                .map(String.class::cast).orElse("start");

        Date dataNow = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String simpleDataNow = formatter.format(dataNow);

        LOGGER.info("ProcessInstanceId: " + processInstanceId);
        boolean aggiornaGiudizioFinale = true;
        boolean nonAggiornaGiudizioFinale = false;
        FaseEsecuzioneEnum faseEsecuzioneValue = FaseEsecuzioneEnum
                .fromValue(faseEsecuzione.getValue(execution).toString());
        LOGGER.info("-- faseEsecuzione: {}", faseEsecuzioneValue);
        switch (faseEsecuzioneValue) {
        case PROCESS_START: {
            oivSetGroupsAndVisibility.configuraVariabiliStart(execution);
            manageControlli.verificaUnicaDomandaAttivaUtente(execution);
            calcolaPunteggioFascia.settaNoAllOggettoSoccrso(execution);
        }
            ;
            break;
        case SMISTAMENTO_START:
            break;
        case SMISTAMENTO_END: {
            operazioniTimer.determinaTimerScadenzaTermini(execution, BOUNDARYTIMER_3);
        }
            ;
            break;
        case ISTRUTTORIA_START: {
            if (Optional.ofNullable(execution.getVariable(SOCCORSO_ISTRUTTORIA_FLAG)).filter(o -> o.equals("1"))
                    .isPresent()) {
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
            } else {
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
            }
        }
            ;
            break;
        case ISTRUTTORIA_END: {
            if (!execution.getVariable("sceltaUtente").equals("richiesta_soccorso_istruttorio")) {
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
            }
        }
            ;
            break;
        case SOCCORSO_ISTRUTTORIO_START: {
            // Sospendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso
            // di scadenza tempi proderumantali (boundarytimer6)
            operazioniTimer.sospendiTimerTempiProceduramentali(execution, BOUNDARYTIMER_3, BOUNDARYTIMER_6);
            execution.setVariable(DATA_INIZIO_SOCCORSO_ISTRUTTORIO, simpleDataNow);
            final Pair<String, byte[]> pair = createOivPdf.creaPdfOiv(execution, soccorsoIstruttorio.name());
            soccorsoIstruttorio(Optional.ofNullable(execution.getVariable(ID_DOMANDA)).filter(String.class::isInstance)
                    .map(String.class::cast).orElse(null), pair.getFirst(), pair.getSecond());
        }
            ;
            break;
        case SOCCORSO_ISTRUTTORIO_END: {
            // Riprendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso
            // di scadenza tempi proderumantali (boundarytimer6)
            operazioniTimer.riprendiTimerTempiProceduramentali(execution, BOUNDARYTIMER_3, BOUNDARYTIMER_6);
            execution.setVariable(DATA_FINE_SOCCORSO_ISTRUTTORIO, simpleDataNow);
            execution.setVariable(GIORNI_DURATA_SOCCORSO_ISTRUTTORIO,
                    operazioniTimer.calcolaGiorniTraDateString(
                            execution.getVariable(DATA_INIZIO_SOCCORSO_ISTRUTTORIO).toString(),
                            execution.getVariable(DATA_FINE_SOCCORSO_ISTRUTTORIO).toString()));
        }
            ;
            break;
        case CAMBIO_ISTRUTTORE_START:
            break;
        case CAMBIO_ISTRUTTORE_END:
            break;
        case VALUTAZIONE_START:
            break;
        case VALUTAZIONE_END: {
            LOGGER.info("VALUTAZIONE_END");
            if (!execution.getVariable("sceltaUtente").equals("richiesta_soccorso_istruttorio")) {
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
            }
        }
            ;
            break;
        case PREAVVISO_RIGETTO_START: {
            // Estende il timer di scadenza tempi proderumantali (boundarytimer3) a 1 anno
            operazioniTimer.setTimerScadenzaTermini(execution, BOUNDARYTIMER_3, 1, 0, 0, 0, 0);
            // Estende il timer di avviso scadenza tempi proderumantali (boundarytimer6) a
            // 25 giorni
            operazioniTimer.setTimerScadenzaTermini(execution, BOUNDARYTIMER_6, 1, 0, 0, 0, 0);
            execution.setVariable(DATA_INIZIO_PREAVVISO_RIGETTO, simpleDataNow);
            FlowsAttachment fileRecuperato = (FlowsAttachment) execution.getVariable("preavvisoRigetto");

            preavvisoRigetto(
                    Optional.ofNullable(execution.getVariable(ManageProcessIscrizioneElencoOiv.ID_DOMANDA))
                            .filter(String.class::isInstance).map(String.class::cast).orElse(null),
                    fileRecuperato.getName(), attachmentService.getAttachmentContentBytes(fileRecuperato));
        }
            ;
            break;
        case PREAVVISO_RIGETTO_END: {
            // Estende il timer di scadenza tempi proderumantali (boundarytimer3) a 30
            // giorni
            operazioniTimer.setTimerScadenzaTermini(execution, BOUNDARYTIMER_3, 0, 0, 30, 0, 0);
            // Estende il timer di avviso scadenza tempi proderumantali (boundarytimer6) a
            // 25 giorni
            operazioniTimer.setTimerScadenzaTermini(execution, BOUNDARYTIMER_6, 0, 0, 25, 0, 0);
            execution.setVariable(DATA_FINE_PREAVVISO_RIGETTO, simpleDataNow);
            execution.setVariable(GIORNI_DURATA_PREAVVISO_RIGETTO,
                    operazioniTimer.calcolaGiorniTraDateString(
                            execution.getVariable(DATA_INIZIO_PREAVVISO_RIGETTO).toString(),
                            execution.getVariable(DATA_FINE_PREAVVISO_RIGETTO).toString()));
        }
            ;
            break;
        case ISTRUTTORIA_SU_PREAVVISO_START:
            break;
        case ISTRUTTORIA_SU_PREAVVISO_END: {
            calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
        }
            ;
            break;
        case VALUTAZIONE_PREAVVISO_START:
            break;
        case VALUTAZIONE_PREAVVISO_END: {
            calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
        }
            ;
            break;
        case FIRMA_DG_RIGETTO_START:
            break;
        case FIRMA_DG_RIGETTO_END: {
            final Optional<FlowsAttachment> flowsAttachment = Optional
                    .ofNullable(attachmentService.getAttachementsForProcessInstance(processInstanceId))
                    .map(stringFlowsAttachmentMap -> stringFlowsAttachmentMap.entrySet().stream())
                    .filter(entryStream -> {
                        return entryStream.anyMatch(stringFlowsAttachmentEntry -> {
                            return stringFlowsAttachmentEntry.getKey().equals(rigetto.name())
                                    || stringFlowsAttachmentEntry.getKey().equals(rigettoMotivato.name())
                                    || stringFlowsAttachmentEntry.getKey().equals(rigettoDopoPreavviso.name())
                                    || stringFlowsAttachmentEntry.getKey().equals(rigettoDopo10Giorni.name())
                                    || stringFlowsAttachmentEntry.getKey().equals(RigettoDef10Giorni.name());
                        });
                    }).map(entryStream -> entryStream.findAny())
                    .map(stringFlowsAttachmentEntry -> stringFlowsAttachmentEntry.get().getValue());
            if (flowsAttachment.isPresent()) {
                comunicazioni(
                        Optional.ofNullable(execution.getVariable(ID_DOMANDA)).filter(String.class::isInstance)
                                .map(String.class::cast).orElse(null),
                        flowsAttachment.get().getName(),
                        attachmentService.getAttachmentContentBytes(flowsAttachment.get()), rigetto.name());
            }
        }
            break;
        case END_IMPROCEDIBILE: {
            execution.setVariable(statoFinaleDomanda.name(), "IMPROCEDIBILE");
            final Pair<String, byte[]> pair = createOivPdf.creaPdfOiv(execution, improcedibile.name());
            comunicazioni(
                    Optional.ofNullable(execution.getVariable(ID_DOMANDA)).filter(String.class::isInstance)
                            .map(String.class::cast).orElse(null),
                    pair.getFirst(), pair.getSecond(), improcedibile.name());

        }
            ;
            break;
        case END_APPROVATA: {
            execution.setVariable("numeroIscrizioneInElenco",
                    iscriviInElenco(Optional.ofNullable(execution.getVariable(ID_DOMANDA))
                            .filter(String.class::isInstance).map(String.class::cast).orElse(null)));
            execution.setVariable(statoFinaleDomanda.name(), "DOMANDA APPROVATA");
        }
            ;
            break;
        case END_RESPINTA: {
            execution.setVariable(statoFinaleDomanda.name(), "RESPINTA");
        }
            ;
            break;
        case AVVISO_SCADENZA_TEMPI_PROCEDURALI_START: {
            execution.setVariable(TEMPI_PROCEDIMENTALI_DOMANDA, "IN SCADENZA");
        }
            ;
            break;
        case SCADENZA_TEMPI_PROCEDURALI_START: {
            execution.setVariable(TEMPI_PROCEDIMENTALI_DOMANDA, "SCADUTI");
        }
            ;
            break;
        case SCADENZA_TEMPI_SOCCORSO_ISTRUTTORIO: {
            execution.setVariable(TEMPI_SOCCORSO_ISTRUTTORIO, "SCADUTI");
        }
            ;
            break;
        case SCADENZA_TEMPI_PREAVVISO_RIGETTO: {
            execution.setVariable(TEMPI_PREAVVISO_RIGETTO, "SCADUTI");
        }
            ;
            break;
        case PROCESS_END: {
            oivSetGroupsAndVisibility.configuraVariabiliEnd(execution);
        }
            ;
            break;
        default:
            break;
        }
        // Codice per gestire le Scelte
        manageSceltaUtente.azioneScelta(execution, faseEsecuzioneValue.getValue(),
                SceltaUtenteEnum.fromValue(sceltaUtente));
        LOGGER.info("sceltaUtente: {}", sceltaUtente);
        // print della fase
        LOGGER.info("dettagli Istanza di flusso: {}", execution.getVariable("name"));

    }

    private String iscriviInElenco(String id) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(env.getProperty(ISCRIVI_INELENCO))
                .queryParam(ID_DOMANDA, id);
        return Optional.ofNullable(oivRestTemplate.getForEntity(builder.buildAndExpand().toUri(), Map.class))
                .filter(mapResponseEntity -> mapResponseEntity.getStatusCode() == HttpStatus.OK)
                .map(ResponseEntity::getBody).map(map -> map.get("progressivo")).map(Integer.class::cast)
                .map(String::valueOf).orElse(null);
    }

    private void soccorsoIstruttorio(String id, String fileName, byte[] bytes) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(env.getProperty(SOCCORSO_ISTRUTTORIO))
                .queryParam(ID_DOMANDA, id).queryParam(FILE_NAME, fileName);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
                map, headers);
        oivRestTemplate.postForEntity(builder.buildAndExpand().toUri(), requestEntity, Void.class);
    }

    private void comunicazioni(String id, String fileName, byte[] bytes, String type) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(env.getProperty(COMUNICAZIONI))
                .queryParam(ID_DOMANDA, id).queryParam(FILE_NAME, fileName).queryParam("type", type);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
                map, headers);
        oivRestTemplate.postForEntity(builder.buildAndExpand().toUri(), requestEntity, Void.class);
    }

    private void preavvisoRigetto(String id, String fileName, byte[] bytes) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(env.getProperty(PREAVVISO_RIGETTO))
                .queryParam(ID_DOMANDA, id).queryParam(FILE_NAME, fileName);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
                map, headers);

        oivRestTemplate.postForEntity(builder.buildAndExpand().toUri(), requestEntity, Void.class);

    }
}
