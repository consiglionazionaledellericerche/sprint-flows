package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestUtil;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static it.cnr.si.flows.ng.utils.Utils.DESC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class FlowsTaskResourceTest {

    public static final String TASK_NAME = "Firma UO";
    @Autowired
    FlowsTaskResource flowsTaskResource;
    @Autowired
    TestUtil util;
    @Autowired
    FlowsProcessInstanceResource flowsProcessInstanceResource;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private ProcessInstanceResponse processInstance;
    @Autowired
    private TaskService taskService;

    @Before
    public void setUp() throws Exception {
        processInstance = util.mySetUp("missioni");
    }



    @After
    public void tearDown() {
        util.myTearDown();
    }


    @Test
    @Ignore
    public void testGetMyTasks() {
        //TODO: trovare un flusso che assegni un task ad un utente specifico
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetAvailableTasks() {
        //QADMIN ha sia ROLE_ADMIN che ROLE_USER (quindi può vedere il task istanziato)
        ResponseEntity<DataResponse> response = flowsTaskResource.getAvailableTasks();
        assertEquals(OK, response.getStatusCode());
        assertEquals(1, response.getBody().getSize());
        assertEquals(1, ((ArrayList) response.getBody().getData()).size());
        util.logout();

        //USER è solo ROLE_USER (quindi può vedere il task istanziato)
        util.loginUser();
        response = flowsTaskResource.getAvailableTasks();
        assertEquals(OK, response.getStatusCode());
        assertEquals(1, response.getBody().getSize());
        assertEquals(1, ((ArrayList) response.getBody().getData()).size());
        util.logout();

        //spaclient è solo ROLE_ADMIN (quindi non può accedere al servizio - AccessDeniedException)
        util.loginSpaclient();
        flowsTaskResource.getAvailableTasks();
    }


    @Test
    public void testGetTaskInstance() {
        ResponseEntity<TaskResponse> response = flowsTaskResource.getTaskInstance(util.getFirstTaskId());
        assertEquals(OK, response.getStatusCode());
        assertEquals(TASK_NAME, ((TaskResponse) response.getBody()).getName());
    }

    @Test
    @Ignore
    public void testGetTaskVariables() {
        //TODO: Test goes here...
//        flowsTaskResource.getTaskVariables()
    }

    @Test
    @Ignore
    public void testCompleteTask() {
        //TODO: Test goes here...
//        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
//        req.setParameter("taskId", taskId);
//        req.setParameter("definitionId", processDefinitionMissioni);
//        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
//        assertEquals(OK, response.getStatusCode());
    }

    @Test
    @Ignore
    public void testAssignTask() {
        //TODO: Test goes here...
//        flowsTaskResource.assignTask();
    }

    @Test
    @Ignore
    public void testUnclaimTask() {
        //TODO: Test goes here...
//        flowsTaskResource.unclaimTask();
    }

    @Test
    public void testClaimTask() {
//      admin ha ROLE_ADMIN E ROLE_USER quindi può richiamare il metodo
        util.loginAdmin();
        ResponseEntity<Map<String, Object>> response = flowsTaskResource.claimTask(new MockHttpServletRequest(), util.getFirstTaskId());
        assertEquals(OK, response.getStatusCode());
        util.logout();

//      spaclient ha solo ROLE_ADMIN quindi NON può richiamare il metodo
        util.loginSpaclient();
        response = flowsTaskResource.claimTask(new MockHttpServletRequest(), util.getFirstTaskId());
        assertEquals(FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testSearchTask() throws ParseException {
        util.loginAdmin();
        MockHttpServletRequest req = new MockHttpServletRequest();

        String searchField1 = "wfvarValidazioneSpesa";
        String searchField2 = "initiator";
//        String payload = "{params: [{key: " + searchField1 + ", value: true, type: boolean} , {key: " + searchField2 + ", value: \"admin\", type: textEqual}]}";
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, +1);
        Date tomorrow = cal.getTime();
        cal.add(Calendar.DATE, -2);
        Date yesterday = cal.getTime();
        String payload = "{params: [{key: " + searchField1 + ", value: true, type: boolean}, " +
                "{key: initiator, value: \"admin\", type: textEqual}, " +
                "{key: \"startDateGreat\", value: \"" + sdf.format(yesterday) + "\", type: \"date\"}," +
                "{key: \"startDateLess\", value: \"" + sdf.format(tomorrow) + "\", type: \"date\"}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");
        //verifico la richiesta normale
        ResponseEntity<Object> response = flowsTaskResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 10);
        verifyResponse(response, 1, searchField1, searchField2);

        //verifico la richiesta su tutte le Process Definition
        response = flowsTaskResource.search(req, "all", true, ASC, 0, 10);
        verifyResponse(response, 1, searchField1, searchField2);

        //cerco le Process Instance completate (active = false  ==>  0 risultati)
        response = flowsTaskResource.search(req, util.getProcessDefinition().split(":")[0], false, DESC, 0, 10);
        verifyResponse(response, 0, searchField1, searchField2);

        //parametri sbagliati (0 risultati)
        payload = "{params: [{key: " + searchField1 + ", value: false, type: boolean} , {key: initiator, value: \"admin\", type: textEqual}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        response = flowsTaskResource.search(req, util.getProcessDefinition().split(":")[0], true, ASC, 0, 10);
        verifyResponse(response, 0, searchField1, searchField2);
    }

    @Test
    public void testGetTasksCompletedForMe() {
        //completo il primo task
        util.loginSpaclient();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", util.getFirstTaskId());
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());

        //assegno il task a spaclient
        flowsTaskResource.assignTask(req, taskService.createTaskQuery().singleResult().getId(), "spaclient");
        //Setto spaclient come owner dello stesso task
        taskService.setOwner(taskService.createTaskQuery().singleResult().getId(), "spaclient");

        //Recupero solo il flusso completato da spaclient e non quello assegnatogli né quello di cui è owner
        response = flowsTaskResource.getTasksCompletedForMe(new MockHttpServletRequest(), 0, 1000);
        assertEquals(OK, response.getStatusCode());
        assertEquals(util.getFirstTaskId(), ((HistoricTaskInstanceEntity) ((ArrayList) ((Map) response.getBody()).get("tasks")).get(0)).getId());

        //Verifico che il metodo funzioni anche con admin
        util.logout();
        util.loginAdmin();
        response = flowsTaskResource.getTasksCompletedForMe(new MockHttpServletRequest(), 0, 1000);
        assertEquals(OK, response.getStatusCode());
        assertEquals("Admin non deve vedere task perchè non l'ha ANCORA completato ma ha solo avviato il flusso", 0, ((ArrayList) ((Map) response.getBody()).get("tasks")).size());

        //completo un altro task con admin
        req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", taskService.createTaskQuery().singleResult().getId());
        response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());

        //Admin vede solo il task che ha completato
        response = flowsTaskResource.getTasksCompletedForMe(new MockHttpServletRequest(), 0, 1000);
        assertEquals(OK, response.getStatusCode());
        assertEquals("Admin non vede il task che ha appena completato", 1, ((ArrayList) ((Map) response.getBody()).get("tasks")).size());
    }

    private void verifyResponse(ResponseEntity<Object> response, int expectedTotalItems, String searchField1, String searchField2) {
        assertEquals(OK, response.getStatusCode());
        HashMap body = (HashMap) response.getBody();
        ArrayList responseList = (ArrayList) body.get("tasks");
        assertEquals(responseList.size(), ((Long) body.get("totalItems")).intValue());
        assertEquals(expectedTotalItems, ((Long) body.get("totalItems")).intValue());

        if (responseList.size() > 0) {
            HistoricTaskInstanceResponse taskresponse = ((HistoricTaskInstanceResponse) responseList.get(0));
            assertTrue(taskresponse.getProcessDefinitionId().contains(util.getProcessDefinition()));
            //verifico che la Process Instance restituita rispetti i parametri della ricerca
            List<RestVariable> variables = taskresponse.getVariables();
            RestVariable variable = variables.stream().filter(v -> v.getName().equals(searchField1)).collect(Collectors.toList()).get(0);
            assertEquals(true, variable.getValue());

            variable = variables.stream().filter(v -> v.getName().equals(searchField2)).collect(Collectors.toList()).get(0);
            assertEquals("admin", variable.getValue());
        }
    }
}