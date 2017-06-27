package it.cnr.si.flows.ng.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class MailConfguration {

    @Value("${spring.mail.recipients}")
    private List<String> mailRecipients;
    @Value("${spring.mail.activated}")
    private boolean mailActivated;

    public List<String> getMailRecipients() {
        return mailRecipients;
    }
    public void setMailRecipients(List<String> mailRecipients) {
        this.mailRecipients = mailRecipients;
    }

    public boolean isMailActivated() {
        return mailActivated;
    }
    public void setMailActivated(boolean mailActivated) {
        this.mailActivated = mailActivated;
    }

    // --- //

    private ITemplateResolver htmlTemplateResolver() {
        final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(2));
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
