package it.cnr.si.flows.ng.service;

import it.cnr.si.FlowsApp;
import it.cnr.si.service.MembershipService;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;


@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
@Ignore
public class RelationshipServiceTest {

    private final Logger log = LoggerFactory.getLogger(RelationshipServiceTest.class);


    @Inject
    private MembershipService membershipService;


}