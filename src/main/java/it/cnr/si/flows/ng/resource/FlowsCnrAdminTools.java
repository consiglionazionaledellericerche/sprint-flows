package it.cnr.si.flows.ng.resource;

import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_DESCRIZIONE;
import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_STATO;
import static it.cnr.si.flows.ng.service.FlowsTaskService.LENGTH_TITOLO;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.websocket.server.PathParam;

import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricIdentityLink;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.cmd.AddIdentityLinkForProcessInstanceCmd;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.activiti.engine.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntity;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntityManager;
import org.activiti.engine.task.IdentityLinkType;
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
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.scritture.BossDto;
import it.cnr.si.service.dto.anagrafica.simpleweb.SimpleEntitaOrganizzativaWebDto;
import org.springframework.web.bind.annotation.RequestParam;

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
    @Inject
    private ProcessEngine processEngine;

    @RequestMapping(value = "/resendExternalMessages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> resendExternalMessages() {

        log.info("Resending External Messages (manual trigger)");
        extenalMessageSender.sendMessages();
        extenalMessageSender.sendErrorMessages();
        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "firma-errata-missioni", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<String>>> getErroriFirmaMissioni() {
        
        List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
            .processDefinitionKey("missioni")
            .finished()
            .list();
        
        int total = processInstances.size();
        log.info(""+total);
        
        List<HistoricProcessInstance> filteredPIs = processInstances.parallelStream().filter(pi -> {
            Map<String, Object> variables = historyService.
                    createHistoricProcessInstanceQuery().
                    processInstanceId(pi.getId()).
                    includeProcessVariables().
                    singleResult()
                    .getProcessVariables();
            String validazioneSpesaFlag = (String)variables.get("validazioneSpesaFlag");
            FlowsAttachment fileMissione = (FlowsAttachment)variables.get("missioni");
            String statoFinaleDomanda = String.valueOf(variables.get("STATO_FINALE_DOMANDA"));
            
            if (!statoFinaleDomanda.startsWith("FIRMATO"))
                return false;
            
            if (fileMissione == null) {
                log.error("La Process Instance "+ pi.getId() +" non ha l'allegato missioni");
            } else {
                if (validazioneSpesaFlag != null && validazioneSpesaFlag.equalsIgnoreCase("si")) {
                    if (fileMissione.getFilename().contains(".signed.signed."))
                        return false;
                } 
                if (fileMissione.getFilename().contains(".signed."))
                    return false;
                
            }
            return true;
        }).collect(Collectors.toList());
        
        
        log.info(""+filteredPIs.size());

        Map<String, List<String>> result = new ConcurrentHashMap<>();
        
        filteredPIs.parallelStream().forEach(pi -> {
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(pi.getId()).list();
            
            for ( HistoricTaskInstance task : tasks) {
                historyService.getHistoricIdentityLinksForTask(task.getId()).stream()
                .filter(il -> {
                    return il.getType().equals(TASK_EXECUTOR);
                }).forEach(il -> {
                    String esecutore = il.getUserId();
                    List<String> processList = result.containsKey(esecutore) ? result.get(esecutore) : new ArrayList<String>();
                    processList.add(pi.getId());
                    result.put(esecutore, processList);
                });
            }
        });
        
        return ResponseEntity.ok(result);
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
    
    @RequestMapping(value = "addHistoricIdentityLink", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> addHistoricIdentityLink(
            @RequestParam("procInstId") String procInstId,
            @RequestParam(name = "userId", required = false) String userId,
            @RequestParam(value = "groupId", required = false) String groupId) {

        AddIdentityLinkForHistoricProcessInstanceCmd cmd = new AddIdentityLinkForHistoricProcessInstanceCmd(procInstId, userId, groupId, Utils.PROCESS_VISUALIZER);
        processEngine.getManagementService().executeCommand(cmd);

        return ResponseEntity.ok().build();
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
    
    public class AddIdentityLinkForHistoricProcessInstanceCmd implements Command<Void>, Serializable {

        private static final long serialVersionUID = 1L;

        protected String processInstanceId;

        protected String userId;

        protected String groupId;

        protected String type;

        public AddIdentityLinkForHistoricProcessInstanceCmd(String processInstanceId, String userId, String groupId, String type) {
          validateParams(processInstanceId, userId, groupId, type);
          this.processInstanceId = processInstanceId;
          this.userId = userId;
          this.groupId = groupId;
          this.type = type;
        }

        protected void validateParams(String processInstanceId, String userId, String groupId, String type) {

          if (processInstanceId == null) {
            throw new ActivitiIllegalArgumentException("processInstanceId is null");
          }

          if (type == null) {
            throw new ActivitiIllegalArgumentException("type is required when adding a new process instance identity link");
          }

          if (userId == null && groupId == null) {
            throw new ActivitiIllegalArgumentException("userId and groupId cannot both be null");
          }

        }

        public Void execute(CommandContext commandContext) {

          HistoricProcessInstance processInstance = commandContext.getHistoricProcessInstanceEntityManager().findHistoricProcessInstance(processInstanceId);

          if (processInstance == null) {
            throw new ActivitiObjectNotFoundException("Cannot find process instance with id " + processInstanceId, HistoricProcessInstance.class);
          }

          HistoricIdentityLinkEntity il = new HistoricIdentityLinkEntity();
          il.setProcessInstanceId(processInstanceId);
          il.setGroupId(this.groupId);
          il.setUserId(this.userId);
          il.setType(this.type);

          commandContext.getDbSqlSession().insert(il);
          return null;
        }

      }

}
