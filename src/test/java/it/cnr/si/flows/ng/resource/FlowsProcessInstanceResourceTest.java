package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.HistoryService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.TestServices.TITOLO_DELL_ISTANZA_DEL_FLUSSO;
import static it.cnr.si.flows.ng.utils.Utils.*;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class FlowsProcessInstanceResourceTest {

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
    private ProcessInstanceResponse processInstance;


    @Before
    public void setUp() {
        HttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @After
    public void tearDown() {
        util.myTearDown();
        processDeleted++;
    }

    @Test
    public void testGetMyProcesses() throws IOException {
        processInstance = util.mySetUp("acquisti-trasparenza");
        String processInstanceID = verifyMyProcesses(1, 0);
        // testo che, anche se una Process Instance viene sospesa, la vedo ugualmente
        flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstanceID);
        processInstanceID = verifyMyProcesses(1, 0);
        //testo che eliminando una Process Instances NON la vedo tra i processi avviati da me
        MockHttpServletResponse response = new MockHttpServletResponse();
        flowsProcessInstanceResource.delete(response, processInstanceID, "TEST");
        assertEquals(NO_CONTENT.value(), response.getStatus());
        verifyMyProcesses(0, 0);
    }


    @Test
    public void testGetProcessInstanceById() throws Exception {
        processInstance = util.mySetUp("acquisti-trasparenza");

        ResponseEntity<Map<String, Object>> response = flowsProcessInstanceResource.getProcessInstanceById(processInstance.getId());
        assertEquals(OK, response.getStatusCode());

        HistoricProcessInstanceResponse entity = (HistoricProcessInstanceResponse) ((HashMap) response.getBody()).get("entity");
        assertEquals(processInstance.getId(), entity.getId());
        assertEquals(processInstance.getBusinessKey(), entity.getBusinessKey());
        assertEquals(processInstance.getProcessDefinitionId(), entity.getProcessDefinitionId());

        HashMap appo = (HashMap) ((HashMap) response.getBody()).get("identityLinks");
        assertEquals(1, appo.size());
        HashMap identityLinks = (HashMap) appo.get(appo.keySet().toArray()[0]);
        assertEquals("[${gruppoSFD}]", identityLinks.get("candidateGroups").toString());
        assertEquals(((HashSet) identityLinks.get("candidateUsers")).size(), 0);
        assertEquals(((ArrayList) identityLinks.get("links")).size(), 1);
        assertNull(identityLinks.get("assignee"));

        HashMap history = (HashMap) ((ArrayList) ((HashMap) response.getBody()).get("history")).get(0);
        assertEquals(processInstance.getId(), ((HistoricTaskInstanceResponse) history.get("historyTask")).getProcessInstanceId());
        assertEquals(1, ((ArrayList) history.get("historyIdentityLink")).size());

        HashMap attachments = (HashMap) ((HashMap) response.getBody()).get("attachments");
        assertEquals(0, attachments.size());
    }

    @Test
    public void testGetProcessInstances() throws IOException {
        processInstance = util.mySetUp("acquisti-trasparenza");
        //Recupero la Process Definition per acquisti-trasparenza
        util.loginUser();
        DataResponse appo = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();
        String acquistiTrasparenzaId = ((ArrayList<ProcessDefinitionResponse>) appo.getData()).stream()
                .filter(h -> h.getKey().contains("acquisti-trasparenza"))
                .collect(Collectors.toList()).get(0).getId();
        //responsabileacquisti crea una seconda Process Instance di acquisti-trasparenza con suffisso "2" nel titoloIstanzaFlusso
        util.logout();
        util.loginResponsabileAcquisti();
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
        req.setParameter("impegni_json", "[{\"numero\":\"1\",\"importo\":100,\"descrizione\":\"descrizione impegno\",\"vocedispesa\":\"11001 - Arretrati per anni precedenti corrisposti al personale a tempo indeterminato\",\"vocedispesaid\":\"11001\",\"gae\":\"spaclient\"}]");

        ResponseEntity<Object> resp = flowsTaskResource.completeTask(req);
        assertEquals(OK, resp.getStatusCode());
        util.logout();

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
        // verifico che Admin veda UN processo terminato/cancellato in più (quello appena concellato + quelli cancellati nel tearDown dei test precedenti)
        ret = flowsProcessInstanceResource.getProcessInstances(new MockHttpServletRequest(), false, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();
        assertEquals(entities.size(), processDeleted + 1);
        // .. e 1 processo ancora attivo VERIFICANDO CHE GLI ID COINCIDANO
        ret = flowsProcessInstanceResource.getProcessInstances(new MockHttpServletRequest(), true, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();
        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getId(), activeId);

        //VERIFICO FUNZIONI LA RICERCA
        verifyBadSearchParams(request);
    }


    @Test
    public void testSuspend() throws Exception {
        processInstance = util.mySetUp("acquisti-trasparenza");
        assertEquals(false, processInstance.isSuspended());
        ProcessInstanceResponse response = flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstance.getId());
        assertEquals(true, response.isSuspended());
    }

    @Test
    public void testSearchProcessInstances() throws ParseException, IOException {
        processInstance = util.mySetUp("acquisti-trasparenza");
        util.loginAdmin();
        MockHttpServletRequest req = new MockHttpServletRequest();

        String searchField1 = "strumentoAcquisizioneId";
        String searchValue1 = "11";
        String searchField2 = "initiator";
        String searchValue2 = "admin";
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date tomorrow = cal.getTime();
        cal.add(Calendar.DATE, -2);
        Date yesterday = cal.getTime();
        String payload = "{params: [{key: " + searchField1 + ", value: \"" + searchValue1 + "\", type: textEqual}, " +
                "{key: " + searchField2 + ", value: \"" + searchValue2 + "\", type: text}, " +
                "{key: \"startDateGreat\", value: \"" + Utils.formattaData(yesterday) + "\", type: \"date\"}," +
                "{key: \"startDateLess\", value: \"" + Utils.formattaData(tomorrow) + "\", type: \"date\"}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        //verifico la richiesta normale
        ResponseEntity<Object> response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 10);
        verifySearchResponse(response, 1, searchField1, searchValue1, searchField2, searchValue2);

        //verifico la richiesta su tutte le Process Definition
        response = flowsProcessInstanceResource.search(req, "all", true, ASC, 0, 10);
        verifySearchResponse(response, 1, searchField1, searchValue1, searchField2, searchValue2);

        //cerco le Process Instance completate (active = false  ==>  #(processDeleted) risultati)
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], false, DESC, 0, 10);
        verifySearchResponse(response, processDeleted, searchField1, searchValue1, searchField2, searchValue2);

        //parametri sbagliati (strumentoAcquisizioneId 12 invece di 11) ==> 0 risultati
        payload = "{params: [{key: " + searchField1 + ", value: \"12\", type: textEqual} , {key: initiator, value: \"admin\", type: text}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 10);
        verifySearchResponse(response, 0, searchField1, searchValue1, searchField2, searchValue2);
    }

    @Test
    public void testExportCsv() throws IOException {
        //avvio un flusso acquisti-trasparenza
        processInstance = util.mySetUp("acquisti-trasparenza");
        //faccio l'exportCsv su tutti le Process Instance attive
        MockHttpServletRequest req = new MockHttpServletRequest();
        String payload = "{params: [{key: initiator, value: \"\", type: text}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");
        MockHttpServletResponse responseAll = new MockHttpServletResponse();
        flowsProcessInstanceResource.exportCsv(req, responseAll, ALL_PROCESS_INSTANCES, true, ASC, -1, -1);
        assertEquals(OK.value(), responseAll.getStatus());

        //faccio l'exportCsv su UNA SOLA Process Instance attiva
        MockHttpServletResponse responseOne = new MockHttpServletResponse();
        flowsProcessInstanceResource.exportCsv(req, responseOne, ALL_PROCESS_INSTANCES, true, ASC, 0, 2);
        assertEquals(OK.value(), responseOne.getStatus());
        assertEquals(responseAll.getContentAsString(), responseOne.getContentAsString());

        //verifico che exportCsv dei flussi NON ATTIVI è vuoto
        MockHttpServletResponse terminatedProcessInstances = new MockHttpServletResponse();
        flowsProcessInstanceResource.exportCsv(req, terminatedProcessInstances, ALL_PROCESS_INSTANCES, false, ASC, -1, -1);
        assertEquals(OK.value(), terminatedProcessInstances.getStatus());
