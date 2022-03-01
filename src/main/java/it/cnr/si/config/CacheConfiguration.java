package it.cnr.si.config;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Configuration
@AutoConfigureAfter(value = { MetricsConfiguration.class, DatabaseConfiguration.class })
public class CacheConfiguration {

    private final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    private static HazelcastInstance hazelcastInstance;

    @Inject
    private Environment env;

    private CacheManager cacheManager;

    @Value("#{'${cache.hazelcast.packages}'.split(',')}")
    private List<String> packages;

    @PreDestroy
    public void destroy() {
        log.info("Closing Cache Manager");
        Hazelcast.shutdownAll();
    }

    @Bean
    public CacheManager cacheManager(HazelcastInstance hazelcastInstance) {
        log.debug("Starting HazelcastCacheManager");
        cacheManager = new com.hazelcast.spring.cache.HazelcastCacheManager(hazelcastInstance);
        return cacheManager;
    }

    @Bean
    public HazelcastInstance hazelcastInstance(JHipsterProperties jHipsterProperties) throws UnknownHostException {
        log.debug("Configuring Hazelcast");
        Config config = new Config();

        String mancenter = env.getProperty("cache.hazelcast.mancenter");

        if (mancenter != null) {
            log.info("using mancenter: " + mancenter);
            ManagementCenterConfig mc = new ManagementCenterConfig();
            mc.setEnabled(true);
            mc.setUrl(mancenter);
            config.setManagementCenterConfig(mc);
        } else {
            log.info("no mancenter configured");
        }

        String hazelcastInstanceName = env.getProperty("cache.hazelcast.name", String.class, "sprint");
        Integer hazelcastPort = env.getProperty("cache.hazelcast.port", Integer.class, 5701);
        Integer hazelcastMulticastPort = env.getProperty("cache.hazelcast.multicastPort", Integer.class);
        Integer hazelcastOutboundPort = env.getProperty("cache.hazelcast.outboundPort", Integer.class, 1488);
        Integer hazelcastTimeToLiveSeconds = env.getProperty("cache.hazelcast.timeToLiveSeconds", Integer.class, 3600);
        String members = env.getProperty("cache.hazelcast.members");

        String publicIp = env.getProperty("cache.hazelcast.publicIp");

        NetworkConfig networkConfig = config.getNetworkConfig();
        InterfacesConfig networkInterface = networkConfig.getInterfaces();
        String hostAddress = env.getProperty("cache.hazelcast.localIp", InetAddress.getLocalHost().getHostAddress());
        log.info("Local IP: "+ hostAddress);
        networkInterface.setEnabled(true).addInterface(hostAddress);
        if(publicIp != null)
            networkConfig.setPublicAddress(publicIp);

        config.setInstanceName(hazelcastInstanceName);
        config.getNetworkConfig().setPort(hazelcastPort);
        config.getNetworkConfig().setPortAutoIncrement(false);

        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);

        if (hazelcastOutboundPort != null) {
            log.info("hazelcastOutboundPort: " + hazelcastOutboundPort);
            config.getNetworkConfig().addOutboundPort(hazelcastOutboundPort);
        }

        config.setGroupConfig(new GroupConfig());
        config.getGroupConfig().setName("sprint-flows");
        config.getGroupConfig().setPassword("sprint-flows-pass");

        if (members != null) {
            log.info("TCP members: " + members);
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getTcpIpConfig().setMembers(Arrays.asList(members.split(",")));
        } else if (hazelcastMulticastPort != null) {
            log.info("multicast on port " + hazelcastMulticastPort);
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getMulticastConfig().setMulticastPort(hazelcastMulticastPort);
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        } else {
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        }

        config.getMapConfigs().put("default", initializeDefaultMapConfig());
        config.getMapConfig("default").setTimeToLiveSeconds(hazelcastTimeToLiveSeconds);
        
        jHipsterProperties.getCache().setTimeToLiveSeconds(hazelcastTimeToLiveSeconds);
        packages.stream().forEach(cachePackage -> {
            config.getMapConfigs().put(cachePackage, initializeDomainMapConfig(jHipsterProperties));
            log.info("package {} added to cache configuration", cachePackage);
        });

        hazelcastInstance = HazelcastInstanceFactory.newHazelcastInstance(config);

        return hazelcastInstance;
    }

    private MapConfig initializeDefaultMapConfig() {
        MapConfig mapConfig = new MapConfig();

        /*
            Number of backups. If 1 is set as the backup-count for example,
            then all entries of the map will be copied to another JVM for
            fail-safety. Valid numbers are 0 (no backup), 1, 2, 3.
         */
        mapConfig.setBackupCount(1);

        /*
            Valid values are:
            NONE (no eviction),
            LRU (Least Recently Used),
            LFU (Least Frequently Used).
            NONE is the default.
         */
        mapConfig.setEvictionPolicy(EvictionPolicy.LRU);

        /*
            Maximum size of the map. When max size is reached,
            map is evicted based on the policy defined.
            Any integer between 0 and Integer.MAX_VALUE. 0 means
            Integer.MAX_VALUE. Default is 0.
         */
        mapConfig.setMaxSizeConfig(new MaxSizeConfig(0, MaxSizeConfig.MaxSizePolicy.USED_HEAP_SIZE));

        /*
            When max. size is reached, specified percentage of
            the map will be evicted. Any integer between 0 and 100.
            If 25 is set for example, 25% of the entries will
            get evicted.
         */
        mapConfig.setEvictionPercentage(25);

        return mapConfig;
    }

    private MapConfig initializeDomainMapConfig(JHipsterProperties jHipsterProperties) {
        MapConfig mapConfig = new MapConfig();

        mapConfig.setTimeToLiveSeconds(jHipsterProperties.getCache().getTimeToLiveSeconds());
        return mapConfig;
    }
    

    /**
    * @return the unique instance.
    */
    public static HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }
}
