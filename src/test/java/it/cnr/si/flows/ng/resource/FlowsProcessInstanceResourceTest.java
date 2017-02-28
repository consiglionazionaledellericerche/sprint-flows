package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.web.rest.TestUtil;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;


@SpringBootTest
@ContextConfiguration(classes = {FlowsApp.class})
@RunWith(SpringRunner.class)
public class FlowsProcessInstanceResourceTest {

    @Autowired
    FlowsTaskResource flowsTaskResource;
    @Autowired
    FlowsProcessInstanceResource flowsProcessInstanceResource;
    @Autowired
    TestUtil util;
    @Autowired
    FlowsProcessDefinitionResource flowsProcessDefinitionResource;
    private String processDefinitionKey;


    @Before
    public void setUp() throws Exception {
        util.loginAdmin();
        DataResponse ret = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();

        Map<String, Object> data = new HashMap();
        processDefinitionKey = ((ProcessDefinitionResponse) ((ArrayList) ret.getData()).get(8)).getId();
        data.put("definitionId", processDefinitionKey);
        flowsTaskResource.completeTask(new MockHttpServletRequest(), data);
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
    public void testGetWorkflowInstances() throws Exception {
        //TODO: Test goes here...
    }

    @Test
    public void testGetProcessInstanceById() throws Exception {
        //TODO: Test goes here...
    }

    @Test
    public void testGetProcessInstancesActives() throws Exception {
        ResponseEntity<Map<String, Object>> ret = flowsProcessInstanceResource.getProcessInstancesActives(new MockHttpServletRequest());

        assertEquals(ret.getStatusCode(), HttpStatus.OK);

        Map resp = ret.getBody();
        ProcessInstanceResponse entity = (ProcessInstanceResponse) ((List) resp.get("entities")).get(0);
        assertEquals(entity.getProcessDefinitionId(), processDefinitionKey);
    }


    @Test
    public void testGetWorkflowVariables() throws Exception {
        //TODO: Test goes here...
    }
}
