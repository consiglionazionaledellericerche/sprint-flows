package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Callable;

import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.testAcquistiAvvisi;
import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
//@ActiveProfiles(profiles = "native,unittests,oiv")
public class FlowsProcessInstanceResourceTest {

    private static final int LOAD_TEST_PROCESS_INSTANCES = 700;
    private static int processDeleted = 0;
    @Autowired
    RuntimeService runtimeService;
    @Inject
    private HistoryService historyService;
    @Inject
    private FlowsTaskResource flowsTaskResource;
    @Inject
    private FlowsProcessInstanceResource flowsProcessInstanceResource;
    @Inject
    private TestServices util;
    @Inject
    private FlowsProcessDefinitionResource flowsProcessDefinitionResource;
    @Inject
    private RepositoryService repositoryService;
    @Inject
    private TaskService taskService;

    private StopWatch stopWatch = new StopWatch();
    private ProcessInstanceResponse processInstance;
    @Inject
    private Utils utils;



    @Before
    public void setUp() {
        HttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @After
    public void tearDown() {
        System.out.println(stopWatch.prettyPrint());
        processDeleted = processDeleted + util.myTearDown();
    }

    @Test
    public void testGetMyProcesses() throws Exception {
        processInstance = util.mySetUp(acquisti);
        String processInstanceID = verifyMyProcesses(1, 0);
//        La sospensione non viene usata dall`applicazione
//        // testo che, anche se una Process Instance viene sospesa, la vedo ugualmente
//        util.loginAdmin();
//        flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstanceID);
//
//        util.loginResponsabileAcquisti();
//        processInstanceID = verifyMyProcesses(1, 0);
        //testo che cancellandola una Process Instances NON la vedo tra i processi avviati da me
        util.loginAdmin();
        ResponseEntity response = flowsProcessInstanceResource.delete(processInstanceID, "TEST");
        assertEquals(OK, response.getStatusCode());
        util.loginResponsabileAcquisti();
        verifyMyProcesses(0, 0);
    }


    @Test(expected = AccessDeniedException.class)
    public void testGetProcessInstanceById() throws Exception {
        processInstance = util.mySetUp(acquisti);

        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) flowsProcessInstanceResource.getProcessInstanceById(processInstance.getId(), true);
        assertEquals(OK, response.getStatusCode());

        HistoricProcessInstanceResponse entity = (HistoricProcessInstanceResponse) ((HashMap) response.getBody()).get("entity");
        assertEquals(processInstance.getId(), entity.getId());
        assertEquals(processInstance.getBusinessKey(), entity.getBusinessKey());
        assertEquals(processInstance.getProcessDefinitionId(), entity.getProcessDefinitionId());

        HashMap appo = (HashMap) ((HashMap) response.getBody()).get("identityLinks");
        assertEquals(2, appo.size());
        HashMap identityLinks = (HashMap) appo.get(appo.keySet().toArray()[1]); // TODO
        assertEquals("[${gruppoSFD}]", identityLinks.get("candidateGroups").toString());
        assertEquals(0, ((HashSet) identityLinks.get("candidateUsers")).size());
        assertEquals(1, ((ArrayList) identityLinks.get("links")).size());
        assertNull(identityLinks.get("assignee"));

        HashMap history = (HashMap) ((ArrayList) ((HashMap) response.getBody()).get("history")).get(0);
        assertEquals(processInstance.getId(), ((HistoricTaskInstanceResponse) history.get("historyTask")).getProcessInstanceId());
        assertEquals(1, ((ArrayList) history.get("historyIdentityLink")).size());

        HashMap attachments = (HashMap) ((HashMap) response.getBody()).get("attachments");
        assertEquals(0, attachments.size());

        //verifica che gli utenti con ROLE_ADMIN POSSANO accedere al servizio
        util.loginAdmin();
        flowsProcessInstanceResource.getProcessInstanceById(processInstance.getId(), false);

        //verifica AccessDeniedException (risposta 403 Forbidden) in caso di accesso di utenti non autorizzati
        util.loginUser();
        flowsProcessInstanceResource.getProcessInstanceById(processInstance.getId(), false);
    }


//    @Test
//    public void testSuspend() throws Exception {
//        processInstance = util.mySetUp(acquisti);
//        assertEquals(false, processInstance.isSuspended());
//        //solo admin può sospendere il flow
//        util.loginAdmin();
//        ProcessInstanceResponse response = flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstance.getId());
//        assertEquals(true, response.isSuspended());
//    }


    @Test
    public void testGetVariable() throws Exception {
        processInstance = util.mySetUp(acquisti);
//        processInstance = util.mySetUp(iscrizioneElencoOiv);

    }


