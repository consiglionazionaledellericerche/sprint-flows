package it.cnr.si.flows.ng.resource;

import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_DESCRIZIONE;
import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_STATO;
import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_TITOLO;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import javax.inject.Inject;

import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import it.cnr.si.service.ExternalMessageSender;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;

import static it.cnr.si.flows.ng.utils.Utils.*;

@Controller
@RequestMapping("api/admin")
@Secured(AuthoritiesConstants.ADMIN)
@Profile("cnr")
public class FlowsCnrAdminTools {
    
    private final Logger log = LoggerFactory.getLogger(FlowsCnrAdminTools.class);

    @Inject
    private HistoryService historyService;
    @Inject
    private AceService aceService;
    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private ExternalMessageSender extenalMessageSender;
    

    @RequestMapping(value = "/resendExternalMessages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> resendExternalMessages() {

        log.info("Resending External Messages (manual trigger)");
        extenalMessageSender.sendMessages();
        extenalMessageSender.sendErrorMessages();
        return ResponseEntity.ok().build();
    }
    
    /**
     * La ddMMyyyy deve essere in format dd/MM/yyyy
     * @param ddMMyyyy La startDate deve essere in format dd/MM/yyyy
     * @return
     * @throws ParseException
     */
    @RequestMapping(value = "firmatario-errato/{ddMMyyyy:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<String>>> getFirmatarioErrato(@PathVariable String ddMMyyyy) throws ParseException {
        
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        Date start = sdf.parse(ddMMyyyy);
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("covid19")
                .unfinished()
                .startedAfter(start)
                .orderByProcessInstanceStartTime().asc()
                .list();
        
        Map<String, BossDto> bossCache = new HashMap<String, BossDto>(); // uso una cache per risparmiare sui roundtrip con ACE
        List<String> results = new ArrayList<String>();
        List<String> errors = new ArrayList<String>();
        String info = "Dal "+ ddMMyyyy +" ad oggi ci sono "+ instances.size() +" flussi ancora attivi, seguono eventuali incongruenze di assegnazioni";
        results.add(info);
        log.info(info);
        
        ForkJoinPool customThreadPool = new ForkJoinPool(6);
        customThreadPool.submit(
                () -> instances.parallelStream().forEach(i -> {
                    String gruppoFirmatarioAttuale = historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(i.getId())
                            .variableName("gruppoResponsabileProponente")
                            .singleResult()
                            .getValue()
                            .toString();
                    String initiator = historyService.createHistoricVariableInstanceQuery()
                            .processInstanceId(i.getId())
                            .variableName("initiator")
                            .singleResult()
                            .getValue()
                            .toString();
                    String gruppoFirmatarioDellUtente = null;
                    String dbsfa = null;
                    String dsfa = null;
                    String cdsuosfa = null;
                    String usernameBoss = null;
                    String dbsfu = null;
                    String dsfu = null;
                    String cdsuosfu = null;
                    try {
                        SimpleEntitaOrganizzativaWebDto strutturaFirmatarioAttuale = aceBridgeService.getStrutturaById(Integer.parseInt(gruppoFirmatarioAttuale.split("@")[1]));
                        dsfa = strutturaFirmatarioAttuale.getDenominazione();
                        cdsuosfa = strutturaFirmatarioAttuale.getCdsuo();
                        BossDto boss = bossCache.computeIfAbsent(initiator, k -> aceBridgeService.bossFirmatarioByUsername(initiator));
                        usernameBoss = boss.getUtente().getUsername();
                        gruppoFirmatarioDellUtente = "responsabile-struttura@"+ boss.getEntitaOrganizzativa().getId();
                        SimpleEntitaOrganizzativaWebDto strutturaFirmatarioDellUtente = aceBridgeService.getStrutturaById(Integer.parseInt(gruppoFirmatarioDellUtente.split("@")[1]));
                        dsfu = strutturaFirmatarioDellUtente.getDenominazione();
                        cdsuosfu = strutturaFirmatarioDellUtente.getCdsuo();
                        if(!gruppoFirmatarioAttuale.equals(gruppoFirmatarioDellUtente)) {
                            String e = "Il flusso "+ i.getId() +" avviato dall'utente "+ initiator
                                    + " il giorno "+ i.getStartTime()
                                    + " è andato al gruppo "+ gruppoFirmatarioAttuale
                                    + " ("+ dbsfa +" - "+ dsfa +" - "+ cdsuosfa +")"
                                    + " invece che a "+ usernameBoss +" del gruppo "+ gruppoFirmatarioDellUtente
                                    + " ("+ dbsfu +" - "+ dsfu +" - "+ cdsuosfu +")";
                            log.info(e);
                            results.add(e);
                        }
                    } catch (Exception e) {
                        String err = "firmatario-errato: Errore nel processamento del flusso "+ i.getId() 
                        +" avviato dall'utente "+initiator
                        +" il giorno "+ i.getStartTime()
                        +" che è andato al gruppo "+ gruppoFirmatarioAttuale
                        + " ("+ dbsfa +" - "+ dsfa +" - "+ cdsuosfa +")"
                        +" invece che a "+ usernameBoss +" del gruppo "+ gruppoFirmatarioDellUtente +")"
                        + " ("+ dbsfu +" - "+ dsfu +" - "+ cdsuosfu +")"
                        +" con messaggio: "+ e.getMessage();
                        log.error(err, e);
                        errors.add(err);
                    }
                })).join();
        
        Map<String, List<String>> result = new HashMap<>();
        result.put("results", results);
        result.put("errors", errors);
        return ResponseEntity.ok(result);
    }
    
