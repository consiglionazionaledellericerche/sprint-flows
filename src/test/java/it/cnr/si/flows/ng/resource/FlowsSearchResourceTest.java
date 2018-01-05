package it.cnr.si.flows.ng.resource;

import static it.cnr.si.flows.ng.TestServices.TITOLO_DELL_ISTANZA_DEL_FLUSSO;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.initiator;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.oggetto;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.startDate;
import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static it.cnr.si.flows.ng.utils.Utils.DESC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import static it.cnr.si.flows.ng.utils.Utils.*;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class FlowsSearchResourceTest {

    @Inject
    private TestServices util;
    @Inject
    private FlowsProcessInstanceResource flowsProcessInstanceResource;
    @Inject 
    private FlowsTaskResource flowsTaskResource;
    @Inject
    private FlowsSearchResource flowsSearchResource;
    
    private ProcessInstanceResponse processInstance;
    private static int processDeleted = 0;
    
    @Test
    public void testProcessInstanceSearch() throws IOException, JSONException {
        processInstance = util.mySetUp(acquisti.getValue());
        util.loginAdmin();

        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date tomorrow = cal.getTime();
        cal.add(Calendar.DATE, -2);
        Date yesterday = cal.getTime();
        
        Map<String, String> requestParams = new HashMap<>();
        
        requestParams.put("oggetto", "textEqual="+ TITOLO_DELL_ISTANZA_DEL_FLUSSO);
        requestParams.put(initiator.name(), "text="+ TestServices.getRA());
        requestParams.put(startDate + "Great", "date="+ formattaData(yesterday));
        requestParams.put(startDate + "Less", "date="+ formattaData(tomorrow));
        requestParams.put("processDefinitionKey", util.getProcessDefinition().split(":")[0]);
        requestParams.put("order", ASC);
        requestParams.put("active", "true");
        requestParams.put("firstResult", "0");
        requestParams.put("maxResults", "20");

        //verifico la richiesta normale
        ResponseEntity<Object> response = flowsSearchResource.search(requestParams);
        verifySearchResponse(response, 1, 1);

        //verifico la richiesta su tutte le Process Definition
        response = flowsSearchResource.search(requestParams);
        verifySearchResponse(response, 1, 1);

        //cerco che le Process Instance completate (active = false) siano quelle cancellate nel tearDown (processDeleted) + 2 (quella cancellata in testGetMyProcesses (NON quella creata con oggetto diverso in testGetProcessInstances) )
        requestParams.put("active", "false");
        requestParams.put("order", DESC);
        response = flowsSearchResource.search(requestParams);
        verifySearchResponse(response, processDeleted + 1, processDeleted + 1);

        /*
         * VERIFICA GESTIONE DELLE AUTHORITIES TODO
         */
//        verifyAuthorities(1, req, oggetto.name(), TITOLO_DELL_ISTANZA_DEL_FLUSSO, initiator.name(), 1);


        //parametri sbagliati (strumentoAcquisizioneId 12 invece di 11, initiator = admin invece dell'RA) ==> 0 risultati
        requestParams.put("oggetto", "textEqual=12");
        requestParams.put("initiator", "text=admin");
        requestParams.put("order", ASC);
        requestParams.put("active", "true");
        requestParams.put("firstResult", "0");
        requestParams.put("maxResults", "20");
        
        response = flowsSearchResource.search(requestParams);
        verifySearchResponse(response, 0, 0);
    }


//    @Test
//    public void loadTestSearchProcessInstances() throws IOException, JSONException {
//        int maxResults = 25;
//        // creo un certo numero di Process Instances
//        for (int i = 0; i < LOAD_TEST_PROCESS_INSTANCES; i++) {
//            try {
//                util.mySetUp(acquisti.getValue());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        processInstance = util.mySetUp(acquisti.getValue());
//        util.loginResponsabileAcquisti();
//        MockHttpServletRequest req = new MockHttpServletRequest();
//
//        String searchField1 = "strumentoAcquisizioneId";
//        String searchValue1 = "11";
//
//        final Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DATE, +1);
//        Date tomorrow = cal.getTime();
//        cal.add(Calendar.DATE, -2);
//        Date yesterday = cal.getTime();
//        String payload = "{processParams: [{key: " + searchField1 + ", value: \"" + searchValue1 + "\", type: textEqual}, " +
//                "{key: " + Enum.VariableEnum.initiator.name() + ", value: \"" + TestServices.getRA() + "\", type: text}, " +
//                "{key: " + startDate + "Great, value: \"" + utils.formattaData(yesterday) + "\", type: \"date\"}," +
//                "{key: " + startDate + "Less, value: \"" + utils.formattaData(tomorrow) + "\", type: \"date\"}]}";
//        req.setContent(payload.getBytes());
//        req.setContentType("application/json");
//
//        //verifico la richiesta normale
//        stopWatch.start(String.format("verifico una richiesta normale ( %s istanze recuperate!)", LOAD_TEST_PROCESS_INSTANCES + 1));
//        ResponseEntity<Object> response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, LOAD_TEST_PROCESS_INSTANCES + 1, maxResults);
//
//        //verifico la richiesta su tutte le Process Definition
//        stopWatch.start(String.format("verifico la richiesta su tutte le Process Definition ( %s istanze recuperate!)", LOAD_TEST_PROCESS_INSTANCES + 1));
//        response = flowsProcessInstanceResource.search(req, ALL_PROCESS_INSTANCES, true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, LOAD_TEST_PROCESS_INSTANCES + 1, maxResults);
//
//        //verifico la richiesta su tutte le Process Definition TERMINATE (0 risultati)
//        stopWatch.start(String.format("verifico la richiesta su tutte le Process Definition per le process Instances TERMINATE( %s istanze recuperate!)", 0));
//        response = flowsProcessInstanceResource.search(req, ALL_PROCESS_INSTANCES, false, ASC, 0, maxResults);
//        stopWatch.stop();
//        //recupero le pross instances cancellate nei tearDown + quella rimossa in testGetMyProcesses (eseguito prima)
//        verifySearchResponse(response, processDeleted + 1, processDeleted + 1);
//
//        /*
//         * VERIFICA GESTIONE DELLE AUTHORITIES
//         */
//        verifyAuthorities(maxResults, req, searchField1, searchValue1, initiator.name(), LOAD_TEST_PROCESS_INSTANCES + 1);
//
//        //parametri sbagliati (strumentoAcquisizioneId 12 invece di 11, initiator = admin invece dell'RA) ==> 0 risultati
//        payload = "{processParams: [{key: " + searchField1 + ", value: \"12\", type: textEqual} , {key: initiator, value: \"admin\", type: text}]}";
//        req.setContent(payload.getBytes());
//        req.setContentType("application/json");
//        stopWatch.start(String.format("verifico una richiesta SBAGLIATA ( %s istanze recuperate!)", 0));
//        response = flowsProcessInstanceResource.search(req, "all", true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, 0, 0);
//    }


//    private void verifyAuthorities(int maxResults, MockHttpServletRequest req, String searchField1, String searchValue1, String searchField2, int expectedTotalItems) throws JSONException {
//        ResponseEntity<Object> response;
//
//        //Uno User normale ed il direttore, a questo punto dei flussi non devono poterli vedere
//        util.loginUser();
//        stopWatch.start(String.format("verifico una richiesta di USER ( %s istanze recuperate!)", 0));
//        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, 0, 0);
//        util.loginDirettore();
//        stopWatch.start(String.format("verifico una richiesta di DIRETTORE ( %s istanze recuperate!)", 0));
//        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, 0, 0);
//
//        //l'sfd ed ENTRAMBI i responsabili acquisti devono vedere TUTTI i flussi avviati
//        util.loginSfd();
//        stopWatch.start(String.format("verifico una richiesta di SFD ( %s istanze recuperate!)", maxResults));
//        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, expectedTotalItems, maxResults);
//        util.loginResponsabileAcquisti();
//        stopWatch.start(String.format("verifico una richiesta di RA ( %s istanze recuperate!)", maxResults));
//        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, expectedTotalItems, maxResults);
//        util.loginResponsabileAcquisti2();
//        stopWatch.start(String.format("verifico una richiesta di RA2 ( %s istanze recuperate!)", maxResults));
//        response = flowsProcessInstanceResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, maxResults);
//        stopWatch.stop();
//        verifySearchResponse(response, expectedTotalItems, maxResults);
//    }

//    @Test
//    public void testExportCsv() throws IOException {
//        //avvio un flusso acquisti
//        processInstance = util.mySetUp(acquisti.getValue());
//        //faccio l'exportCsv su tutti le Process Instance attive
//        MockHttpServletRequest req = new MockHttpServletRequest();
//        String payload = "{processParams: [{key: initiator, value: \"\", type: text}]}";
//        req.setContent(payload.getBytes());
//        req.setContentType("application/json");
//        MockHttpServletResponse responseAll = new MockHttpServletResponse();
//        flowsProcessInstanceResource.exportCsv(req, responseAll, ALL_PROCESS_INSTANCES, true, ASC, 0, 1000);
//        assertEquals(OK.value(), responseAll.getStatus());
//
//        //faccio l'exportCsv su UNA SOLA Process Instance attiva
//        MockHttpServletResponse responseOne = new MockHttpServletResponse();
//        flowsProcessInstanceResource.exportCsv(req, responseOne, ALL_PROCESS_INSTANCES, true, ASC, 0, 2);
//        assertEquals(OK.value(), responseOne.getStatus());
//        assertEquals(responseAll.getContentAsString(), responseOne.getContentAsString());
//
//        //verifico che exportCsv dei flussi NON ATTIVI è vuoto
//        MockHttpServletResponse terminatedProcessInstances = new MockHttpServletResponse();
//        flowsProcessInstanceResource.exportCsv(req, terminatedProcessInstances, ALL_PROCESS_INSTANCES, false, ASC, 0, 1000);
//        assertEquals(OK.value(), terminatedProcessInstances.getStatus());
////        verifico che le righe del csv siano quanti i flussi "terminati" + 1 (intestazione del file csv)
//        assertEquals(terminatedProcessInstances.getContentAsString().split("\n").length,
//                     historyService.createHistoricProcessInstanceQuery().finished().list().size() + 1);
//    }


    private void verifySearchResponse(ResponseEntity<Object> response, int expectedTotalItems, int expectedResponseItems) throws JSONException {
        assertEquals(OK, response.getStatusCode());
        HashMap body = (HashMap) response.getBody();
        ArrayList responseList = (ArrayList) body.get("processInstances");
        assertEquals("Lunghezza della lista NON corrispondente alle attese", expectedResponseItems, responseList.size());
        assertEquals("TotalItems NON corrispondente alle attese", expectedTotalItems, ((Integer) body.get("totalItems")).intValue());

        if (responseList.size() > 0) {
            for (int i = 0; i < (expectedTotalItems > expectedResponseItems ? expectedResponseItems : expectedTotalItems); i++) {
                HistoricProcessInstanceResponse taskresponse = ((HistoricProcessInstanceResponse) responseList.get(i));
                assertTrue(taskresponse.getProcessDefinitionId().contains(util.getProcessDefinition()));
                //verifico che le Process Instance restituite dalla search rispettino i parametri della ricerca
                org.json.JSONObject json = new org.json.JSONObject(taskresponse.getName());
                assertEquals(TITOLO_DELL_ISTANZA_DEL_FLUSSO, json.getString(oggetto.name()));
                assertEquals(TestServices.getRA(), json.getString(initiator.name()));
            }
        }
    }

    //verifico che non prenda nessun elemento (SEARCH PARAMS SBAGLIATI)
    private void verifyBadSearchParams(MockHttpServletRequest request) {
        ResponseEntity<DataResponse> response;

        //prendo solo quello avviato da RA
        String content = "{\"processParams\":" +
                "[{\"key\":\"" + oggetto + "\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}," +
                "{\"key\":\"" + startDate + "Great\",\"value\":\"" + formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        ArrayList<HistoricProcessInstanceResponse> entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(1, entities.size());
        assertEquals(1, ((ArrayList) response.getBody().getData()).size());

        //titolo flusso sbagliato
        content = "{\"processParams\":" +
                "[{\"key\":" + oggetto + ",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "AAAAAAAAA" + "\",\"type\":\"text\"}," +
                "{\"key\":" + initiator + ",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}," +
                "{\"key\":" + startDate + "Great,\"value\":\"" + formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());

        //initiator sbaliato
        content = "{\"processParams\":" +
                "[{\"key\":" + oggetto + ",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":" + initiator + ",\"value\":\"" + TestServices.getRA() + "AAA" + "\",\"type\":\"textEqual\"}," +
                "{\"key\":" + startDate + "Great,\"value\":\"" + formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());

        //STARTDATE sbaliata
        content = "{\"processParams\":" +
                "[{\"key\":\"" + oggetto + "\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":" + initiator + ",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}," +
                "{\"key\":" + startDate + "Great,\"value\":\"" + new DateTime().plusDays(1).toString("yyyy-MM-dd") + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());
        response = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) response.getBody().getData();
        assertEquals(0, entities.size());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
    }
    
    
    
    
    /* ----- TASKS ----- */
    @Test
    public void testTaskSearch() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());

        util.logout();
        util.loginSfd();
        //verifico che la ricerca recuperi il primo task della process instance appena avviata
        MockHttpServletRequest request = new MockHttpServletRequest();
        String content = "{\"processParams\":" +
                "[{\"key\":\"" + oggetto + "\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"" + initiator + "\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica Decisione\",\"type\":null}]}";
        request.setContent(content.getBytes());
        
        Map<String, String> requestParams = new HashMap<>();
        
        requestParams.put("oggetto", "textEqual="+ TITOLO_DELL_ISTANZA_DEL_FLUSSO);
        requestParams.put(initiator.name(), "textEqual="+ TestServices.getRA());
        requestParams.put("processDefinitionKey", ALL_PROCESS_INSTANCES);
        requestParams.put("order", ASC);
        requestParams.put("active", "true");
        requestParams.put("firstResult", "0");
        requestParams.put("maxResults", "100");
        

        // TODO fixare e decommentare