    @Test
    public void testAddAndDeleteGroupIdentityLink() throws Exception {
        processInstance = util.mySetUp(acquisti);
        //le funzionalità può essere acceduta solo con privileggi "ADMIN"
        util.loginAdmin();

        //test-junit@2216 come GRUPPO "VISUALIZZATORE"
        String groupId = "test-junit@2216";
        String userId = null;
        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertFalse(identityLinks.stream()
                            .anyMatch(a -> a.getGroupId() == groupId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));

        ResponseEntity<Void> response = flowsProcessInstanceResource.setIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        assertEquals(OK, response.getStatusCode());
        List<IdentityLink> newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(newIdentityLinks.size(), identityLinks.size() + 1);

        assertTrue(newIdentityLinks.stream()
                           .anyMatch(a -> a.getGroupId() == groupId &&
                                   a.getProcessInstanceId().equals(processInstance.getId())  &&
                                   a.getType() == Utils.PROCESS_VISUALIZER));

        //cancellazione IdentityLink aggiunto prima
        response = flowsProcessInstanceResource.deleteIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(OK, response.getStatusCode());
        assertFalse(newIdentityLinks.stream()
                            .anyMatch(a -> a.getGroupId() == groupId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));
    }


    @Test
    public void testAddAndDeleteUserIdentityLink() throws Exception {
        processInstance = util.mySetUp(acquisti);
        //le funzionalità può essere acceduta solo con privileggi "ADMIN"
        util.loginAdmin();

        //test-junit come UTENTE "VISUALIZZATORE"
        String groupId = null;
        String userId = "test-junit";
        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertFalse(identityLinks.stream()
                            .anyMatch(a -> a.getUserId() == userId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));

        ResponseEntity<Void> response = flowsProcessInstanceResource.setIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        assertEquals(OK, response.getStatusCode());
        List<IdentityLink> newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(newIdentityLinks.size(), identityLinks.size() + 1);

        assertTrue(newIdentityLinks.stream()
                           .anyMatch(a -> a.getUserId() == userId &&
                                   a.getProcessInstanceId().equals(processInstance.getId())  &&
                                   a.getType() == Utils.PROCESS_VISUALIZER));

        //cancellazione IdentityLink aggiunto prima
        response = flowsProcessInstanceResource.deleteIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(OK, response.getStatusCode());
        assertFalse(newIdentityLinks.stream()
                            .anyMatch(a -> a.getUserId() == userId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));
    }



    @Test()
    public void getProcessInstancesForURPTest() throws Exception {

        processInstance = util.mySetUp(testAcquistiAvvisi);
        LocalDate dataScadenzaAvvisoPreDetermina = LocalDate.of(2019, Month.SEPTEMBER, 4);

        //AVVISI SCADUTI
        util.loginPortaleCnr();
        // terminiRicorso > (oggi - startFlusso) ==> resultSet = 1
        ResponseEntity<List<Map<String, Object>>> res = flowsProcessInstanceResource
                .getProcessInstancesForURP((int) (dataScadenzaAvvisoPreDetermina.until(LocalDate.now(), ChronoUnit.DAYS) + 1), true, null, 0, 10, ASC);
        assertEquals(OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());

        // terminiRicorso < (oggi - startFlusso) ==> resultSet = 0
        res = flowsProcessInstanceResource
                .getProcessInstancesForURP((int) (dataScadenzaAvvisoPreDetermina.until(LocalDate.now(), ChronoUnit.DAYS) - 1), true, null, 0, 10, ASC);

        assertEquals(OK, res.getStatusCode());
        assertEquals(0, res.getBody().size());

        // vado avanti col flusso (Pre-determina -> Verifica))
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        String processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(acquisti.getProcessDefinition())
                .latestVersion()
                .singleResult()
                .getId();
        req.setParameter("processDefinitionId", processDefinition);
        req.setParameter("taskId", util.getFirstTaskId());
        req.setParameter("commento", "commento determina JUNIT ");
        req.setParameter("dataScadenzaAvvisoPreDetermina", "2019-09-04T00:00:00.000Z");
        req.setParameter("sceltaUtente", "PredisponiDetermina");
        req.setParameter("tipologiaAcquisizione", "Procedura ristretta");
        req.setParameter("tipologiaAcquisizioneId", "12");
        req.setParameter("strumentoAcquisizione", "PROCEDURA SELETTIVA - MEPA");
        req.setParameter("strumentoAcquisizioneId", "21");
        req.setParameter("tipologiaProceduraSelettiva", "economicamenteVantaggiosa");
        req.setParameter("rup", "marco.spasiano");
        req.setParameter("rup_label", "MARCO SPASIANO");
        req.setParameter("impegni_json", "[{\"descrizione\":\"Impegno numero 1\",\"percentualeIva\":20,\"importoNetto\":100,\"vocedispesa\":\"11001 - Arretrati per anni precedenti corrisposti al personale a tempo indeterminato\",\"vocedispesaid\":\"11001\",\"uo\":\"2216\",\"gae\":\"spaclient\",\"progetto\":\"Progetto impegno 1\"}]");

        util.loginResponsabileAcquisti();

        ResponseEntity<ProcessInstanceResponse> response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());

