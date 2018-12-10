package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.dto.FlowsAttachment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FlowsAttachmentServiceTest {

    @Inject
    private FlowsAttachmentService attService;

    @Test
    public void addProtocollo() {
    }

    @Test
    public void mergeProtocolli1() {

        String nuovoProtocollo = "nuovoProtocollo";
        String vecchiProtocolli = "Vecchio::vecchio2";

        String result = attService.addProtocollo(vecchiProtocolli, nuovoProtocollo);
        Assert.assertEquals("", "Vecchio::vecchio2::nuovoProtocollo", result);


        String result2 = attService.addProtocollo(result, nuovoProtocollo);
        Assert.assertEquals("", "Vecchio::vecchio2::nuovoProtocollo", result2);


        String result3 = attService.addProtocollo(result, "vecchio2");
        Assert.assertEquals("", "Vecchio::vecchio2::nuovoProtocollo", result3);


        String result4 = attService.addProtocollo(null, nuovoProtocollo);
        Assert.assertEquals("", "nuovoProtocollo", result4);

        String result5 = attService.addProtocollo(result, null);
        Assert.assertEquals("", "Vecchio::vecchio2::nuovoProtocollo", result5);

        String result6 = attService.addProtocollo(null, null);
        Assert.assertEquals("", "", result6);

    }
}