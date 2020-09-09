package it.cnr.si.flows.ng.resource;

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
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;

@Controller
@RequestMapping("api/attachments")
@Secured(AuthoritiesConstants.ADMIN)
public class FlowsAdminTools {
    
    private final Logger log = LoggerFactory.getLogger(FlowsAdminTools.class);

    
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private HistoryService historyService;
    @Inject
    private AceService aceService;
    @Inject
    private AceBridgeService aceBridgeService;
    
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
                        EntitaOrganizzativaWebDto strutturaFirmatarioAttuale = aceBridgeService.getStrutturaById(Integer.parseInt(gruppoFirmatarioAttuale.split("@")[1]));
                        dbsfa = strutturaFirmatarioAttuale.getDenominazioneBreve();
                        dsfa = strutturaFirmatarioAttuale.getDenominazione();
                        cdsuosfa = strutturaFirmatarioAttuale.getCdsuo();
                        BossDto boss = bossCache.computeIfAbsent(initiator, k -> aceService.bossFirmatarioByUsername(initiator));
                        usernameBoss = boss.getUsername();
                        gruppoFirmatarioDellUtente = "responsabile-struttura@"+ boss.getIdEntitaOrganizzativa();
                        EntitaOrganizzativaWebDto strutturaFirmatarioDellUtente = aceBridgeService.getStrutturaById(Integer.parseInt(gruppoFirmatarioDellUtente.split("@")[1]));
                        dbsfu = strutturaFirmatarioDellUtente.getDenominazioneBreve();
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

}
