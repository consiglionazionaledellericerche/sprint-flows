package it.cnr.si.flows.ng.service;

import com.google.common.net.MediaType;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.resource.FlowsAttachmentResource;
import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import it.cnr.si.service.SummaryPdfService;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import static it.cnr.si.flows.ng.TestServices.JUNIT_TEST;
import static it.cnr.si.flows.ng.utils.Actions.revoca;
import static it.cnr.si.flows.ng.utils.Actions.revocaSemplice;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.OK;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class SummaryPdfServiceTest {

    @Inject
    private SummaryPdfService summaryPdfService;
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
    public void testSummaryPdfProcessCompleted() {
        processInstance = util.mySetUp("acquisti-trasparenza");

        Map<String, FlowsAttachment> docs = flowsAttachmentResource.getAttachementsForProcessInstance(processInstance.getId()).getBody();
        assertTrue(docs.isEmpty());

        //completo il primo task (come sfd)
        util.loginSfd();
        completeTask();
        //accedo come direttore e Revoco
        util.logout();
        util.loginDirettore();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", taskService.createTaskQuery().singleResult().getId());
        req.setParameter("processDefinitionId", processInstance.getProcessDefinitionId());
        req.setParameter("sceltaUtente", revoca.getValue());
        req.setParameter("commentoRevoca", "commento di revoca" + JUNIT_TEST);
        assertEquals(OK, flowsTaskResource.completeTask(req).getStatusCode());
        //acceddo come ra e confermo la revoca semplice
        util.logout();
        util.loginResponsabileAcquisti();
        req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", taskService.createTaskQuery().singleResult().getId());
        req.setParameter("processDefinitionId", processInstance.getProcessDefinitionId());
        req.setParameter("sceltaUtente", revocaSemplice.getValue());
        req.setParameter("commento", "commento di revoca semplice" + JUNIT_TEST);
        assertEquals(OK, flowsTaskResource.completeTask(req).getStatusCode());
        //controllo che non ci siano task attivi
        assertTrue(taskService.createTaskQuery().list().size() == 0);

        //verifico che il summaryPdf sia salvato tra i documenti del flusso (e non sia vuoto)
        docs = flowsAttachmentResource.getAttachementsForProcessInstance(processInstance.getId()).getBody();
        assertFalse(docs.isEmpty());
        String titoloPdf = "Summary_" + processInstance.getBusinessKey() + ".pdf";
        FlowsAttachment summary = docs.get(titoloPdf);
        assertEquals(summary.getFilename(), titoloPdf);
        assertEquals(summary.getName(), titoloPdf);
        assertEquals(summary.getMimetype(), MediaType.PDF.toString());
        assertTrue(summary.getBytes().length > 0);
    }

    @Test
    public void testSummaryPdfService() throws IOException, ParseException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        processInstance = util.mySetUp("acquisti-trasparenza");

        util.loginSfd();
        completeTask();

        summaryPdfService.createPdf(processInstance.getId(), outputStream);
        assertTrue(outputStream.size() > 0);

        //metto il contenuto dell'outputStream in un summary fisico'
        File summary = new File("./src/test/resources/summary-test/summaryCreato.pdf");

        FileOutputStream fop = new FileOutputStream(summary);
        byte[] byteArray = outputStream.toByteArray();
        fop.write(byteArray);
        fop.flush();
        fop.close();
        String content = new String(byteArray);
        assertFalse(content.isEmpty());
    }

    private void completeTask() {
        //completo il primo task
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
//        req.setParameter("taskId", util.getFirstTaskId());
        req.setParameter("taskId", taskService.createTaskQuery().singleResult().getId());
        req.setParameter("processDefinitionId", processInstance.getProcessDefinitionId());
        req.setParameter("sceltaUtente", "Approva");
        req.setParameter("commento", "commento approvazione" + JUNIT_TEST);

        assertEquals(OK, flowsTaskResource.completeTask(req).getStatusCode());
    }
}
