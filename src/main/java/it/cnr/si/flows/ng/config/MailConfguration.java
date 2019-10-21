package it.cnr.si.flows.ng.config;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ReplicatedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

@Configuration
public class MailConfguration {

    private static final Logger log = LoggerFactory.getLogger(MailConfguration.class);

    public static final String MAIL_CONFIG = "mailConfig";
    public static final String MAIL_ACTIVATED = "mailActivated";
    public static final String MAIL_RECIPIENTS = "mailRecipients";
    public static final String MAIL_URL = "mailUrl";


    @Value("${spring.mail.recipients:}")
    private List<String> initMailRecipients;
    @Value("${spring.mail.activated:false}")
    private boolean initMailActivated;
    @Value("${spring.mail.url}")
    private String url;

    @Inject
    private HazelcastInstance hazelcastInstance;

    @Scheduled(fixedDelay = 3000, initialDelay = 10000) // 10m
    public void logStatus() {
        log.debug("MailConfig: {} {}", isMailActivated(), getMailRecipients());
    }


    @PostConstruct
    public void init() {
        ReplicatedMap<Object, Object> mailConfig = hazelcastInstance.getReplicatedMap(MAIL_CONFIG);

        if (!mailConfig.containsKey(MAIL_ACTIVATED)) {
            log.info("MailConfig non settato, imposto i predefiniti: {} {}", initMailActivated, initMailRecipients);

            mailConfig.put(MAIL_ACTIVATED, initMailActivated);
            mailConfig.put(MAIL_RECIPIENTS, initMailRecipients);
            mailConfig.put(MAIL_URL, url);

        } else {
            log.info("MailConfig gia' settato, leggo le impostazioni predefiniti: {} {}", mailConfig.get(MAIL_ACTIVATED), MAIL_RECIPIENTS);
        }

        logStatus();
    }

    public List<String> getMailRecipients() {
        return (List<String>) hazelcastInstance.getReplicatedMap(MAIL_CONFIG).get(MAIL_RECIPIENTS);
    }
    public void setMailRecipients(List<String> mailRecipients) {
        hazelcastInstance.getReplicatedMap(MAIL_CONFIG).put(MAIL_RECIPIENTS, mailRecipients);
        logStatus();
    }

    public boolean isMailActivated() {
        return (boolean) hazelcastInstance.getReplicatedMap(MAIL_CONFIG).get(MAIL_ACTIVATED);
    }
    public void setMailActivated(boolean mailActivated) {
        hazelcastInstance.getReplicatedMap(MAIL_CONFIG).put(MAIL_ACTIVATED, mailActivated);
        logStatus();
    }

    public String getMailUrl() {
        return String.valueOf(hazelcastInstance.getReplicatedMap(MAIL_CONFIG).get(MAIL_URL));
    }
    // --- //

    private ITemplateResolver htmlTemplateResolver() {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(2);
        templateResolver.setPrefix("/mails/");
//        templateResolver.setResolvablePatterns(Collections.singleton("html/*"));
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
//        templateResolver.setCharacterEncoding(EMAIL_TEMPLATE_ENCODING);
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine(){
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(htmlTemplateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }
}
