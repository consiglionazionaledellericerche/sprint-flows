package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.utils.Utils;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.task.IdentityLink;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.history.HistoricProcessInstanceResponse;
import org.activiti.rest.service.api.history.HistoricTaskInstanceResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static it.cnr.si.flows.ng.TestServices.TITOLO_DELL_ISTANZA_DEL_FLUSSO;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.*;
import static it.cnr.si.flows.ng.utils.Utils.ALL_PROCESS_INSTANCES;
import static it.cnr.si.flows.ng.utils.Utils.ASC;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles(profiles = "test,cnr")
//@ActiveProfiles(profiles = "test,oiv")
public class FlowsProcessInstanceResourceTest {

    private static final int LOAD_TEST_PROCESS_INSTANCES = 700;
    private static int processDeleted = 0;
    @Autowired
    RuntimeService runtimeService;
    @Inject
    private HistoryService historyService;
    @Inject
    private FlowsTaskResource flowsTaskResource;
    @Inject
    private FlowsProcessInstanceResource flowsProcessInstanceResource;
    @Inject
    private TestServices util;
    @Inject
    private FlowsProcessDefinitionResource flowsProcessDefinitionResource;
    @Inject
    private RepositoryService repositoryService;

    private StopWatch stopWatch = new StopWatch();
    private ProcessInstanceResponse processInstance;
    @Inject
    private Utils utils;



    @Before
    public void setUp() {
        HttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @After
    public void tearDown() {
        System.out.println(stopWatch.prettyPrint());
        processDeleted = processDeleted + util.myTearDown();
    }

    @Test
    public void testGetMyProcesses() throws IOException {
        processInstance = util.mySetUp(acquisti);
        String processInstanceID = verifyMyProcesses(1, 0);
        // testo che, anche se una Process Instance viene sospesa, la vedo ugualmente
        util.loginAdmin();
        flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstanceID);

        util.loginResponsabileAcquisti();
        processInstanceID = verifyMyProcesses(1, 0);
        //testo che sospendendo una Process Instances NON la vedo tra i processi avviati da me
        util.loginAdmin();
        MockHttpServletResponse response = new MockHttpServletResponse();
        flowsProcessInstanceResource.delete(response, processInstanceID, "TEST");
        assertEquals(NO_CONTENT.value(), response.getStatus());
        util.loginResponsabileAcquisti();
        verifyMyProcesses(0, 0);
    }


    @Test(expected = AccessDeniedException.class)
    public void testGetProcessInstanceById() throws IOException {
        processInstance = util.mySetUp(acquisti);

        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) flowsProcessInstanceResource.getProcessInstanceById(new MockHttpServletRequest(), processInstance.getId(), true);
        assertEquals(OK, response.getStatusCode());

        HistoricProcessInstanceResponse entity = (HistoricProcessInstanceResponse) ((HashMap) response.getBody()).get("entity");
        assertEquals(processInstance.getId(), entity.getId());
        assertEquals(processInstance.getBusinessKey(), entity.getBusinessKey());
        assertEquals(processInstance.getProcessDefinitionId(), entity.getProcessDefinitionId());

        HashMap appo = (HashMap) ((HashMap) response.getBody()).get("identityLinks");
        assertEquals(2, appo.size());
        HashMap identityLinks = (HashMap) appo.get(appo.keySet().toArray()[1]); // TODO
        assertEquals("[${gruppoSFD}]", identityLinks.get("candidateGroups").toString());
        assertEquals(0, ((HashSet) identityLinks.get("candidateUsers")).size());
        assertEquals(1, ((ArrayList) identityLinks.get("links")).size());
        assertNull(identityLinks.get("assignee"));

        HashMap history = (HashMap) ((ArrayList) ((HashMap) response.getBody()).get("history")).get(0);
        assertEquals(processInstance.getId(), ((HistoricTaskInstanceResponse) history.get("historyTask")).getProcessInstanceId());
        assertEquals(1, ((ArrayList) history.get("historyIdentityLink")).size());

        HashMap attachments = (HashMap) ((HashMap) response.getBody()).get("attachments");
        assertEquals(0, attachments.size());

        //verifica che gli utenti con ROLE_ADMIN POSSANO accedere al servizio
        util.loginAdmin();
        flowsProcessInstanceResource.getProcessInstanceById(new MockHttpServletRequest(), processInstance.getId(), false);