//        verifico che le righe dell csv siano quanti i flussi "terminati" + 1 (intestazione del file csv)
        assertEquals(terminatedProcessInstances.getContentAsString().split("\n").length,
                     historyService.createHistoricProcessInstanceQuery().finished().list().size() + 1);
    }





    private void verifySearchResponse(ResponseEntity<Object> response, int expectedTotalItems, String searchField1, String searchValue1, String searchField2, String searchValue2) {
        assertEquals(OK, response.getStatusCode());
        HashMap body = (HashMap) response.getBody();
        ArrayList responseList = (ArrayList) body.get("processInstances");
        assertEquals(responseList.size(), ((Long) body.get("totalItems")).intValue());
        assertEquals(expectedTotalItems, ((Long) body.get("totalItems")).intValue());

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


    private String verifyMyProcesses(int startedByAdmin, int startedBySpaclient) {
        String proceeeInstanceID = null;
        // Admin vede la Process Instance che ha avviato
        ResponseEntity<DataResponse> response = flowsProcessInstanceResource.getMyProcessInstances(true, ALL_PROCESS_INSTANCES, ASC, 0, 100);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedByAdmin, response.getBody().getSize());
        List<HistoricProcessInstanceResponse> processInstances = ((List<HistoricProcessInstanceResponse>) response.getBody().getData());
        assertEquals(startedByAdmin, processInstances.size());
        if (processInstances.size() > 0)
            proceeeInstanceID = processInstances.get(0).getId();
        util.logout();

        // User NON vede la Process Instance avviata da Admin
        util.loginUser();
        response = flowsProcessInstanceResource.getMyProcessInstances(true, "all", ASC, 0, 100);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedBySpaclient, response.getBody().getSize());
        assertEquals(startedBySpaclient, ((List<HistoricProcessInstanceResponse>) response.getBody().getData()).size());
        util.logout();
        util.loginAdmin();
        return proceeeInstanceID;
    }

    //verifico che non prenda nessun elemento (SEARCH PARAMS SBAGLIATI)
    private void verifyBadSearchParams(MockHttpServletRequest request) {
        ResponseEntity<DataResponse> response;
        //prendo solo quello avviato da ADMIN
        String content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"admin\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + Utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        ArrayList<HistoricProcessInstanceResponse> entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(1, entities.size());
        assertEquals(1, ((ArrayList) response.getBody().getData()).size());
        //titolo flusso sbagliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "AAAAAAAAA" + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"admin\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + Utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        //initiator sbaliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"admi\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + Utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        //STARTDATE sbaliata
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"admin\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + new DateTime().plusDays(1).toString("yyyy-MM-dd") + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
    }
}
