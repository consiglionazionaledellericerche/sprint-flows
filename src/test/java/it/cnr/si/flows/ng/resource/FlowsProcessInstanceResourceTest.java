package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestUtil;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.TestUtil.TITOLO_DELL_ISTANZA_DEL_FLUSSO;
import static it.cnr.si.flows.ng.utils.Utils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class FlowsProcessInstanceResourceTest {

    private static int processDeleted = 0;
    @Autowired
    private FlowsTaskResource flowsTaskResource;
    @Autowired
    private FlowsProcessInstanceResource flowsProcessInstanceResource;
    @Autowired
    private TestUtil util;
    @Autowired
    private FlowsProcessDefinitionResource flowsProcessDefinitionResource;
    private ProcessInstanceResponse processInstance;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");


    @After
    public void tearDown() {
        util.myTearDown();
        processDeleted++;
    }

    @Test
    public void testGetMyProcesses() {
        processInstance = util.mySetUp("missioni");
        String processInstanceID = verifyMyProcesses(1, 0);
        // testo che, anche se una Process Instance viene sospesa, la vedo ugualmente
        flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstanceID);
        processInstanceID = verifyMyProcesses(1, 0);
        //testo che eliminando una Process Instances NON la vedo tra i processi avviati da me
        flowsProcessInstanceResource.delete(new MockHttpServletResponse(), processInstanceID, "TEST");
        verifyMyProcesses(0, 0);
    }


    @Test
    @Ignore
    public void testGetMyTasks() {
        //TODO: Test goes here...
    }

    @Test
    @Ignore
    public void testGetAvailableTasks() {
        //TODO: Test goes here...
    }

    @Test
    @Ignore
    public void testGetWorkflowInstances() throws Exception {
        //TODO: Test goes here...
    }

    @Test
    @Ignore
    public void testGetProcessInstanceById() throws Exception {
        //TODO: Test goes here...
    }

    @Test
    public void testGetProcessInstances() {
        processInstance = util.mySetUp("acquisti-trasparenza");
        //Recupero la Process Definition per permessi ferie
        util.loginUser();
        DataResponse appo = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();
        String acquistiTrasparenzaId = ((ArrayList<ProcessDefinitionResponse>) appo.getData()).stream()
                .filter(h -> h.getKey().contains("acquisti-trasparenza"))
                .collect(Collectors.toList()).get(0).getId();
        //User crea una seconda Process Instance di acquisti-trasparenza con suffisso "2"
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("processDefinitionId", acquistiTrasparenzaId);
        req.setParameter("titoloIstanzaFlusso", TITOLO_DELL_ISTANZA_DEL_FLUSSO + "2");
        req.setParameter("descrizioneAcquisizione", "descrizione" + "2");
        req.setParameter("tipologiaAcquisizioneI", "procedura aperta");
        req.setParameter("tipologiaAcquisizioneId", "11");
        req.setParameter("strumentoAcquisizione", "AFFIDAMENTO DIRETTO - MEPA o CONSIP\n");
        req.setParameter("strumentoAcquisizioneId", "11");
        req.setParameter("priorita", "Alta");
        flowsTaskResource.completeTask(req);
        util.logout();

        //Verifico che Admin veda entrambe le Process Instances create
        util.loginAdmin();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + sdf.format(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());

        ResponseEntity<DataResponse> ret = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        assertEquals(HttpStatus.OK, ret.getStatusCode());
        ArrayList<HistoricProcessInstanceResponse> entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();
        //vedo sia la Process Instance avviata da admin che quella avviata da User
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
    @Ignore
    public void testGetWorkflowVariables() throws Exception {
        //TODO: Test goes here...
    }

    @Test
    public void testSuspend() throws Exception {
        processInstance = util.mySetUp("missioni");
        assertEquals(false, processInstance.isSuspended());
        ProcessInstanceResponse response = flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstance.getId());
        assertEquals(true, response.isSuspended());
    }

    @Test
    public void testSearchProcessInstances() throws ParseException {
        processInstance = util.mySetUp("missioni");
        util.loginAdmin();
        MockHttpServletRequest req = new MockHttpServletRequest();

        String searchField1 = "wfvarValidazioneSpesa";
        String searchField2 = "initiator";
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date tomorrow = cal.getTime();
        cal.add(Calendar.DATE, -2);
        Date yesterday = cal.getTime();
        String payload = "{params: [{key: " + searchField1 + ", value: true, type: boolean}, " +
                "{key: " + searchField2 + ", value: \"admin\", type: textEqual}, " +
                "{key: \"startDateGreat\", value: \"" + sdf.format(yesterday) + "\", type: \"date\"}," +
                "{key: \"startDateLess\", value: \"" + sdf.format(tomorrow) + "\", type: \"date\"}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        //verifico la richiesta normale
        ResponseEntity<Object> response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 10);
        verifyResponse(response, 1, searchField1, searchField2);

        //verifico la richiesta su tutte le Process Definition
        response = flowsProcessInstanceResource.search(req, "all", true, ASC, 0, 10);
        verifyResponse(response, 1, searchField1, searchField2);

        //cerco le Process Instance completate (active = false  ==>  #(processDeleted) risultati)
        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], false, DESC, 0, 10);
        verifyResponse(response, processDeleted, searchField1, searchField2);

        //parametri sbagliati (0 risultati)
        payload = "{params: [{key: " + searchField1 + ", value: false, type: boolean} , {key: initiator, value: \"admin\", type: textEqual}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 10);
        verifyResponse(response, 0, searchField1, searchField2);
    }

    private void verifyResponse(ResponseEntity<Object> response, int expectedTotalItems, String searchField1, String searchField2) {
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
            assertEquals(true, variable.getValue());

            variable = variables.stream().filter(v -> v.getName().equals(searchField2)).collect(Collectors.toList()).get(0);
            assertEquals("admin", variable.getValue());
        }
    }


    private String verifyMyProcesses(int startedByAdmin, int startedBySpaclient) {
        String proceeeInstanceID = null;
        // Admin vede la Process Instance che ha avviato
        ResponseEntity<DataResponse> response = flowsProcessInstanceResource.getMyProcessInstances(true, "all", ASC, 0, 100);
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
                "{\"key\":\"startDateGreat\",\"value\":\"" + sdf.format(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        ArrayList<HistoricProcessInstanceResponse> entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(1, entities.size());
        assertEquals(1, ((ArrayList) response.getBody().getData()).size());
        //titolo flusso sbagliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "AAAAAAAAA" + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"admin\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + sdf.format(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        //initiator sbaliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"admi\",\"type\":\"textEqual\"}," +
                "{\"key\":\"startDateGreat\",\"value\":\"" + sdf.format(new Date()) + "\",\"type\":\"date\"}]}";
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