    // mtrycz 06/01/21 - metodo disabilitato, ci era servito una volta.
    // @RequestMapping(value = "aggiornaName/{aggiorna}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> agiornaName(@PathVariable("aggiorna") Boolean aggiorna) {
        
        List<HistoricProcessInstance> instances = historyService
            .createHistoricProcessInstanceQuery()
            .processInstanceNameLike("{\"stato\":\"\"%") // prendo le PI con lo stato vuoto
            .list();
        
        instances.stream().forEach(pi -> {

            log.info("Processo la ProcessInstance "+ pi.getId() +" con name "+ pi.getName());
            
            HistoricVariableInstance statoFinale = historyService
                    .createHistoricVariableInstanceQuery()
                    .processInstanceId(pi.getId())
                    .variableName("statoFinale")
                    .singleResult();
            
            if (statoFinale == null || statoFinale.getValue() == null) {
                log.info("Questa pi non ha lo statoFinale: "+ pi.getId());
                return;
            }
            
            String stato = statoFinale.getValue().toString();
            String name = getName(pi.getId(), stato);
            
            log.info("Inserisco nella ProcessInstance "+ pi.getId() +" il name:"+ name);
            
            if (aggiorna) 
                historyService
                    .createNativeHistoricProcessInstanceQuery()
                    .sql("update act_hi_procinst set name_ = #{piname} where proc_inst_id_ = #{piid} ")
                    .parameter("piname", name)
                    .parameter("piid", pi.getId())
                    .singleResult();
            
            log.info("ProcessInstance "+ pi.getId() +" aggiornata con successo");
        });
        
        return ResponseEntity.ok().build();
    }

    private String getName(String processInstanceId, String stato) {

        String initiator = "";
        String titolo = "";
        String descrizione = "";

        initiator = historyService
            .createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName(INITIATOR)
            .singleResult().getValue().toString();
        
        titolo = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(TITOLO)
                .singleResult().getValue().toString();
        
        descrizione = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .variableName(DESCRIZIONE)
                .singleResult().getValue().toString();

        org.json.JSONObject name = new org.json.JSONObject();
        name.put(DESCRIZIONE, ellipsis(descrizione, LENGTH_DESCRIZIONE));
        name.put(TITOLO, ellipsis(titolo, LENGTH_TITOLO));
        name.put(STATO, ellipsis(stato, LENGTH_STATO) );
        name.put(INITIATOR, initiator);

        return name.toString();
    }
}
