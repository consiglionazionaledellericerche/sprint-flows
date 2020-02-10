package it.cnr.si.flows.ng.resource.oiv;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.resource.FlowsAttachmentResource;
import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import it.cnr.si.flows.ng.service.FlowsPdfService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;

import static it.cnr.si.flows.ng.TestServices.JUNIT_TEST;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.iscrizioneElencoOiv;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles(profiles = "native,unittests,oiv")
@ActiveProfiles(profiles = "oiv,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
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
    public void testSummaryPdfService() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        processInstance = util.mySetUp(iscrizioneElencoOiv);

        util.loginIstruttore();
        completeTask();

        flowsPdfService.makeSummaryPdf(processInstance.getId(), outputStream);
        assertTrue(outputStream.size() > 0);
        util.makeFile(outputStream, "summaryCreato.pdf");


    }

    private void completeTask() throws Exception {
        //completo il primo task
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("taskId", util.getFirstTaskId());
        req.setParameter("processDefinitionId", processInstance.getProcessDefinitionId());
        req.setParameter("sceltaUtente", "prendo_in_carico_la_domanda");
        req.setParameter("commento", "commento di presa in carico della domanda " + JUNIT_TEST);

        assertEquals(OK, flowsTaskResource.completeTask(req).getStatusCode());
    }
}
