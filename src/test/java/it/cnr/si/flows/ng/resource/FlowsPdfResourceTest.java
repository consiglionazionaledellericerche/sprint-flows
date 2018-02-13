package it.cnr.si.flows.ng.resource;

import it.cnr.jada.comp.ComponentException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static it.cnr.si.flows.ng.utils.Enum.PdfType.rigetto;
import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.iscrizioneElencoOiv;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "oiv")
public class FlowsPdfResourceTest {

    @Inject
    private FlowsPdfResource flowsPdfResource;
    @Inject
    private TestServices util;
    private ProcessInstanceResponse processInstance;
    private HttpServletRequest mockRequest;

    @Before
    public void setUp() throws IOException {
        // setto correttamente il request contexHolder
        mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);

        //avvio un'istanza del flusso iscrizione-elenco-oiv
        processInstance = util.mySetUp(iscrizioneElencoOiv);
    }

    @After
    public void tearDown() {
        util.myTearDown();
    }


    @Test
    public void testMakePdf() throws IOException, ComponentException {
        util.loginApp();
        ResponseEntity<byte[]> response = flowsPdfResource.makePdf(processInstance.getId(),
                                                                   rigetto.getValue());
        //verifico che il file creato sia un pdf non vuoto e che abbia il nome giusto
        HttpHeaders headers = response.getHeaders();
        String titolo = headers.get("Content-Disposition").get(0).split("\"")[1];
        assertEquals("rigetto-utenteRichiedente.pdf", titolo);
        assertEquals(MediaType.APPLICATION_PDF, headers.getContentType());
        assertTrue(response.getBody().length > 0);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(response.getBody().length);
        outputStream.write(response.getBody());
        util.makeFile(outputStream, titolo);
    }
}