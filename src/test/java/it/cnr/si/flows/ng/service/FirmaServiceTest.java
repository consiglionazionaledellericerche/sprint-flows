package it.cnr.si.flows.ng.service;

import it.cnr.jada.firma.arss.ArubaSignServiceException;
import it.cnr.jada.firma.arss.stub.SignReturnV2;
import it.cnr.si.FlowsApp;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.fail;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,unittests,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
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
            LOGGER.error("500 firma non riuscita - " + textMessage);
            fail();
        }
        
    }

    @Test
    @Ignore
    public void testSignMany() {

        String username = "";
        String password = "";
        String otp = "";
        try {

            for (int i = 1; i < 1001; i = i+100) {
                byte[] fileContent = Files.readAllBytes(Paths.get("./src/test/resources/pdf-test/summaryCreato.pdf"));

                List<byte[]> fileContents = new ArrayList<>();
                for (int j = 0; j < i; j++)
                    fileContents.add(fileContent);

                System.out.println("Testo la firma con numero files: "+ i);
                List<SignReturnV2> signResponses = firmaService.firmaMultipla(username, password, otp, fileContents);
                Optional<SignReturnV2> any = signResponses.stream().filter(r -> !r.getStatus().equals("OK")).findAny();
                if ( any.isPresent() ) {
                    System.out.println(any.get());
                    Assert.fail("Una risposta non era OK");
                }
            }
        } catch ( ArubaSignServiceException | IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
