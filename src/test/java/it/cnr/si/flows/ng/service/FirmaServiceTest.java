package it.cnr.si.flows.ng.service;

import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.si.FlowsApp;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FirmaServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmaServiceTest.class);

    @Autowired
    private FlowsFirmaService firmaService;
    
    @Ignore // TODO non e' chiaro se lasciare le credenziali di test su git
    @Test
    public void testFirma() throws IOException {
        

        String username = "";
        String password = "";
        String otp = "";
        String textMessage = "";

        byte[] bytes = Files.readAllBytes(Paths.get("./src/test/resources/pdf-test/summaryCreato.pdf"));
        
        try {
            byte[] bytesfirmati = firmaService.firma(username, password, otp, bytes);
            Files.write(Paths.get("./src/test/resources/pdf-test/summaryFirmato.pdf"), bytesfirmati);
            
        } catch (ArubaSignServiceException e) {
            LOGGER.error("firma non riuscita", e);
            if (e.getMessage().indexOf("error code 0001") != -1) {
                textMessage = "controlla il formato del file sottopsto alla firma";
            } else if(e.getMessage().indexOf("error code 0003") != -1) {
                textMessage = "Errore in fase di verifica delle credenziali";
            } else if(e.getMessage().indexOf("error code 0004") != -1) {
                textMessage = "Errore nel PIN";
            } else {
                textMessage = "errore generico";
            }
            LOGGER.error("500", "firma non riuscita - " + textMessage);
            fail();
        }
        
    }

}
