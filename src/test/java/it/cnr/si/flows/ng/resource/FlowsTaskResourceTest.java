package it.cnr.si.flows.ng.resource;

import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import org.activiti.engine.TaskService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static it.cnr.si.flows.ng.TestServices.TITOLO_DELL_ISTANZA_DEL_FLUSSO;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class FlowsTaskResourceTest {

    public static final String FIRST_TASK_NAME = "Verifica Decisione";
    private static final String SECOND_TASK_NAME = "Firma Decisione";
    @Autowired
    private FlowsTaskResource flowsTaskResource;
    @Autowired
    private TestServices util;
    @Autowired
    private FlowsProcessInstanceResource flowsProcessInstanceResource;
    private ProcessInstanceResponse processInstance;
    @Autowired
    private TaskService taskService;

    @Before
    public void setUp() {
        HttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @After
    public void tearDown() {
        util.myTearDown();
    }



    @Test
    public void testGetMyTasks() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());
//       all'inizio del test SFD non ha task assegnati'
        util.loginSfd();
        ResponseEntity<DataResponse> response = flowsTaskResource.getMyTasks(new MockHttpServletRequest(), ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(0, response.getBody().getSize());
        ArrayList myTasks = (ArrayList) response.getBody().getData();
        assertEquals(0, myTasks.size());

        flowsTaskResource.claimTask(util.getFirstTaskId());

        MockHttpServletRequest request = new MockHttpServletRequest();
        String content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica Decisione\",\"type\":null}]}";
        request.setContent(content.getBytes());
        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        myTasks = (ArrayList) response.getBody().getData();
        assertEquals(1, response.getBody().getSize());
        assertEquals(1, myTasks.size());
        assertEquals(util.getFirstTaskId(), ((TaskResponse) myTasks.get(0)).getId());

        verifyBadSearchParams(request);

//        verifico che non prenda nessun risultato ( DOPO CHE IL TASK VIENE DISASSEGNATO)
        flowsTaskResource.unclaimTask(util.getFirstTaskId());

        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(0, response.getBody().getSize());
        myTasks = (ArrayList) response.getBody().getData();
        assertEquals(0, myTasks.size());
    }


    @Test
    public void testGetAvailableTasks() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());
        //SFD è sfd@sisinfo (quindi può vedere il task istanziato)
        util.logout();
        util.loginSfd();
        ResponseEntity<DataResponse> response = flowsTaskResource.getAvailableTasks(new MockHttpServletRequest(), ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(1, response.getBody().getSize());
        assertEquals(1, ((ArrayList) response.getBody().getData()).size());
        util.logout();

        //USER NON è sfd@sisinfo (quindi NON può vedere il task istanziato)
        util.loginUser();
        response = flowsTaskResource.getAvailableTasks(new MockHttpServletRequest(), ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(0, response.getBody().getSize());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        util.logout();
    }


    @Test(expected = AccessDeniedException.class)
    public void testGetTaskInstance() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());
        ResponseEntity<Map<String, Object>> response = flowsTaskResource.getTask(util.getFirstTaskId());
        assertEquals(OK, response.getStatusCode());
        assertEquals(FIRST_TASK_NAME, ((TaskResponse) response.getBody().get("task")).getName());
        //verifico che gli utenti SENZA ROLE_ADMIN non possano accedere al servizio
        util.logout();
        util.loginUser();
        flowsTaskResource.getTask(util.getFirstTaskId());
    }


    @Test(expected = AccessDeniedException.class)
    public void testVerifyGetTaskInstance() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());

        //verifico che gli utenti che NON SONO IN GRUPPI CANDIDATE DEL TASK NE' ROLE_ADMIN
        // (ad esempio il direttore) non possano accedere al servizio
        util.loginDirettore();
        flowsTaskResource.getTask(util.getFirstTaskId());
    }


    @Test
    public void testCompleteTask() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());
        //completo il primo task
        util.loginSfd();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", util.getFirstTaskId());
        assertEquals(OK, flowsTaskResource.completeTask(req).getStatusCode());

        //verifico che il task completato sia avanzato
        String content = "{\"processParams\":[],\"taskParams\":[]}";
        MockMultipartHttpServletRequest searchRequest = new MockMultipartHttpServletRequest();
        searchRequest.setContent(content.getBytes());
        ResponseEntity<Object> response = flowsTaskResource.search(searchRequest, ALL_PROCESS_INSTANCES, true, ASC, 0, 100);
        assertEquals(OK, response.getStatusCode());
        assertEquals(SECOND_TASK_NAME, ((ArrayList<HistoricTaskInstanceResponse>) ((HashMap) response.getBody()).get("tasks")).get(0).getName());
    }


    @Test
    public void testSearch() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());

        util.logout();
        util.loginSfd();
        //verifico che la ricerca recuperi il primo task della process instance appena avviata
        MockHttpServletRequest request = new MockHttpServletRequest();
        String content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica Decisione\",\"type\":null}]}";
        request.setContent(content.getBytes());
        ResponseEntity response = flowsTaskResource.search(request, ALL_PROCESS_INSTANCES, true, ASC, 0, 100);
        ArrayList<HistoricTaskInstanceResponse> tasks = (ArrayList<HistoricTaskInstanceResponse>) ((HashMap) response.getBody()).get("tasks");
        assertEquals(Long.valueOf("1"), ((HashMap) response.getBody()).get("totalItems"));
        assertEquals(1, tasks.size());
        assertEquals(util.getFirstTaskId(), ((HistoricTaskInstanceResponse) tasks.get(0)).getId());
        //verifico che con parametri di ricerca sbagliati non abbia task nel searchResult
        verifyBadSearchParams(request);
    }

    @Test
    public void testTaskAssignedInMyGroups() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());
        MockHttpServletRequest request = new MockHttpServletRequest();

        //sfd NON deve vedere NESSUN Task prima dell'assegnazione del task a responsabileAcquisti2
        util.loginSfd();
        ResponseEntity<DataResponse> response = flowsTaskResource.taskAssignedInMyGroups(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(0, (new ArrayList((Collection) response.getBody().getData())).size());

        //assegno il task a responsabileAcquisti2
        util.loginResponsabileAcquisti2();
        ResponseEntity<Map<String, Object>> resp = flowsTaskResource.claimTask(util.getFirstTaskId());
        assertEquals(OK, resp.getStatusCode());
        response = flowsTaskResource.taskAssignedInMyGroups(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(0, (new ArrayList((Collection) response.getBody().getData())).size());

        //verifico che responsabileAcquisti veda il task assegnato ad responsabileaquisti2 PERCHÈ FANNO PARTE DELLO STESSO GRUPPO (RA)
        util.loginResponsabileAcquisti();
        response = flowsTaskResource.taskAssignedInMyGroups(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(1, (new ArrayList((Collection) response.getBody().getData())).size());

        //sfd NON deve vedere il Task assegnato a responsabileAcquisti2 perchè NON hanno NESSUN gruppo in comune
        util.loginSfd();
        response = flowsTaskResource.taskAssignedInMyGroups(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals(0, (new ArrayList((Collection) response.getBody().getData())).size());
    }

    // non uso expected perche' voglio controllare *esttamente* dove viene lanciata l'eccezione
    @Test
    public void testClaimTask() throws IOException {
        processInstance = util.mySetUp(acquisti.getValue());

//      sfd è sfd@sisinfo quindi può prendere in carico il flusso
        util.loginSfd();
        ResponseEntity<Map<String, Object>> response = flowsTaskResource.claimTask(util.getFirstTaskId());
        assertEquals(OK, response.getStatusCode());
        response = flowsTaskResource.unclaimTask(util.getFirstTaskId());
        assertEquals(OK, response.getStatusCode());
        util.logout();

//        todo: testare il caso in cui un utente non opuò richiamare il metodo(non spaclient)
////      spaclient NON è sfd@sisinfo quindi NON può richiamare il metodo
//        util.loginSpaclient();
//        try {
//            response = flowsTaskResource.claimTask(util.getFirstTaskId());
//            fail("Expected AccessDeniedException");
//        } catch (AccessDeniedException e) { /* expected */}
    }


    @Test
    public void testGetTasksCompletedForMe() throws ArubaSignServiceException, IOException {
        processInstance = util.mySetUp(acquisti.getValue());
        //completo il primo task
        util.loginSfd();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", util.getFirstTaskId());
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());

        //assegno il task a user
        flowsTaskResource.assignTask(req, taskService.createTaskQuery().singleResult().getId(), "user");
        //Setto user come owner dello stesso task
        taskService.setOwner(taskService.createTaskQuery().singleResult().getId(), "user");

        //Recupero solo il flusso completato da user e non quello assegnatogli né quello di cui è owner
        response = flowsTaskResource.getTasksCompletedByMe(new MockHttpServletRequest(), ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        assertEquals(OK, response.getStatusCode());
        ArrayList<HistoricTaskInstanceResponse> tasks = (ArrayList<HistoricTaskInstanceResponse>) ((DataResponse) response.getBody()).getData();
        assertTrue(tasks.stream().anyMatch(t -> t.getId().equals(util.getFirstTaskId())));


        //Verifico che il metodo funzioni anche con ADMIN
        util.logout();
        util.loginAdmin();
        response = flowsTaskResource.getTasksCompletedByMe(new MockHttpServletRequest(), ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        assertEquals(OK, response.getStatusCode());
        assertEquals("ADMIN non deve vedere task perchè NON NE HA COMPLETATO NESSUNO ma ha solo avviato il flusso",
                     0, ((ArrayList<HistoricTaskInstanceResponse>) ((DataResponse) response.getBody()).getData()).size());
    }


    private void verifyBadSearchParams(MockHttpServletRequest request) {
        String content;
        ResponseEntity<DataResponse> response;//verifico che non prenda nessun elemento (SEARCH PARAMS SBAGLIATI)
        //titolo flusso sbagliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "AAA\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica Decisione\",\"type\":null}]}";
        request.setContent(content.getBytes());
        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(0, response.getBody().getSize());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        //initiator sbaliato
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"admi\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica Decisione\",\"type\":null}]}";
        request.setContent(content.getBytes());
        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(0, response.getBody().getSize());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
        //Fase sbaliata
        content = "{\"processParams\":" +
                "[{\"key\":\"titoloIstanzaFlusso\",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":\"initiator\",\"value\":\"" + TestServices.getRA() + "\",\"type\":\"textEqual\"}]," +
                "\"taskParams\":" +
                "[{\"key\":\"Fase\",\"value\":\"Verifica DecisioneEEEEE\",\"type\":null}]}";
        request.setContent(content.getBytes());
        response = flowsTaskResource.getMyTasks(request, ALL_PROCESS_INSTANCES, 0, 100, ASC);
        assertEquals(0, response.getBody().getSize());
        assertEquals(0, ((ArrayList) response.getBody().getData()).size());
    }
}