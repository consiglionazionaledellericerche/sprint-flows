package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.utils.Enum;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.TestServices.TITOLO_DELL_ISTANZA_DEL_FLUSSO;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.initiator;
import static it.cnr.si.flows.ng.utils.Utils.*;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class FlowsProcessInstanceResourceTest {

    private static final int LOAD_TEST_PROCESS_INSTANCES = 700;
    private static int processDeleted = 0;
    @Inject
    HistoryService historyService;
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

    private StopWatch stopWatch = new StopWatch();
    private ProcessInstanceResponse processInstance;


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
    public void testGetMyProcesses() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());
        String processInstanceID = verifyMyProcesses(1, 0);
        // testo che, anche se una Process Instance viene sospesa, la vedo ugualmente
        util.loginAdmin();
        flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstanceID);

        util.loginResponsabileAcquisti();
        processInstanceID = verifyMyProcesses(1, 0);
        //testo che sospendendo una Process Instances NON la vedo tra i processi avviati da me
        util.loginAdmin();
        MockHttpServletResponse response = new MockHttpServletResponse();
        flowsProcessInstanceResource.delete(response, processInstanceID, "TEST");
        assertEquals(NO_CONTENT.value(), response.getStatus());
        util.loginResponsabileAcquisti();
        verifyMyProcesses(0, 0);
    }


    @Test(expected = AccessDeniedException.class)
    public void testGetProcessInstanceById() throws Exception {
        processInstance = util.mySetUp(acquisti.getValue());

        ResponseEntity<Map<String, Object>> response = flowsProcessInstanceResource.getProcessInstanceById(new MockHttpServletRequest(), processInstance.getId());
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
        flowsProcessInstanceResource.getProcessInstanceById(new MockHttpServletRequest(), processInstance.getId());

        //verifica AccessDeniedException (risposta 403 Forbidden) in caso di accesso di utenti non autorizzati
        util.loginUser();
        flowsProcessInstanceResource.getProcessInstanceById(new MockHttpServletRequest(), processInstance.getId());
    }

    @Test
    public void testGetProcessInstances() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());

        //responsabileacquisti crea una seconda Process Instance di acquisti con suffisso "2" nel titoloIstanzaFlusso
        util.loginResponsabileAcquisti();
        String acquistiTrasparenzaId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(acquisti.getValue()).latestVersion().singleResult().getId();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("processDefinitionId", acquistiTrasparenzaId);
        req.setParameter("titoloIstanzaFlusso", TITOLO_DELL_ISTANZA_DEL_FLUSSO + "2");
        req.setParameter("descrizioneAcquisizione", "descrizione" + "2");
        req.setParameter("tipologiaAcquisizioneI", "procedura aperta");
        req.setParameter("tipologiaAcquisizioneId", "11");
        req.setParameter("strumentoAcquisizione", "AFFIDAMENTO DIRETTO - MEPA o CONSIP\n");
        req.setParameter("strumentoAcquisizioneId", "11");
        req.setParameter("priorita", "Alta");
        req.setParameter("rup", "spaclient");
        req.setParameter("impegni_json", "[{\"numero\":\"1\",\"importoNetto\":100,\"importoLordo\":120,\"descrizione\":\"descrizione impegno\",\"vocedispesa\":\"11001 - Arretrati per anni precedenti corrisposti al personale a tempo indeterminato\",\"vocedispesaid\":\"11001\",\"gae\":\"spaclient\"}]");

        ResponseEntity<Object> resp = flowsTaskResource.completeTask(req);
        assertEquals(OK, resp.getStatusCode());

        //Verifico che Admin veda entrambe le Process Instances create
        util.loginAdmin();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + Utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());

        ResponseEntity<DataResponse> ret = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        assertEquals(HttpStatus.OK, ret.getStatusCode());
        ArrayList<HistoricProcessInstanceResponse> entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();

        //vedo sia la Process Instance avviata da admin che quella avviata da responsabileacquisti
        assertEquals(2, entities.size());
        assertEquals(util.getProcessDefinition(), entities.get(0).getProcessDefinitionId());
        assertEquals(acquistiTrasparenzaId, entities.get(1).getProcessDefinitionId());

        //cancello un processo
        MockHttpServletResponse response = new MockHttpServletResponse();
        String activeId = entities.get(0).getId();
        String notActiveId = entities.get(1).getId();
        flowsProcessInstanceResource.delete(response, notActiveId, "test");
        assertEquals(response.getStatus(), NO_CONTENT.value());

        // verifico che RA veda UN processo terminato/cancellato in più (quello appena concellato + quelli cancellati nel tearDown dei test precedenti)
        util.loginResponsabileAcquisti();
        ret = flowsProcessInstanceResource.getProcessInstances(new MockHttpServletRequest(), false, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();
        assertEquals(processDeleted + 2, entities.size());

        // .. e 1 processo ancora attivo VERIFICANDO CHE GLI ID COINCIDANO
        ret = flowsProcessInstanceResource.getProcessInstances(new MockHttpServletRequest(), true, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();
        assertEquals(1, entities.size());
        assertEquals(activeId, entities.get(0).getId());

        //VERIFICO FUNZIONI LA RICERCA
        verifyBadSearchParams(request);
    }


    @Test
    public void testSuspend() throws Exception {
        processInstance = util.mySetUp(acquisti.getValue());
        assertEquals(false, processInstance.isSuspended());
        //solo admin può sospendere il flow
        util.loginAdmin();
        ProcessInstanceResponse response = flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstance.getId());
        assertEquals(true, response.isSuspended());
    }

    @Test
    public void testSearchProcessInstances() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());
        util.loginAdmin();
        MockHttpServletRequest req = new MockHttpServletRequest();

        String searchField1 = "strumentoAcquisizioneId";
        String searchValue1 = "11";

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date tomorrow = cal.getTime();
        cal.add(Calendar.DATE, -2);
        Date yesterday = cal.getTime();
        String payload = "{params: [{key: " + searchField1 + ", value: \"" + searchValue1 + "\", type: textEqual}, " +
                "{key: " + initiator.name() + ", value: \"" + TestServices.getRA() + "\", type: text}, " +
                "{key: \"startDateGreat\", value: \"" + Utils.formattaData(yesterday) + "\", type: \"date\"}," +
                "{key: \"startDateLess\", value: \"" + Utils.formattaData(tomorrow) + "\", type: \"date\"}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        //verifico la richiesta normale
        ResponseEntity<Object> response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 1000);
        verifySearchResponse(response, 1, 1, searchField1, searchValue1, initiator.name(), TestServices.getRA());

        //verifico la richiesta su tutte le Process Definition
        response = flowsProcessInstanceResource.search(req, "all", true, ASC, 0, 1000);
        verifySearchResponse(response, 1, 1, searchField1, searchValue1, initiator.name(), TestServices.getRA());

        //cerco che le Process Instance completate (active = false) siano quelle cancellate nel tearDown (processDeleted) + 2 (quelle rimosse in testGetProcessInstances ed in testGetMyProcesses)
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], false, DESC, 0, 1000);
        verifySearchResponse(response, processDeleted + 2, processDeleted + 2, searchField1, searchValue1, initiator.name(), TestServices.getRA());

        /*
         * VERIFICA GESTIONE DELLE AUTHORITIES
         */
        verifyAuthorities(1, req, searchField1, searchValue1, initiator.name(), 1);


        //parametri sbagliati (strumentoAcquisizioneId 12 invece di 11, initiator = admin invece dell'RA) ==> 0 risultati
        payload = "{params: [{key: " + searchField1 + ", value: \"12\", type: textEqual} , {key: initiator, value: \"admin\", type: text}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 1000);
        verifySearchResponse(response, 0, 0, searchField1, searchValue1, initiator.name(), TestServices.getRA());
    }


    @Test
    public void loadTestSearchProcessInstances() throws IOException {
        int maxResults = 25;
        // creo un certo numero di Process Instances
        for (int i = 0; i < LOAD_TEST_PROCESS_INSTANCES; i++) {
            try {
                util.mySetUp(acquisti.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        processInstance = util.mySetUp(acquisti.getValue());
        util.loginResponsabileAcquisti();
        MockHttpServletRequest req = new MockHttpServletRequest();

        String searchField1 = "strumentoAcquisizioneId";
        String searchValue1 = "11";

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date tomorrow = cal.getTime();
        cal.add(Calendar.DATE, -2);
        Date yesterday = cal.getTime();
        String payload = "{params: [{key: " + searchField1 + ", value: \"" + searchValue1 + "\", type: textEqual}, " +
                "{key: " + Enum.VariableEnum.initiator.name() + ", value: \"" + TestServices.getRA() + "\", type: text}, " +
                "{key: \"startDateGreat\", value: \"" + Utils.formattaData(yesterday) + "\", type: \"date\"}," +
                "{key: \"startDateLess\", value: \"" + Utils.formattaData(tomorrow) + "\", type: \"date\"}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        //verifico la richiesta normale
        stopWatch.start(String.format("verifico una richiesta normale ( %s istanze recuperate!)", LOAD_TEST_PROCESS_INSTANCES + 1));
        ResponseEntity<Object> response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, LOAD_TEST_PROCESS_INSTANCES + 1, maxResults, searchField1, searchValue1, initiator.name(), TestServices.getRA());

        //verifico la richiesta su tutte le Process Definition
        stopWatch.start(String.format("verifico la richiesta su tutte le Process Definition ( %s istanze recuperate!)", LOAD_TEST_PROCESS_INSTANCES + 1));
        response = flowsProcessInstanceResource.search(req, ALL_PROCESS_INSTANCES, true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, LOAD_TEST_PROCESS_INSTANCES + 1, maxResults, searchField1, searchValue1, initiator.name(), TestServices.getRA());

        //verifico la richiesta su tutte le Process Definition TERMINATE (0 risultati)
        stopWatch.start(String.format("verifico la richiesta su tutte le Process Definition per le process Instances TERMINATE( %s istanze recuperate!)", 0));
        response = flowsProcessInstanceResource.search(req, ALL_PROCESS_INSTANCES, false, ASC, 0, maxResults);
        stopWatch.stop();
        //recupero le pross instajnces cancellate nei tearDown + quella rimossa in testGetMyProcesses (eseguito prima)
        verifySearchResponse(response, processDeleted + 1, processDeleted + 1, searchField1, searchValue1, initiator.name(), TestServices.getRA());

        /*
         * VERIFICA GESTIONE DELLE AUTHORITIES
         */
        verifyAuthorities(maxResults, req, searchField1, searchValue1, initiator.name(), LOAD_TEST_PROCESS_INSTANCES + 1);

        //parametri sbagliati (strumentoAcquisizioneId 12 invece di 11, initiator = admin invece dell'RA) ==> 0 risultati
        payload = "{params: [{key: " + searchField1 + ", value: \"12\", type: textEqual} , {key: initiator, value: \"admin\", type: text}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");
        stopWatch.start(String.format("verifico una richiesta SBAGLIATA ( %s istanze recuperate!)", 0));
        response = flowsProcessInstanceResource.search(req, "all", true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, 0, 0, searchField1, searchValue1, initiator.name(), TestServices.getRA());
    }


    private void verifyAuthorities(int maxResults, MockHttpServletRequest req, String searchField1, String searchValue1, String searchField2, int expectedTotalItems) {
        ResponseEntity<Object> response;

        //Uno User normale ed il direttore, a questo punto dei flussi non devono poterli vedere
        util.loginUser();
        stopWatch.start(String.format("verifico una richiesta di USER ( %s istanze recuperate!)", 0));
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, 0, 0, searchField1, searchValue1, searchField2, TestServices.getRA());
        util.loginDirettore();
        stopWatch.start(String.format("verifico una richiesta di DIRETTORE ( %s istanze recuperate!)", 0));
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, 0, 0, searchField1, searchValue1, searchField2, TestServices.getRA());

        //l'sfd ed ENTRAMBI i responsabili acquisti devono vedere TUTTI i flussi avviati
        util.loginSfd();
        stopWatch.start(String.format("verifico una richiesta di SFD ( %s istanze recuperate!)", maxResults));
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, expectedTotalItems, maxResults, searchField1, searchValue1, searchField2, TestServices.getRA());
        util.loginResponsabileAcquisti();
        stopWatch.start(String.format("verifico una richiesta di RA ( %s istanze recuperate!)", maxResults));
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, expectedTotalItems, maxResults, searchField1, searchValue1, searchField2, TestServices.getRA());
        util.loginResponsabileAcquisti2();
        stopWatch.start(String.format("verifico una richiesta di RA2 ( %s istanze recuperate!)", maxResults));
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
        stopWatch.stop();
        verifySearchResponse(response, expectedTotalItems, maxResults, searchField1, searchValue1, searchField2, TestServices.getRA());
    }

    @Test
    public void testExportCsv() throws IOException {
        //avvio un flusso acquisti
        processInstance = util.mySetUp(acquisti.getValue());
        //faccio l'exportCsv su tutti le Process Instance attive
        MockHttpServletRequest req = new MockHttpServletRequest();
        String payload = "{params: [{key: initiator, value: \"\", type: text}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");
        MockHttpServletResponse responseAll = new MockHttpServletResponse();
        flowsProcessInstanceResource.exportCsv(req, responseAll, ALL_PROCESS_INSTANCES, true, ASC, 0, 1000);
        assertEquals(OK.value(), responseAll.getStatus());

        //faccio l'exportCsv su UNA SOLA Process Instance attiva
        MockHttpServletResponse responseOne = new MockHttpServletResponse();
        flowsProcessInstanceResource.exportCsv(req, responseOne, ALL_PROCESS_INSTANCES, true, ASC, 0, 2);
        assertEquals(OK.value(), responseOne.getStatus());
        assertEquals(responseAll.getContentAsString(), responseOne.getContentAsString());

        //verifico che exportCsv dei flussi NON ATTIVI è vuoto
        MockHttpServletResponse terminatedProcessInstances = new MockHttpServletResponse();
        flowsProcessInstanceResource.exportCsv(req, terminatedProcessInstances, ALL_PROCESS_INSTANCES, false, ASC, 0, 1000);
        assertEquals(OK.value(), terminatedProcessInstances.getStatus());
