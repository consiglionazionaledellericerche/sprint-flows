package it.cnr.si.flows.ng.listeners.oiv;


import it.cnr.si.flows.ng.listeners.oiv.service.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static it.cnr.si.flows.ng.utils.Enum.PdfType.improcedibile;


@Component
@Profile("oiv")
public class ManageProcessIscrizioneElencoOiv implements ExecutionListener {
    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageProcessIscrizioneElencoOiv.class);


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

    private Expression faseEsecuzione;

    @Override
    public void notify(DelegateExecution execution) throws Exception {

        String processInstanceId = execution.getProcessInstanceId();
        String sceltaUtente = "start";
        if (execution.getVariable("sceltaUtente") != null) {
            sceltaUtente = (String) execution.getVariable("sceltaUtente");
        }
        Date dataNow = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        String simpleDataNow = formatter.format(dataNow);


        LOGGER.info("ProcessInstanceId: " + processInstanceId);
        boolean aggiornaGiudizioFinale = true;
        boolean nonAggiornaGiudizioFinale = false;
        FaseEsecuzioneEnum faseEsecuzioneValue = FaseEsecuzioneEnum.fromValue(faseEsecuzione.getValue(execution).toString());
        switch (faseEsecuzioneValue) {
            case PROCESS_START: {
                LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
                oivSetGroupsAndVisibility.configuraVariabiliStart(execution);
                manageControlli.verificaUnicaDomandaAttivaUtente(execution);
                calcolaPunteggioFascia.settaNoAllOggettoSoccrso(execution);
            }
            ;
            break;
            case SMISTAMENTO_START: {
                LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case SMISTAMENTO_END: {
                LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
                operazioniTimer.determinaTimerScadenzaTermini(execution, "boundarytimer3");
            }
            ;
            break;
            case ISTRUTTORIA_START: {
                LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
                if ((execution.getVariable("soccorsoIstruttoriaFlag") != null) && (execution.getVariable("soccorsoIstruttoriaFlag").toString().equals("1"))) {
                    calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
                } else {
                    calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
                }
            }
            ;
            break;
            case ISTRUTTORIA_END: {
                LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
            }
            ;
            break;
            case SOCCORSO_ISTRUTTORIO_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                //Sospendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso di scadenza tempi proderumantali (boundarytimer6)
                operazioniTimer.sospendiTimerTempiProceduramentali(execution, "boundarytimer3", "boundarytimer6");
                execution.setVariable("dataInizioSoccorsoIstruttorio", simpleDataNow);
            }
            ;
            break;
            case SOCCORSO_ISTRUTTORIO_END: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                //Riprendo i timer di scadenza tempi proderumantali (boundarytimer3) e avviso di scadenza tempi proderumantali (boundarytimer6)
                operazioniTimer.riprendiTimerTempiProceduramentali(execution, "boundarytimer3", "boundarytimer6");
                execution.setVariable("dataFineSoccorsoIstruttorio", simpleDataNow);
                execution.setVariable("giorniDurataSoccorsoIstruttorio", operazioniTimer.calcolaGiorniTraDateString(execution.getVariable("dataInizioSoccorsoIstruttorio").toString(), execution.getVariable("dataFineSoccorsoIstruttorio").toString()));
            }
            ;
            break;
            case CAMBIO_ISTRUTTORE_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case CAMBIO_ISTRUTTORE_END: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case VALUTAZIONE_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case VALUTAZIONE_END: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
            }
            ;
            break;
            case PREAVVISO_RIGETTO_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                // Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 1 anno
                operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer3", 1, 0, 0, 0, 0);
                // Estende  il timer di avviso scadenza tempi proderumantali (boundarytimer6) a 25 giorni
                operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer6", 1, 0, 0, 0, 0);
                execution.setVariable("dataInizioPreavvisoRigetto", simpleDataNow);
            }
            ;
            break;
            case PREAVVISO_RIGETTO_END: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                // Estende  il timer di scadenza tempi proderumantali (boundarytimer3) a 30 giorni
                operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer3", 0, 0, 30, 0, 0);
                // Estende  il timer di avviso scadenza tempi proderumantali (boundarytimer6) a 25 giorni
                operazioniTimer.setTimerScadenzaTermini(execution, "boundarytimer6", 0, 0, 25, 0, 0);
                execution.setVariable("dataFinePreavvisoRigetto", simpleDataNow);
                execution.setVariable("giorniDurataPreavvisoRigetto", operazioniTimer.calcolaGiorniTraDateString(execution.getVariable("dataInizioPreavvisoRigetto").toString(), execution.getVariable("dataFinePreavvisoRigetto").toString()));
            }
            ;
            break;
            case ISTRUTTORIA_SU_PREAVVISO_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case ISTRUTTORIA_SU_PREAVVISO_END: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, aggiornaGiudizioFinale);
            }
            ;
            break;
            case VALUTAZIONE_PREAVVISO_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case VALUTAZIONE_PREAVVISO_END: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                calcolaPunteggioFascia.calcolaAggiornaGiudizioFinale(execution, nonAggiornaGiudizioFinale);
            }
            ;
            break;
            case FIRMA_DG_RIGETTO_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case FIRMA_DG_RIGETTO_END: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;
            case END_IMPROCEDIBILE: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                execution.setVariable("statoFinaleDomanda", "IMPROCEDIBILE");
                createOivPdf.CreaPdfOiv(execution, improcedibile.name());
            }
            ;
            break;
            case END_APPROVATA: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                execution.setVariable("numeroIscrizioneInElenco",
                        iscriviInElenco(Optional.ofNullable(execution.getVariable("idDomanda"))
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .orElse(null)));
                execution.setVariable("statoFinaleDomanda", "DOMANDA APPROVATA");
            }
            ;
            break;
            case END_RESPINTA: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                execution.setVariable("statoFinaleDomanda", "RESPINTA");
            }
            ;
            break;
            case AVVISO_SCADENZA_TEMPI_PROCEDURALI_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                execution.setVariable("tempiProcedimentaliDomanda", "IN SCADENZA");
            }
            ;
            break;
            case SCADENZA_TEMPI_PROCEDURALI_START: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                execution.setVariable("tempiProcedimentaliDomanda", "SCADUTI");
            }
            ;
            break;
            case SCADENZA_TEMPI_SOCCORSO_ISTRUTTORIO: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                execution.setVariable("tempiSoccorsoIstruttorio", "SCADUTI");
            }
            ;
            break;
            case SCADENZA_TEMPI_PREAVVISO_RIGETTO: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
                execution.setVariable("tempiPreavvisoRigetto", "SCADUTI");
            }
            ;
            break;
            case PROCESS_END: {
                LOGGER.info("-- faseEsecuzione: " + faseEsecuzioneValue);
                oivSetGroupsAndVisibility.configuraVariabiliEnd(execution);
            }
            ;
            break;
            default: {
                LOGGER.info("--faseEsecuzione: " + faseEsecuzioneValue);
            }
            ;
            break;

        }
        // Codice per gestire le Scelte
        manageSceltaUtente.azioneScelta(execution, faseEsecuzioneValue.getValue(), sceltaUtente);
        LOGGER.info("sceltaUtente: " + sceltaUtente);
        //print della fase
        LOGGER.info("dettagli Istanza di flusso: " + execution.getVariable("name"));

    }

    private String iscriviInElenco(String id) {
        final RelaxedPropertyResolver relaxedPropertyResolver = new RelaxedPropertyResolver(env, "oiv.");
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(relaxedPropertyResolver.getProperty("iscrivi-inelenco"))
                .queryParam("idDomanda", id);
        return Optional.ofNullable(oivRestTemplate.getForEntity(builder.buildAndExpand().toUri(), Map.class))
                .filter(mapResponseEntity -> mapResponseEntity.getStatusCode() == HttpStatus.OK)
                .map(ResponseEntity::getBody)
                .map(map -> map.get("progressivo"))
                .map(Integer.class::cast)
                .map(String::valueOf)
                .orElse(null);
    }

}
