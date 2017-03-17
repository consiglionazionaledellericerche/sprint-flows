package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestUtil;
import org.activiti.engine.TaskService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@SpringBootTest
@ContextConfiguration(classes = {FlowsApp.class})
@RunWith(SpringRunner.class)
public class FlowsTaskResourceTest {

    @Autowired
    FlowsTaskResource flowsTaskResource;

    @Autowired
    FlowsProcessDefinitionResource flowsProcessDefinitionResource;
    @Autowired
    TestUtil util;
    @Autowired
    FlowsProcessInstanceResource flowsProcessInstanceResource;
    private String taskId;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RestResponseFactory restResponseFactory;
    private String processDefinitionMissioni;


    @Before
    public void setUp() throws Exception {
        util.loginAdmin();
        DataResponse ret = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();

        ArrayList<ProcessDefinitionResponse> processDefinitions = (ArrayList) ret.getData();
        for (ProcessDefinitionResponse pd : processDefinitions) {
            if (pd.getId().contains("missioni")) {
                processDefinitionMissioni = pd.getId();
                break;
            }
        }
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("definitionId", processDefinitionMissioni);
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
//        Recupero il taskId
        taskId = taskService.createTaskQuery().singleResult().getId();
    }

    @After
    public void tearDown() {
        util.logout();
    }

    @Test
    public void testGetMyTasks() {
        //TODO: Test goes here...
    }

    @Test
    public void testGetAvailableTasks() {
        //TODO: Test goes here...
    }


    @Test
    public void testGetTaskInstance() {
        //TODO: Test goes here...
    }

    @Test
    public void testGetTaskVariables() {
        //TODO: Test goes here...
    }

    @Test
    public void testCompleteTask() {
        //TODO: Test goes here...
    }

    @Test
    public void testAssignTask() {
        //TODO: Test goes here...
    }

    @Test
    public void testUnclaimTask() {
        //TODO: Test goes here...
    }

    @Test
    public void testClaimTask() {
//      admin ha ROLE_ADMIN E ROLE_USER quindi può richiamare il metodo
        util.loginAdmin();
        ResponseEntity<Map<String, Object>> response = flowsTaskResource.claimTask(new MockHttpServletRequest(), taskId);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        util.logout();

//      spaclient ha solo ROLE_ADMIN quindi NON può richiamare il metodo
        util.loginSpaclient();
        response = flowsTaskResource.claimTask(new MockHttpServletRequest(), taskId);
        assertEquals(response.getStatusCode(), HttpStatus.FORBIDDEN);
    }

    @Test
    public void testSearchTask() {
        util.loginAdmin();
        MockHttpServletRequest req = new MockHttpServletRequest();

        String searchField1 = "wfvarValidazioneSpesa";
        String searchField2 = "initiator";
        String payload = "{params: [{key: " + searchField1 + ", value: true, type: boolean} , {key: " + searchField2 + ", value: \"admin\", type: textEqual}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");
        //verifico la richiesta normale
        ResponseEntity<Object> response = flowsTaskResource.search(req, processDefinitionMissioni.split(":")[0], true, ASC, 0, 10);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        ArrayList responseList = (ArrayList) response.getBody();
        assertEquals(responseList.size(), 1);
        TaskResponse taskresponse = ((TaskResponse) responseList.get(0));
        assertTrue(taskresponse.getProcessDefinitionId().contains(processDefinitionMissioni));

        //verifico che la Process Instance restituita rispetti i parametri della ricerca
        List<RestVariable> variables = taskresponse.getVariables();
        RestVariable variable = variables.stream().filter(v -> v.getName().equals(searchField1)).collect(Collectors.toList()).get(0);
        assertEquals(variable.getValue(), true);

        variable = variables.stream().filter(v -> v.getName().equals(searchField2)).collect(Collectors.toList()).get(0);
        assertEquals(variable.getValue(), "admin");

        //cerco le Process Instance completate (0 risultati)
        response = flowsTaskResource.search(req, processDefinitionMissioni.split(":")[0], false, ASC, 0, 10);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        responseList = (ArrayList) response.getBody();
        assertEquals(responseList.size(), 0);

        //parametri sbagliati (0 risultati)
        payload = "{params: [{key: " + searchField1 + ", value: false, type: boolean} , {key: initiator, value: \"admin\", type: textEqual}]}";
        req.setContent(payload.getBytes());
        req.setContentType("application/json");

        response = flowsTaskResource.search(req, processDefinitionMissioni.split(":")[0], true, ASC, 0, 10);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        responseList = (ArrayList) response.getBody();
        assertEquals(responseList.size(), 0);
    }
}