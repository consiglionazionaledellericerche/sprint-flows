package it.cnr.si.flows.ng.resource;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;

import static it.cnr.si.flows.ng.TestServices.JUNIT_TEST;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.iscrizioneElencoOiv;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;


//todo: va in errore con l'interazione con http://cool-jconon-funzione-pubblica.test.si.cnr.it (forse non trova la priocess instance avviata sul bd in memoria durante il test)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test,oiv")
public class OivSummaryPdfResouceTest {

    @Inject
    private FlowsPdfService flowsPdfService;
    @Inject
    private TestServices util;
    @Inject
    private FlowsTaskResource flowsTaskResource;
    @Inject
    private FlowsAttachmentResource flowsAttachmentResource;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private TaskService taskService;
    private ProcessInstanceResponse processInstance;

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
    public void testSummaryPdfProcessCompleted() throws IOException {
        processInstance = util.mySetUp(iscrizioneElencoOiv);
        //todo: da fare quando l'ambiente sarà più stabile
    }

    @Test
    public void testSummaryPdfService() throws IOException, ParseException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        processInstance = util.mySetUp(iscrizioneElencoOiv);

        util.loginIstruttore();
        completeTask();

        flowsPdfService.makeSummaryPdf(processInstance.getId(), outputStream);
        assertTrue(outputStream.size() > 0);
        util.makeFile(outputStream, "summaryCreato.pdf");


    }

    private void completeTask() throws IOException {
        //completo il primo task
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", taskService.createTaskQuery().singleResult().getId());
        req.setParameter("processDefinitionId", processInstance.getProcessDefinitionId());
        req.setParameter("sceltaUtente", "prendo_in_carico_la_domanda");
        req.setParameter("commento", "commento di presa in carico della domanda " + JUNIT_TEST);

        assertEquals(OK, flowsTaskResource.completeTask(req).getStatusCode());
    }
}
