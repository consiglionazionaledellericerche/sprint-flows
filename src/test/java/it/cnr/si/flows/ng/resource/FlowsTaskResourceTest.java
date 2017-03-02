package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestUtil;
import org.activiti.engine.TaskService;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.junit.After;
import org.junit.Assert;
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
import java.util.HashMap;
import java.util.Map;



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


    @Before
    public void setUp() throws Exception {
        util.loginAdmin();
        DataResponse ret = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();

        Map<String, Object> data = new HashMap();
        ArrayList<ProcessDefinitionResponse> processDefinitions = (ArrayList) ret.getData();
        String processDefinitionMissioni = null;
        for (ProcessDefinitionResponse pd : processDefinitions) {
            if (pd.getId().contains("missioni")) {
                processDefinitionMissioni = pd.getId();
                break;
            }
        }
        data.put("definitionId", processDefinitionMissioni);
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("definitionId", processDefinitionMissioni);
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
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
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        util.logout();

//      spaclient ha solo ROLE_ADMIN quindi NON può richiamare il metodo
        util.loginSpaclient();
        response = flowsTaskResource.claimTask(new MockHttpServletRequest(), taskId);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.FORBIDDEN);
    }
}