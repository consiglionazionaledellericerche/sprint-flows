package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.Assert;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import java.net.URISyntaxException;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,unittests,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
@Ignore // TODO TUTTI I TESTS FUNZIONALI e DIPENDENTI DA ACE/SIPER DA SPOSTARE FUORI DA JUNIT
public class CoolFlowsBridgeTest {

    @Inject
    private TestRestTemplate rt;
    @Inject
    private CoolFlowsBridgeService bridge;

    @Test
    public void loginTest() throws URISyntaxException, LoginException {

        Object tasks = bridge.getCoolAvailableAndAssignedTasks("silvia.rossi");
        Assert.notNull(tasks);
        System.out.println(tasks);

    }
}
