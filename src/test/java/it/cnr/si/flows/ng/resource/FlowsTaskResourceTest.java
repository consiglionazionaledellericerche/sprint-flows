package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.web.rest.TestUtil;
import org.activiti.rest.common.api.DataResponse;
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
    TestUtil util;


    @Before
    public void setUp() throws Exception {
        util = new TestUtil();
        util.loginAdmin();
        DataResponse ret = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();

        Map<String, Object> data = new HashMap();
        data.put("definitionId", ((ProcessDefinitionResponse) ((ArrayList) ret.getData()).get(8)).getId());
        ResponseEntity<Object> response = flowsTaskResource.completeTask(new MockHttpServletRequest(), data);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @After
    public void tearDown() {
        util.logout();
//        super.tearDown();
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
        util.loginAdmin();


        //TODO: Test goes here...
    }
}