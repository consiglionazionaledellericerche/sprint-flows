package it.cnr.si.flows.ng.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

@Service
public class JavaMailSender extends JavaMailSenderImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaMailSender.class);
    public static final String MAIL_PORT = "mail.port";
    public static final String MAIL_USER = "mail.user";
    public static final String MAIL_PASSWORD = "mail.password";

    @Inject
    private Environment env;

    @PostConstruct
    public void initMailProperties() {
        Properties javaMailProperties = new Properties();
        Arrays.asList(env.getActiveProfiles()).stream()
                .map(s -> new RelaxedPropertyResolver(env, s.concat(".")).getSubProperties("mail."))
                .forEach(stringObjectMap -> javaMailProperties.putAll(stringObjectMap));
        LOGGER.info("Mail configuration with properties: {}", javaMailProperties);
        setPort(
                Optional.ofNullable(javaMailProperties.get(MAIL_PORT))
                    .filter(Integer.class::isInstance)
                    .map(Integer.class::cast)
                    .orElse(null)
        );
        setUsername(javaMailProperties.getProperty(MAIL_USER));
        setPassword(javaMailProperties.getProperty(MAIL_PASSWORD));
        setJavaMailProperties(javaMailProperties);
    }
}
