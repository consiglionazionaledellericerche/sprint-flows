package it.cnr.si.flows.ng.service;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import it.cnr.si.domain.Counter;
import it.cnr.si.repository.CounterRepository;

/**
 * Service Implementation for managing Counter.
 */
@Service
public class CounterService {

    private final Logger log = LoggerFactory.getLogger(CounterService.class);

    @Inject
    private CounterRepository counterRepository;
    @Inject
    private PlatformTransactionManager transactionManager;
    @Inject
    private HazelcastInstance hazelcastInstance;

    /**
     *  Get all the counters.
     *
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<Counter> findAll() {
        log.debug("Request to get all Counters");
        List<Counter> result = counterRepository.findAll();
        return result;
    }

    //   NON @Transactional!!!
    // La transazione va gestita a mano perche' non gioca bene col synchronized
    // (In sostanza ottiene la transazione prima del synchronized, e alcune transazioni gia' iniziate si ritrovano dati obsoleti)
    public long getNext(String name) {

        ILock lock = hazelcastInstance.getLock("counter-"+ name);
        lock.lock();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            Counter c = counterRepository.findByName(name);
            if(c == null) {
                c = new Counter();
                c.setName(name);
                c.setValue(0L);
            }
            c.setValue(c.getValue()+1);
            c = counterRepository.saveAndFlush(c);
            transactionManager.commit(status);
            return c.getValue();

        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        } finally {
            try {
                lock.unlock();
            } catch(IllegalMonitorStateException ex) {
                /* noaction */
            }
        }
    }

}
