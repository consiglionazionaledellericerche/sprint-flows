package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,unittests,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
@Ignore // TODO per ora testiamo solo Showcase, funzionalita' CNR da testare separatamente
public class SiperServiceTest {

    @Inject
    private SiperService siperService;

    @Test
    public void testGetResponsabileUo() {

        String responsabile = (String) siperService.getResponsabileCDSUO("ASR108").get(0).get("uid");

        assertEquals("maurizio.lancia", responsabile);

    }
}
