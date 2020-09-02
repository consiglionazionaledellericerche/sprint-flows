package it.cnr.si.flows.ng.resource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.MembershipService;
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
    
    @RequestMapping(value = "firmatario-errato", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getFirmatarioErrato() throws ParseException {
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Date start = sdf.parse("01/09/2020");
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("covid19")
                .unfinished()
                .startedAfter(start)
//                .includeProcessVariables()
                .list();
        
        List<String> result = new ArrayList<String>();
        Map<String, BossDto> bossCache = new HashMap<String, BossDto>(); // uso una cache per risparmiare sui roundtrip con ACE
        
        instances.forEach(i -> {
            try {
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
                BossDto boss = bossCache.computeIfAbsent(initiator, k -> aceService.bossFirmatarioByUsername(initiator));
                String gruppoFirmatarioDellUtente = "responsabile-struttura@"+ boss.getIdEntitaOrganizzativa();
                if(!gruppoFirmatarioAttuale.equals(gruppoFirmatarioDellUtente)) {
                    String e = "Il Flusso "+ i.getId() +" avviato dall'utente "+ initiator +" è andato al Gruppo "+ gruppoFirmatarioAttuale +
                            " invece che a "+ boss.getUsername() +" del gruppo "+ gruppoFirmatarioDellUtente;
                    log.info(e);
                    result.add(e);
                }
            } catch (Exception e) {
                log.error("firmatario-errato: Errore nel processamento del flusso "+ i.getId(), e);
            }
        });
        
        return ResponseEntity.ok(result);
    }

}
