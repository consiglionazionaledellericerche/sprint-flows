package it.cnr.si.flows.ng.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

public class ActivitiBeanNameGenerator extends AnnotationBeanNameGenerator {
    
    public ActivitiBeanNameGenerator() {
        super();
    }
    
    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        return "activiti"+super.generateBeanName(definition, registry);
    }
}
