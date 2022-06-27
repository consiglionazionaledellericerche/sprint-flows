package it.cnr.si.flows.ng.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Service
public class JavaMailSender extends JavaMailSenderImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaMailSender.class);

    @Value("cnr.mail.mail.host")
    private String mailHost;
    @Value("cnr.mail.mail.port")
    private String mailPort;
    @Value("cnr.mail.mail.protocol")
    private String mailProtocol;
    @Value("cnr.mail.mail.user")
    private String mailUser;
    @Value("cnr.mail.mail.user")
    private String mailPassword;
    @Value("cnr.mail.mail.send.error.to")
    private String mailSendErrorTo;
    @Value("cnr.mail.mail.from")
    private String mailFrom;

    @PostConstruct
    public void initMailProperties() {

        setHost(mailHost);
        
        try {
            Integer port = Integer.parseInt(mailPort);
            setPort(port);
        } catch (Exception e) {}
        
        setProtocol(mailProtocol);
        setUsername(mailUser);
        setPassword(mailPassword);

        Properties props = new Properties();
        props.setProperty("mail.send.error.to", mailSendErrorTo);
        props.setProperty("mail.from", mailFrom);
        
        setJavaMailProperties(props);

        LOGGER.info("Setting java mail properties {} {} {} {} {} {}",
            mailHost,
            mailPort,
            mailProtocol,
            mailUser,
            "****",
            props);
    }
}
