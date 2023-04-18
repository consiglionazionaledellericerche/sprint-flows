package it.cnr.si.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

/**
 * @author paoloenricocirone
 * "Registra" nel registro dei bean la mia definizione customizzata del bean "liquibase"
 * che "punta" a FlowsDatabaseConfiguration.liquibase() invece di DatabaseConfiguration.liquibase()
 *
 * (prende un diverso master a seconda del profilo spring con il quale si avvia l'applicazione)
 */
@Component
public class ServiceRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        GenericBeanDefinition gbd = new GenericBeanDefinition();

        gbd.setScope("singleton");
        gbd.setAbstract(false);
        gbd.setLazyInit(false);
        gbd.setAutowireMode(3);
        gbd.setDependencyCheck(0);
        gbd.setAutowireCandidate(true);
        gbd.setPrimary(false);
        gbd.setFactoryBeanName("databaseConfiguration");
        gbd.setFactoryMethodName("liquibase");
        gbd.setInitMethodName(null);
        gbd.setDestroyMethodName("(inferred)");

        registry.registerBeanDefinition("liquibase", gbd );
    }


    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        //TODO: metodo vuoto perchè presente nell'interfaccia BeanDefinitionRegistryPostProcessor ma non mi serve per iniettare il singleton di liquibase
    }
}