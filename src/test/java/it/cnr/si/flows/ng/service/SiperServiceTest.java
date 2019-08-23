package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "test,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class SiperServiceTest {

    @Inject
    private SiperService siperService;

    @Test
    public void testGetResponsabileUo() {

        System.out.println(siperService.getResponsabileCDSUO("000400").get(0).get("codice_sede"));


    }
}