//        verifico che le righe del csv siano quanti i flussi "terminati" + 1 (intestazione del file csv)
        assertEquals(terminatedProcessInstances.getContentAsString().split("\n").length,
                     historyService.createHistoricProcessInstanceQuery().finished().list().size() + 1);
    }


    private void verifySearchResponse(ResponseEntity<Object> response, int expectedTotalItems, int expectedResonseItems, String searchField1, String searchValue1, String searchField2, String searchValue2) {
        assertEquals(OK, response.getStatusCode());
        HashMap body = (HashMap) response.getBody();
        ArrayList responseList = (ArrayList) body.get("processInstances");
        assertEquals("Lunghezza della lista NON corrispondente alle attese", expectedResonseItems, responseList.size());
        assertEquals("TotalItems NON corrispondente alle attese", expectedTotalItems, ((Integer) body.get("totalItems")).intValue());

        if (responseList.size() > 0) {
            HistoricProcessInstanceResponse taskresponse = ((HistoricProcessInstanceResponse) responseList.get(0));
            assertTrue(taskresponse.getProcessDefinitionId().contains(util.getProcessDefinition()));
            //verifico che la Process Instance restituita rispetti i parametri della ricerca
            List<RestVariable> variables = taskresponse.getVariables();
            RestVariable variable = variables.stream().filter(v -> v.getName().equals(searchField1)).collect(Collectors.toList()).get(0);
            assertEquals(searchValue1, variable.getValue());

            variable = variables.stream().filter(v -> v.getName().equals(searchField2)).collect(Collectors.toList()).get(0);
            assertEquals(searchValue2, variable.getValue());
        }
    }


    private String verifyMyProcesses(int startedByRA, int startedBySpaclient) {
        String proceeeInstanceID = null;
        // Admin vede la Process Instance che ha avviato
        ResponseEntity<DataResponse> response = flowsProcessInstanceResource.getMyProcessInstances(true, ALL_PROCESS_INSTANCES, ASC, 0, 100);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedByRA, response.getBody().getSize());
        List<HistoricProcessInstanceResponse> processInstances = ((List<HistoricProcessInstanceResponse>) response.getBody().getData());
        assertEquals(startedByRA, processInstances.size());
        if (processInstances.size() > 0)
            proceeeInstanceID = processInstances.get(0).getId();

        // User NON vede la Process Instance avviata da Admin
        util.loginUser();
        response = flowsProcessInstanceResource.getMyProcessInstances(true, "all", ASC, 0, 100);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedBySpaclient, response.getBody().getSize());
        assertEquals(startedBySpaclient, ((List<HistoricProcessInstanceResponse>) response.getBody().getData()).size());
        util.loginResponsabileAcquisti();
        return proceeeInstanceID;
    }

    //verifico che non prenda nessun elemento (SEARCH PARAMS SBAGLIATI)
    private void verifyBadSearchParams(MockHttpServletRequest request) {
        ResponseEntity<DataResponse> response;

        //prendo solo quello avviato da RA
        String content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + Utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        ArrayList<HistoricProcessInstanceResponse> entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(1, entities.size());
        assertEquals(1, ((ArrayList) response.getBody().getData()).size());

        //titolo flusso sbagliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "AAAAAAAAA" + "\",\"type\":\"text\"}," +
                "{\"key\":\"" + Enum.VariableEnum.initiator.name() + "\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + Utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());

        //initiator sbaliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"" + Enum.VariableEnum.initiator.name() + "\",\"value\":\"" + TestServices.getRA() + "AAA" + "\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + Utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());

        //STARTDATE sbaliata
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"" + Enum.VariableEnum.initiator.name() + "\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + new DateTime().plusDays(1).toString("yyyy-MM-dd") + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
    }
}