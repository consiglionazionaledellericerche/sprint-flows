package it.cnr.si.flows.ng.listeners.cnr.covid19;


import it.cnr.si.firmadigitale.firma.arss.stub.PdfSignApparence;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.*;
import it.cnr.si.flows.ng.utils.CNRPdfSignApparence;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.delegate.Expression;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Component
@Profile("cnr")
public class ManageCovid19_v1 implements ExecutionListener {
    public static final String STATO_FINALE_GRADUATORIA = "statoFinaleDomanda";
    private static final long serialVersionUID = 686169707042367215L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageCovid19_v1.class);
    @Inject
    private FirmaDocumentoService firmaDocumentoService;
    @Inject
    private ProtocolloDocumentoService protocolloDocumentoService;
    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;
    @Inject
    private StartCovid19SetGroupsAndVisibility_v1 startCovid19SetGroupsAndVisibility_v1;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private TaskService taskService;
    @Inject
    private FlowsCsvService flowsCsvService;
    @Inject
    private FlowsAttachmentService flowsAttachmentService;
    @Inject
    private FlowsTaskService flowsTaskService;
    @Inject
    private FlowsPdfService flowsPdfService;
    @Inject
    private FlowsPdfBySiglaRestService flowsPdfBySiglaRestService;
    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private AceService aceService;
	@Inject
	private PdfSignApparence covid19Apparence;

    private Expression faseEsecuzione;


    @Override
    public void notify(DelegateExecution execution) throws Exception {

        Map<String, FlowsAttachment> attachmentList;
        String processInstanceId = execution.getProcessInstanceId();
        String executionId = execution.getId();
        String stato = execution.getCurrentActivityName();
        String sceltaUtente = "start";

        if (execution.getVariable("sceltaUtente") != null) {
            sceltaUtente = (String) execution.getVariable("sceltaUtente");
        }

        LOGGER.info("ProcessInstanceId: " + processInstanceId);
        String faseEsecuzioneValue = "noValue";
        faseEsecuzioneValue = faseEsecuzione.getValue(execution).toString();
        LOGGER.info("-- azioneScelta: " + faseEsecuzioneValue + " con sceltaUtente: " + sceltaUtente);

        switch (faseEsecuzioneValue) {
            // START
            case "process-start": {
                startCovid19SetGroupsAndVisibility_v1.configuraVariabiliStart(execution);
            }
            break;
            case "firma-start": {
                // INSERIMENTO VARIABILI FLUSSO
                execution.setVariable("titolo", "Scheda " + execution.getVariable("tipoAttivita") + " - " + execution.getVariable("initiator"));
                execution.setVariable("descrizione", "Scheda Attività - " + execution.getVariable("mese") + " " + execution.getVariable("anno"));

                //PARAMETRI GENERAZIONE PDF
                String tipoAttivita = "rendicontazione";
                if (execution.getVariable("tipoAttivita") != null) {
                    tipoAttivita = execution.getVariable("tipoAttivita").toString();
                }
                String nomeFile = tipoAttivita + ".pdf";
                String labelFile = "Monitoraggio Attività Personale";
                String report = "scrivaniadigitale/smart_working.jrxml";
                //tipologiaDoc è la tipologia del file
                String tipologiaDoc = Enum.PdfType.valueOf("monitoraggioAttivitaCovid19").name();
                String utenteFile = execution.getVariable("initiator").toString();

                //valoreParam per il json che racchiude i dati della stampa
                JSONObject valoreParamJson = new JSONObject();
                valoreParamJson.put("matricola", execution.getVariable("matricola"));
                valoreParamJson.put("nomeCognomeUtente", execution.getVariable("nomeCognomeUtente"));
                valoreParamJson.put("tipoContratto", execution.getVariable("tipoContratto"));
                valoreParamJson.put("cds", execution.getVariable("cds"));
                valoreParamJson.put("direttore", execution.getVariable("direttore"));
                valoreParamJson.put("mese", execution.getVariable("mese").toString());
                valoreParamJson.put("anno", execution.getVariable("anno").toString());
                valoreParamJson.put("attivita_svolta",
                        Optional.ofNullable(execution.getVariable("attivita"))
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .map(s -> s.replaceAll("strong>", "b>"))
                                .map(s -> s.replaceAll("em>", "i>"))
                                .orElse("")
                );
                valoreParamJson.put("tipoAttivita", execution.getVariable("tipoAttivita").toString());
                if (execution.getVariable("tipoAttivita").equals("programmazione")) {
                    valoreParamJson.put("modalita",
                            Optional.ofNullable(execution.getVariable("modalita"))
                                    .filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .map(s -> s.replaceAll("strong>", "b>"))
                                    .map(s -> s.replaceAll("em>", "i>"))
                                    .orElse("")
                    );
                    valoreParamJson.put("dataAvvioSmartWorking", execution.getVariable("dataAvvioSmartWorking"));
                }

                String valoreParam = valoreParamJson.toString();

                if (tipoAttivita.equals("rendicontazione")) {
                    labelFile = "Rendicontazione Attività Personale";
                    execution.setVariable("modalita", null);
                    execution.setVariable("dataAvvioSmartWorking", null);
                } else {
                    labelFile = "Programmazione Attività Personale";
                }
                // UPDATE VARIABILI FLUSSO
                flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, stato);
                // GENERAZIONE PDF
                flowsPdfBySiglaRestService.makePdf(execution, nomeFile, labelFile, report, valoreParam, tipologiaDoc, processInstanceId, utenteFile);
            }
            break;
            case "firma-end": {
                if (sceltaUtente != null && sceltaUtente.equals("Firma")) {
                    firmaDocumentoService.eseguiFirma(execution, Enum.PdfType.valueOf("monitoraggioAttivitaCovid19").name(), covid19Apparence);
                }
            }
            break;
            case "modifica-start": {
                //AGGIORNA DIRETTORE

                BossDto utenteBoss = aceService.bossFirmatarioByUsername(execution.getVariable("initiator").toString());
                execution.setVariable("direttore", utenteBoss.getNome() + " " + utenteBoss.getCognome());
            }
            case "protocollo-end": {
                if (sceltaUtente != null && sceltaUtente.equals("Protocolla")) {
                    protocolloDocumentoService.protocolla(execution, execution.getVariable("tipoAttivita").toString());
                }
            }
            break;
            case "endevent-covid19-start": {
                flowsProcessInstanceService.updateSearchTerms(executionId, processInstanceId, "APPROVATO");
            }
            break;

            case "process-end": {
                //

            }
            break;
            // DEFAULT
            default: {
            }
            break;

        }
    }

    @Bean(name = {"covid19Apparence"})
    @ConfigurationProperties(prefix = "cnr.firma.covid19")
    public CNRPdfSignApparence create() throws IOException {
        return new CNRPdfSignApparence();
    }

}
