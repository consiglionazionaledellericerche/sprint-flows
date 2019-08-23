package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import java.util.List;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class AceBridgeServiceTest {

    private final Logger log = LoggerFactory.getLogger(AceBridgeServiceTest.class);


    @Inject
    private AceBridgeService aceBridgeService;

    @Test
    public void testAfferenza() {

        String afferenzaUtente = aceBridgeService.getAfferenzaUtente("marcinireneusz.trycz").getCdsuo();
        Assert.assertEquals("000411", afferenzaUtente);

    }


    @Test
    public void testEntitaOrganizzativaUtente() {

        Integer idEo = aceBridgeService.getEntitaOrganizzativaDellUtente("marcinireneusz.trycz").getId();
        Assert.assertEquals(Integer.valueOf(2216), idEo);

    }

    @Test
    public void testGetRuoliUtente() {

        List<String> aceGroupsForUser = aceBridgeService.getAceGroupsForUser("marcinireneusz.trycz");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceGroupsForUser("app.abil");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceGroupsForUser("susanna.monti");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceGroupsForUser("luciana.baldoni");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceGroupsForUser("");
        log.info("{}", aceGroupsForUser);
    }
}
