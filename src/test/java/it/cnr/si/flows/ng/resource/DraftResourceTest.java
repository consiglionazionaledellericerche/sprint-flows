package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Draft;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.web.rest.DraftResource;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static it.cnr.si.flows.ng.TestServices.JUNIT_TEST;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpStatus.OK;

@Ignore // TODO TUTTI I TESTS FUNZIONALI DA SPOSTARE FUORI DA JUNIT
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class DraftResourceTest {

    @Inject
    private FlowsTaskResource flowsTaskResource;
    @Inject
    private DraftResource draftResource;
    @Inject
    private TestServices util;
    private ProcessInstanceResponse processInstance;
    private String json = "{\"commento_qualitaProgetto\":\"commento 1\",\"commento_qualitaGruppoDiRicerca\":\"commento 2\",\"commento_pianoDiLavoro\":\"commento 3\",\"commento_valoreAggiunto\":\"commento 4\",\"commento\":\"commento 5\",\"punteggio_qualitaProgetto\":1,\"punteggio_qualitaGruppoDiRicerca\":1.95,\"punteggio_pianoDiLavoro\":3,\"punteggio_valoreAggiunto\":4}";





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
    public void testDeleteDraft(){
        ResponseEntity<Draft> responseDirettore = draftResource.updateDraft(Long.valueOf(util.getFirstTaskId()), json);
        assertEquals(OK, responseDirettore.getStatusCode());
        assertNotNull(responseDirettore.getBody().getId());

        ResponseEntity<List<Draft>> allDraftsRE = draftResource.getAllDrafts();
        assertEquals(OK, allDraftsRE.getStatusCode());
        assertEquals(1, allDraftsRE.getBody().size());

        // vado avanti col flusso (Pre-determina -> Verifica))
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", util.getFirstTaskId());
        req.setParameter("processDefinitionId", processInstance.getProcessDefinitionId());
        req.setParameter("sceltaUtente", "Approva");
        req.setParameter("commento", "commento approvazione" + JUNIT_TEST);        util.loginSfd();
        ResponseEntity<ProcessInstanceResponse> response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());

        //testo che il completamento del task comporti la rimozione del draft
        allDraftsRE = draftResource.getAllDrafts();
        assertEquals(OK, allDraftsRE.getStatusCode());
        assertEquals(0, allDraftsRE.getBody().size());
    }



    @Test
    public void testDraft() throws Exception {
        processInstance = util.mySetUp(acquisti);

        util.loginDirettore();
        ResponseEntity<Draft> responseDirettore = draftResource.updateDraft(Long.valueOf(util.getFirstTaskId()), json);
        assertEquals(OK, responseDirettore.getStatusCode());
        assertNotNull(responseDirettore.getBody().getId());


        ResponseEntity<List<Draft>> allDraftsRE = draftResource.getAllDrafts();
        assertEquals(1, allDraftsRE.getBody().size());
        Draft expectedDraft = allDraftsRE.getBody().get(0);
        assertEquals(json, expectedDraft.getJson());
        //nel primo Draft lo username è null
        assertEquals(null, expectedDraft.getUsername());
        assertEquals(Long.valueOf(util.getFirstTaskId()), expectedDraft.getTaskId());
        assertEquals(json, expectedDraft.getJson());
        assertEquals(json, allDraftsRE.getBody().get(1).getJson());
        assertEquals("maurizio.lancia", allDraftsRE.getBody().get(1).getUsername());
        assertEquals(Long.valueOf(util.getFirstTaskId()), allDraftsRE.getBody().get(1).getTaskId());

        //Testo getDraftById
        ResponseEntity<Draft> draftRE = draftResource.getDraftById(expectedDraft.getId());
        assertEquals(OK, draftRE.getStatusCode());
        assertEquals(expectedDraft.getUsername(), draftRE.getBody().getUsername());

        //Testo getdraftByTaskId (con username paoloenricocirone)
        util.loginDirettore();
        ResponseEntity<Draft> draftsUserRE = draftResource.getDraftByTaskId(Long.valueOf(util.getFirstTaskId()));
        assertEquals(OK, draftsUserRE.getStatusCode());
        assertEquals("maurizio.lancia", draftsUserRE.getBody().getUsername());
    }
}