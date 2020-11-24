package it.cnr.si.flows.ng.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import it.cnr.si.FlowsApp;

//@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles(profiles = "native,showcase,unittests")
//@EnableTransactionManagement
@RunWith(JUnit4ClassRunner.class)
public class UtilsTest {

    @Test
    public void isFullPathTest() {
        assertTrue(Utils.isFullPath("/Comunicazioni al CNR/Missioni/ASR.108/Ordini di Missione/Anno 2020/000000015/OrdineMissione30000024.pdf"));
        assertFalse(Utils.isFullPath("/Comunicazioni al CNR/flows-demo/acquisti/000411/2019/Acquisti-2019-1"));
    }
}
