package it.cnr.si.flows.ng.service;

import feign.FeignException;
import it.cnr.si.FlowsApp;
import it.cnr.si.service.dto.anagrafica.letture.GerarchiaWebDto;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,unittests,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
@Ignore // TODO per ora testiamo solo Showcase, funzionalita' CNR da testare separatamente
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

    @Test(expected = FeignException.class)
    public void testGetRuoliUtente() {

        Set<String> aceGroupsForUser = aceBridgeService.getAceRolesForUser("marcinireneusz.trycz");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceRolesForUser("app.abil");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceRolesForUser("susanna.monti");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceRolesForUser("luciana.baldoni");
        log.info("{}", aceGroupsForUser);
        aceGroupsForUser = aceBridgeService.getAceRolesForUser("");
        log.info("{}", aceGroupsForUser);
    }

    @Test
    public void testGetParent() {

        List<GerarchiaWebDto> parents = aceBridgeService.getParents(34421);
        parents = parents.stream().filter(p -> p.getTipo().getId() == 2).collect(Collectors.toList());

        Assert.assertEquals(Integer.valueOf(2216), parents.get(0).getPadre().getId());

    }

    @Test
    public void testCacheExtendedGroupName() {
        Instant startSlow = Instant.now();
        aceBridgeService.getExtendedGroupNome("sfd@2216");
        Instant endedSlow = Instant.now();
        Long fetchDuration = Duration.between(startSlow, endedSlow).toMillis();
        log.info("Slow fetch duration: {}", fetchDuration);

        Instant startCached = Instant.now();
        aceBridgeService.getExtendedGroupNome("sfd@2216");
        Instant ended = Instant.now();
        Long cacheDuration = Duration.between(startCached, ended).toMillis();
        log.info("Cache hit duration: {}", cacheDuration);

        Assert.assertTrue("Cache did not work, second request was not significantly slower",
                (fetchDuration / cacheDuration) > 10 );

    }

}
