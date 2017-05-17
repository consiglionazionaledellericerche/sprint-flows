package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestUtil;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.activiti.rest.common.api.DataResponse;
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

import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
        ResponseEntity<Map<String, Object>> response = flowsTaskResource.getTask(util.getFirstTaskId());
        assertEquals(OK, response.getStatusCode());
        assertEquals(TASK_NAME, ((TaskResponse) response.getBody().get("task")).getName());
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
    public void testGetTasksCompletedForMe() {
        //completo il primo task
        util.loginUser();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", util.getFirstTaskId());
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());

        //assegno il task a user
        flowsTaskResource.assignTask(req, taskService.createTaskQuery().singleResult().getId(), "user");
        //Setto user come owner dello stesso task
        taskService.setOwner(taskService.createTaskQuery().singleResult().getId(), "user");

        //Recupero solo il flusso completato da user e non quello assegnatogli né quello di cui è owner
        response = flowsTaskResource.getTasksCompletedByMe(new MockHttpServletRequest(), 0, 1000);
        assertEquals(OK, response.getStatusCode());
        assertEquals(util.getFirstTaskId(), ((HistoricTaskInstanceEntity) ((ArrayList) ((Map) response.getBody()).get("tasks")).get(0)).getId());

        //Verifico che il metodo funzioni anche con admin
        util.logout();
        util.loginAdmin();
        response = flowsTaskResource.getTasksCompletedByMe(new MockHttpServletRequest(), 0, 1000);
        assertEquals(OK, response.getStatusCode());
        assertEquals("Admin non deve vedere task perchè non l'ha ANCORA completato ma ha solo avviato il flusso", 0, ((ArrayList) ((Map) response.getBody()).get("tasks")).size());

        //completo un altro task con admin
        req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", taskService.createTaskQuery().singleResult().getId());
        response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());

        //Admin vede solo il task che ha completato
        response = flowsTaskResource.getTasksCompletedByMe(new MockHttpServletRequest(), 0, 1000);
        assertEquals(OK, response.getStatusCode());
        assertEquals("Admin non vede il task che ha appena completato", 1, ((ArrayList) ((Map) response.getBody()).get("tasks")).size());
    }
}