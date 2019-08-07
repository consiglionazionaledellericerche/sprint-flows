package it.cnr.si.flows.ng.config;

import it.cnr.si.config.CacheConfiguration;
import it.cnr.si.flows.ng.service.AceBridgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Configuration
@EnableScheduling
public class CachingConfig {

    private static DateFormat formatoDataOra = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY);
    private final Logger log = LoggerFactory.getLogger(CachingConfig.class);

    @Inject
    private AceBridgeService aceBridgeService;
    @Inject
    private CacheConfiguration cacheConfiguration;

    //    todo: evict anche delle altre cache?
    //metodo di rimozione della cache sulle liste dinamiche recuperate da signa (SCHEDULATO OGNI 24 H)
    @CacheEvict(value = "siglaDynamicList", allEntries = true)
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000, initialDelay = 500)
    public void siglaCacheEvict() {
        log.info("Flush Cache \"siglaDynamicList\" - " + formatoDataOra.format(new Date()));
    }

//    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
//    public void tryCache() {
//        log.info("chiamo getIdRuoloBySigla con sigla staffAmministrativo");
//
//        aceBridgeService.getIdRuoloBySigla("staffAmministrativo");
//    }
}
