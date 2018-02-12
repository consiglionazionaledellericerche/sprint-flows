package it.cnr.si.flows.ng.service;

import com.google.common.net.MediaType;
import it.cnr.jada.comp.ComponentException;
import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.TestServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

import static it.cnr.si.flows.ng.utils.Enum.PdfType.rigetto;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "oiv")
public class PdfServiceTest {

    @Inject
    private FlowsPdfService flowsPdfService;
    @Inject
    private TestServices util;


    @Test
    public void testMakePdf() throws IOException, ComponentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        flowsPdfService.makePdf(outputStream, rigetto);

        //metto il contenuto dell'outputStream in un summary fisico'
        File pdf = util.makeFile(outputStream, "jreport.pdf");

        //verifico che il file creato sia un pdf non vuoto
//        todo: verificare anche il nome del file
//        assertEquals(pdf.getName(), titoloPdf);
        assertEquals(URLConnection.guessContentTypeFromName(pdf.getName()), MediaType.PDF.toString());
        assertTrue(pdf.getTotalSpace() > 0);
    }
}