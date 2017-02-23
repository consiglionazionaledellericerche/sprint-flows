package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static it.cnr.si.web.rest.TestUtil.login;
import static it.cnr.si.web.rest.TestUtil.logout;


@SpringBootTest
@ContextConfiguration(classes = {FlowsApp.class})
@RunWith(SpringRunner.class)
public class FlowsTaskResourceTest {

    @Autowired
    FlowsTaskResource flowsTaskResource;

    @Autowired
    FlowsProcessDefinitionResource flowsProcessDefinitionResource;


    @Before
    public void setUp() throws Exception {
        login();
        DataResponse ret = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();

        Map<String, Object> data = new HashMap();
        data.put("definitionId", ((ProcessDefinitionResponse) ((ArrayList) ret.getData()).get(8)).getId());
        flowsTaskResource.completeTask(new MockHttpServletRequest(), data);
    }

    @After
    public void tearDown() {
        logout();
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
}