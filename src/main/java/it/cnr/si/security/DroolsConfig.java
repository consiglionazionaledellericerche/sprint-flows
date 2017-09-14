package it.cnr.si.security;

import java.io.IOException;
import java.io.InputStreamReader;

import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.common.io.CharStreams;

/**
 * Configurazione delle drools.
 * Si occupa di caricare tutti i file di drools in src/main/resources e rendere disponibili 
 * i containter per le drools.
 *  
 * @author cristian
 * @author daniele
 */
@Configuration
public class DroolsConfig {

    private Resource[] listRules() throws IOException {
        PathMatchingResourcePatternResolver pmrs = new PathMatchingResourcePatternResolver();
        Resource[] resources = pmrs.getResources("classpath*:*.drl");
        return resources;
    }

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        Resource[] files = listRules();

        for(Resource file : files) {

        	String data = CharStreams.toString( new InputStreamReader( file.getInputStream() ));
        	//TODO: verificare se prendere tutte le drools o solo quelle in src/main/resources
            kfs.write("src/main/resources/"+ file.getFilename(), data);
        }

        // kieModule is automatically deployed to KieRepository if successfully built.
        ks.newKieBuilder(kfs).buildAll();
        return ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
    }

    @Bean
    public KieBase kieBase() throws IOException {
        KieBase kieBase = kieContainer().getKieBase();
        return kieBase;
    }
}
