package it.cnr.si.flows.ng.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Properties;

@Service
public class JavaMailSender extends JavaMailSenderImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaMailSender.class);

    private Properties javaMailProperties = new Properties();
    @Inject
    private Environment env;

    @PostConstruct
    public void initMailProperties() {
        Arrays.asList(env.getActiveProfiles()).stream()
                .map(s -> new RelaxedPropertyResolver(env, s.concat(".")).getSubProperties("mail."))
                .forEach(stringObjectMap -> javaMailProperties.putAll(stringObjectMap));
        LOGGER.info("Mail configuration with properties: {}", javaMailProperties);
    }

    @Override
    public Properties getJavaMailProperties() {
        return javaMailProperties;
    }


}