//        ResponseEntity response = flowsSearchResource.search(request, ALL_PROCESS_INSTANCES, true, ASC, 0, 100);
//        ArrayList<HistoricTaskInstanceResponse> tasks = (ArrayList<HistoricTaskInstanceResponse>) ((HashMap) response.getBody()).get("tasks");
//        assertEquals(Long.valueOf("1"), ((HashMap) response.getBody()).get("totalItems"));
//        assertEquals(1, tasks.size());
//        assertEquals(util.getFirstTaskId(), ((HistoricTaskInstanceResponse) tasks.get(0)).getId());
        
        //verifico che con parametri di ricerca sbagliati non abbia task nel searchResult
        verifyBadTaskSearchParams(request);
    }
    
    private void verifyBadTaskSearchParams(MockHttpServletRequest request) {
        String content;
        ResponseEntity<DataResponse> response;//verifico che non prenda nessun elemento (SEARCH PARAMS SBAGLIATI)
        //titolo flusso sbagliato
        content = "{\"processParams\":" +
                "[{\"key\":\"" + oggetto + "\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "AAA\",\"type\":\"text\"}," +
                "{\"key\":\"" + initiator + "\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica Decisione\",\"type\":null}]}";
        request.setContent(content.getBytes());
        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(0, response.getBody().getSize());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        //initiator sbaliato
        content = "{\"processParams\":" +
                "[{\"key\":\"" + oggetto + "\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"" + initiator + "\",\"value\":\"admi\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica Decisione\",\"type\":null}]}";
        request.setContent(content.getBytes());
        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(0, response.getBody().getSize());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        //Fase sbaliata
        content = "{\"processParams\":" +
                "[{\"key\":\"" + oggetto + "\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"" + initiator + "\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica DecisioneEEEEE\",\"type\":null}]}";
        request.setContent(content.getBytes());
        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(0, response.getBody().getSize());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
    }    
}
