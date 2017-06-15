package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import it.cnr.si.flows.ng.resource.FlowsAttachmentResource;
import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import it.cnr.si.service.SummaryPdfService;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.Before;
import org.junit.Ignore;
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
    private ProcessInstanceResponse processInstance;

    @Before
    public void setUp() {
        HttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @Test
    @Ignore
    public void testSummaryPdfProcessCompleted() {
//        todo: da fare
        processInstance = util.mySetUp("acquisti-trasparenza");

        Map<String, FlowsAttachment> docs = flowsAttachmentResource.getAttachementsForProcessInstance(processInstance.getId()).getBody();
        assertTrue(docs.isEmpty());

        //completo un task
        completeFirstTask();
        //annullo il flusso


        //verifico che ci sia il summary.pdf tra i documenti del flusso
        docs = flowsAttachmentResource.getAttachementsForProcessInstance(processInstance.getId()).getBody();
        assertFalse(docs.isEmpty());
//        assertTrue(docs.get(""));

    }

    @Test
    public void testSummaryPdfService() throws IOException, ParseException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        processInstance = util.mySetUp("acquisti-trasparenza");
        completeFirstTask();

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

    private void completeFirstTask() {
        //completo il primo task
        util.loginSfd();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", util.getFirstTaskId());
        assertEquals(OK, flowsTaskResource.completeTask(req).getStatusCode());
    }

}