        // GARE SCADUTE
        util.loginPortaleCnr();
        // terminiRicorso > (oggi - startFlusso) ==> resultSet = 0 (LA LOGICA TEMPORALE DELLE GARE È "OPPOSTA" RISPETTO A QUELLA DEGLI AVVISI)
        res = flowsProcessInstanceResource
                .getProcessInstancesForURP((int)(dataScadenzaAvvisoPreDetermina.until(LocalDate.now(), ChronoUnit.DAYS) + 1), null, false, 0, 10, ASC);
        assertEquals(OK, res.getStatusCode());
        assertEquals(0, res.getBody().size());
        // terminiRicorso < (oggi - startFlusso) ==> resultSet = 1 (LA LOGICA TEMPORALE DELLE GARE È "OPPOSTA" RISPETTO A QUELLA DEGLI AVVISI)
        res = flowsProcessInstanceResource
                .getProcessInstancesForURP((int)(dataScadenzaAvvisoPreDetermina.until(LocalDate.now(), ChronoUnit.DAYS) - 1), null, true, 0, 10, ASC);
        assertEquals(OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }


    @Test
    public void getProcessInstancesForTrasparenzaTest() throws Exception {
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        processInstance = util.mySetUp(acquisti);
        // Finchè non esco dalla macro-fase "DECISIONE A CONTRATTARE" il flagIsTrasparenza è false quindi la Pi nn appare nella ricerca
        util.loginPortaleCnr();
        ResponseEntity<List<Map<String, Object>>> res = flowsProcessInstanceResource
                .getProcessInstancesForTrasparenza(0, 10, ASC);
        assertEquals(OK, res.getStatusCode());
        assertEquals(0, res.getBody().size());


        //visto che devo firmare la Decisione non posso superare questa fase nei test
        //(a meno che nn setti direttamente la variabile settata alla fine della macro-fase "DECISIONE A CONTRATTARE")
        util.loginAdmin();
        flowsProcessInstanceResource.setVariable(processInstance.getId(), Enum.VariableEnum.flagIsTrasparenza.name(), "true");

        util.loginPortaleCnr();
        res = flowsProcessInstanceResource
                .getProcessInstancesForTrasparenza(0, 10, ASC);
        assertEquals(OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }


    private Callable<Integer> getPIForTrasparenzaSize() throws ParseException {
        util.loginPortaleCnr();
        ResponseEntity<List<Map<String, Object>>> res1 = flowsProcessInstanceResource
                .getProcessInstancesForTrasparenza(0, 10, ASC);

        assertEquals(OK, res1.getStatusCode());
        return () -> res1.getBody().size();
    }


    private String getTaskId() {
        return taskService.createTaskQuery().singleResult().getId();
    }


    private String verifyMyProcesses(int startedByRA, int startedBySpaclient) {
        String proceeeInstanceID = null;
        // Admin vede la Process Instance che ha avviato
        Map<String, String> searchParams = new HashMap<>();

        ResponseEntity<Map<String, Object>> response = flowsProcessInstanceResource.getMyProcessInstances(0, 10, ASC, true, ALL_PROCESS_INSTANCES, searchParams);
        assertEquals(OK, response.getStatusCode());

        ArrayList processInstances = (ArrayList) ((DataResponse)response.getBody()).getData();
        assertEquals(startedByRA, processInstances.size());
        if (processInstances.size() > 0)
            proceeeInstanceID = ((HistoricProcessInstanceResponse)processInstances.get(0)).getId() ;

        // User NON vede la Process Instance avviata da Admin
        util.loginUser();

        response = flowsProcessInstanceResource.getMyProcessInstances(0, 10, ASC, true, ALL_PROCESS_INSTANCES, searchParams);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedBySpaclient, ((ArrayList) ((DataResponse)response.getBody()).getData()).size());
        util.loginResponsabileAcquisti();
        return proceeeInstanceID;
    }
}