package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestUtil;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class)
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
        processDefinitionKey = util.mySetUp("missioni");
    }

    @After
    public void tearDown() {
        util.myTearDown();
//        util.logout();
    }

    @Test
    public void testGetMyProcesses() {
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
    public void testGetActiveProcessInstances() {
        //Recupero la Process Definition per permessi ferie
        util.loginUser();
        DataResponse appo = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();
        String permessiFeriePD = ((ArrayList<ProcessDefinitionResponse>) appo.getData()).stream()
                .filter(h -> h.getKey().contains("permessi-ferie"))
                .collect(Collectors.toList()).get(0).getId();
        //User crea una seconda Process Instance
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("definitionId", permessiFeriePD);
        flowsTaskResource.completeTask(req);
        util.logout();

        //Verifico che Admin veda entrambe le Process Instances create
        util.loginAdmin();
        ResponseEntity ret = flowsProcessInstanceResource.getActiveProcessInstances(new MockHttpServletRequest());
        assertEquals(HttpStatus.OK, ret.getStatusCode());
        ArrayList<ProcessInstanceResponse> entities = (ArrayList<ProcessInstanceResponse>) ret.getBody();
        //vedo sia la Process Instance avviata da admin che quella avviata da User
        assertEquals(2, entities.size());
        assertEquals(processDefinitionKey, entities.get(0).getProcessDefinitionId());
        assertEquals(permessiFeriePD, entities.get(1).getProcessDefinitionId());
    }


    @Test
    @Ignore
    public void testGetWorkflowVariables() throws Exception {
        //TODO: Test goes here...
    }


    private String verifyMyProcesses(int startedByAdmin, int startedBySpaclient) {
        String proceeeInstanceID = null;
        // Admin vede la Process Instance che ha avviato
        ResponseEntity<DataResponse> response = flowsProcessInstanceResource.getMyProcessInstances(true);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedByAdmin, response.getBody().getSize());
        List processInstances = ((List) response.getBody().getData());
        assertEquals(startedByAdmin, processInstances.size());
        if (processInstances.size() > 0)
            proceeeInstanceID = ((HistoricProcessInstance) processInstances.get(0)).getId();
        util.logout();

        // Spaclient NON vede la Process Instance avviata da Admin
        util.loginSpaclient();
        response = flowsProcessInstanceResource.getMyProcessInstances(true);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedBySpaclient, response.getBody().getSize());
        assertEquals(startedBySpaclient, ((List) response.getBody().getData()).size());
        util.logout();
        util.loginAdmin();
        return proceeeInstanceID;
    }

}
