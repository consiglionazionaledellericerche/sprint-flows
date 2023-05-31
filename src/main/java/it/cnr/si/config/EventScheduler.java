package it.cnr.si.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;

import it.cnr.si.flows.ng.service.FlowsMailService;
import it.cnr.si.service.ExternalMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;

@Profile("cnr & scheduler")
@EnableScheduling
@Configuration
public class EventScheduler {

    private final Logger log = LoggerFactory.getLogger(EventScheduler.class);

    @Inject
    private HazelcastInstance hazelcastInstance;
    @Inject
    private ExternalMessageSender externalMessageSender;
    @Inject 
    private FlowsMailService flowsMailService;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000) // 1m
    public void scheduledSendMessages() {

        // Soltanto un nodo dovrebbe effettuare l'invio degli ExternalMessage
        // Verifico che il nodo corrente sia il master del cluster
        // prendendo il primo dei member e confrontando se e' il member corrente
        // https://github.com/hazelcast/hazelcast/issues/3760#issuecomment-57928166
        log.info("Numero di nodi in questo cluster: "+ hazelcastInstance.getCluster().getMembers().size());
        if (isMaster()) {
            log.info("Sono il master, processo le rest ExternalMessage");
            externalMessageSender.sendMessages();
        } else {
            log.info("Non sono il master, non processo le rest ExternalMessage");
        }
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000) // 6h
    public void scheduledSendErrorMessages() {

        Member master = hazelcastInstance.getCluster().getMembers().iterator().next();
        if (master == hazelcastInstance.getCluster().getLocalMember()) {
            externalMessageSender.sendErrorMessages();
        } else {
            log.debug("Non sono il master, non processo le rest ExternalMessage in errore");
        }
    }
    
    @Scheduled(cron = "0 0 7 * * MON-FRI")
    public void scheduleEmailNotifications() {
        
        if (isMaster()) {
            log.info("Invio notifiche ricorrenti"+ ZonedDateTime.now());
            flowsMailService.sendScheduledNotifications();
        } else {
            log.debug("Non sono il master, non invio le notifiche ricorrenti");
        }
    }

    private boolean isMaster() {
        Optional<String> masterId = hazelcastInstance.getCluster().getMembers().stream()
                .map(Member::getUuid).sorted().findFirst();
        return masterId.get().equals(hazelcastInstance.getCluster().getLocalMember().getUuid());
    }
}
