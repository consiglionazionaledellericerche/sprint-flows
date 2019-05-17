package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableTransactionManagement
@ActiveProfiles("test,cnr")
public class AceBridgeServiceTest {

    @Inject
    private AceBridgeService aceBridgeService;

    @Test
    public void testAfferenza() {

        String afferenzaUtente = aceBridgeService.getAfferenzaUtente("marcinireneusz.trycz").getCdsuo();
        Assert.assertEquals("000411", afferenzaUtente);

    }


    @Test
    public void testEntitaOrganizzativaUtente() {

        Integer idEo = aceBridgeService.getIdEntitaOrganizzativaDellUtente("marcinireneusz.trycz").getId();
        Assert.assertEquals(Integer.valueOf(2216), idEo);

    }
}