        //verifica AccessDeniedException (risposta 403 Forbidden) in caso di accesso di utenti non autorizzati
        util.loginUser();
        flowsProcessInstanceResource.getProcessInstanceById(new MockHttpServletRequest(), processInstance.getId(), false);
    }

    @Test
    public void testGetProcessInstances() throws IOException {
        processInstance = util.mySetUp(acquisti);

        //responsabileacquisti crea una seconda Process Instance di acquisti con suffisso "2" nel titolo della PI
        util.loginResponsabileAcquisti();
        String acquistiTrasparenzaId = repositoryService.createProcessDefinitionQuery().processDefinitionKey(acquisti.getValue()).latestVersion().singleResult().getId();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("processDefinitionId", acquistiTrasparenzaId);
        req.setParameter(titolo.name(), TITOLO_DELL_ISTANZA_DEL_FLUSSO + "2");
        req.setParameter(descrizione.name(), "descrizione" + "2");
        req.setParameter("tipologiaAcquisizione", "procedura aperta");
        req.setParameter("tipologiaAcquisizioneId", "11");
        req.setParameter("strumentoAcquisizione", "AFFIDAMENTO DIRETTO - MEPA o CONSIP\n");
        req.setParameter("strumentoAcquisizioneId", "11");
        req.setParameter("priorita", "Alta");
        req.setParameter("rup", "spaclient");
        req.setParameter("impegni_json", "[{\"numero\":\"1\",\"importoNetto\":100,\"importoLordo\":120,\"descrizione\":\"descrizione impegno\",\"vocedispesa\":\"11001 - Arretrati per anni precedenti corrisposti al personale a tempo indeterminato\",\"vocedispesaid\":\"11001\",\"gae\":\"spaclient\"}]");

        ResponseEntity<Object> resp = flowsTaskResource.completeTask(req);
        assertEquals(OK, resp.getStatusCode());

        //Verifico che Admin veda entrambe le Process Instances create
        util.loginAdmin();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String content = "{\"processParams\":" +
                "[{\"key\":" + titolo.name() + ",\"value\":\"" + TITOLO_DELL_ISTANZA_DEL_FLUSSO + "\",\"type\":\"text\"}," +
                "{\"key\":" + startDate + "Great,\"value\":\"" + utils.formattaData(new Date()) + "\",\"type\":\"date\"}]}";
        request.setContent(content.getBytes());

        ResponseEntity<DataResponse> ret = flowsProcessInstanceResource.getProcessInstances(request, true, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        assertEquals(HttpStatus.OK, ret.getStatusCode());
        ArrayList<HistoricProcessInstanceResponse> entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();

        //vedo sia la Process Instance avviata da admin che quella avviata da responsabileacquisti
        assertEquals(2, entities.size());
        assertEquals(util.getProcessDefinition(), entities.get(0).getProcessDefinitionId());
        assertEquals(acquistiTrasparenzaId, entities.get(1).getProcessDefinitionId());

        //cancello un processo
        MockHttpServletResponse response = new MockHttpServletResponse();
        String activeId = entities.get(0).getId();
        String notActiveId = entities.get(1).getId();
        flowsProcessInstanceResource.delete(response, notActiveId, "test");
        assertEquals(response.getStatus(), NO_CONTENT.value());

        // verifico che RA veda UN processo terminato/cancellato in più (quello appena concellato + quelli cancellati nel tearDown dei test precedenti)
        util.loginResponsabileAcquisti();
        MockHttpServletRequest voidRequest = new MockHttpServletRequest();
        voidRequest.setContent("{\"processParams\": []}".getBytes());
        ret = flowsProcessInstanceResource.getProcessInstances(voidRequest, false, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();
        assertEquals(processDeleted + 2, entities.size());

        // .. e 1 processo ancora attivo VERIFICANDO CHE GLI ID COINCIDANO
        ret = flowsProcessInstanceResource.getProcessInstances(voidRequest, true, ALL_PROCESS_INSTANCES, 0, 1000, ASC);
        entities = (ArrayList<HistoricProcessInstanceResponse>) ret.getBody().getData();
        assertEquals(1, entities.size());
        assertEquals(activeId, entities.get(0).getId());

        //VERIFICO FUNZIONI LA RICERCA
//        verifyBadSearchParams(request);
        // TODO rifattorizzare anche il test
    }


    @Test
    public void testSuspend() throws IOException {
        processInstance = util.mySetUp(acquisti);
        assertEquals(false, processInstance.isSuspended());
        //solo admin può sospendere il flow
        util.loginAdmin();
        ProcessInstanceResponse response = flowsProcessInstanceResource.suspend(new MockHttpServletRequest(), processInstance.getId());
        assertEquals(true, response.isSuspended());
    }

    @Test
    public void testGetVariable() throws IOException {
        processInstance = util.mySetUp(acquisti);
//        processInstance = util.mySetUp(iscrizioneElencoOiv);



    }




    @Test
    public void testAddAndDeleteGroupIdentityLink() throws IOException {
        processInstance = util.mySetUp(acquisti);
        //le funzionalità può essere acceduta solo con privileggi "ADMIN"
        util.loginAdmin();

        //test-junit@2216 come GRUPPO "VISUALIZZATORE"
        String groupId = "test-junit@2216";
        String userId = null;
        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertFalse(identityLinks.stream()
                            .anyMatch(a -> a.getGroupId() == groupId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));

        ResponseEntity<Void> response = flowsProcessInstanceResource.setIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        assertEquals(OK, response.getStatusCode());
        List<IdentityLink> newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(newIdentityLinks.size(), identityLinks.size() + 1);

        assertTrue(newIdentityLinks.stream()
                           .anyMatch(a -> a.getGroupId() == groupId &&
                                   a.getProcessInstanceId().equals(processInstance.getId())  &&
                                   a.getType() == Utils.PROCESS_VISUALIZER));

        //cancellazione IdentityLink aggiunto prima
        response = flowsProcessInstanceResource.deleteIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(OK, response.getStatusCode());
        assertFalse(newIdentityLinks.stream()
                            .anyMatch(a -> a.getGroupId() == groupId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));
    }




    @Test
    public void testAddAndDeleteUserIdentityLink() throws IOException {
        processInstance = util.mySetUp(acquisti);
        //le funzionalità può essere acceduta solo con privileggi "ADMIN"
        util.loginAdmin();

        //test-junit come UTENTE "VISUALIZZATORE"
        String groupId = null;
        String userId = "test-junit";
        List<IdentityLink> identityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertFalse(identityLinks.stream()
                            .anyMatch(a -> a.getUserId() == userId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));

        ResponseEntity<Void> response = flowsProcessInstanceResource.setIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        assertEquals(OK, response.getStatusCode());
        List<IdentityLink> newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(newIdentityLinks.size(), identityLinks.size() + 1);

        assertTrue(newIdentityLinks.stream()
                           .anyMatch(a -> a.getUserId() == userId &&
                                   a.getProcessInstanceId().equals(processInstance.getId())  &&
                                   a.getType() == Utils.PROCESS_VISUALIZER));

        //cancellazione IdentityLink aggiunto prima
        response = flowsProcessInstanceResource.deleteIdentityLink(processInstance.getId(), Utils.PROCESS_VISUALIZER, groupId, userId);

        newIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(processInstance.getId());
        assertEquals(OK, response.getStatusCode());
        assertFalse(newIdentityLinks.stream()
                            .anyMatch(a -> a.getUserId() == userId &&
                                    a.getProcessInstanceId().equals(processInstance.getId())  &&
                                    a.getType() == Utils.PROCESS_VISUALIZER));
    }


    @Test
    public void getProcessInstancesForTrasparenzaTest() throws IOException, ParseException {
        processInstance = util.mySetUp(acquisti);

        util.loginAdmin();
        ResponseEntity<List<Map<String, Object>>> res = flowsProcessInstanceResource
                .getProcessInstancesForTrasparenza(acquisti.getValue(), 2018, 2018, 0, 10, ASC);

        assertEquals(OK, res.getStatusCode());
        //prendo anche le Pi create negli altri test
        assertEquals(8, res.getBody().size());

        //prova recupero 10 elementi dopo il quinto (result = 3 perchè ho 8 Process Instance in totale - anche quelle generate negli altri test)
        res = flowsProcessInstanceResource
                .getProcessInstancesForTrasparenza(acquisti.getValue(), 2018, 2018, 5, 10, ASC);

        //prendo anche le Pi create negli altri test
        assertEquals(OK, res.getStatusCode());
        assertEquals(3, res.getBody().size());


        //prova senza ordinamento (recupera le 8 process instances - anche quelle generate negli altri test)
        res = flowsProcessInstanceResource
                .getProcessInstancesForTrasparenza(acquisti.getValue(), 2018, 2018, 0, 10, null);

        assertEquals(OK, res.getStatusCode());
        //prendo anche le Pi create negli altri test
        assertEquals(8, res.getBody().size());


        //prova anni sbagliati
        res = flowsProcessInstanceResource
                .getProcessInstancesForTrasparenza(acquisti.getValue(), 2016, 2016, 0, 10, ASC);

        assertEquals(OK, res.getStatusCode());
        assertEquals(0, res.getBody().size());
    }


    private String verifyMyProcesses(int startedByRA, int startedBySpaclient) {
        String proceeeInstanceID = null;
        // Admin vede la Process Instance che ha avviato
        Map<String, String> searchParams = new HashMap<>();
        searchParams.put("active", "true");
        searchParams.put("page", "1");
        searchParams.put("processDefinitionKey", ALL_PROCESS_INSTANCES);
        searchParams.put("order", ASC);

        ResponseEntity<Map<String, Object>> response = flowsProcessInstanceResource.getMyProcessInstances(searchParams);
        assertEquals(OK, response.getStatusCode());
        ArrayList<HistoricProcessInstanceResponse> processInstances = (ArrayList<HistoricProcessInstanceResponse>)response.getBody().get("processInstances");
        assertEquals(startedByRA, processInstances.size());
        if (processInstances.size() > 0)
            proceeeInstanceID = processInstances.get(0).getId() ;
//
        // User NON vede la Process Instance avviata da Admin
        util.loginUser();
        response = flowsProcessInstanceResource.getMyProcessInstances(searchParams);
        assertEquals(OK, response.getStatusCode());
        assertEquals(startedBySpaclient, ((ArrayList<HistoricProcessInstanceResponse>)response.getBody().get("processInstances")).size());
        util.loginResponsabileAcquisti();
        return proceeeInstanceID;
    }

}