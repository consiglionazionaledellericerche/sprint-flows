package it.cnr.si.config;

import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.service.ExternalMessageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EnableScheduling
@Configuration
public class ExtenalMessageSender {

    private final Logger log = LoggerFactory.getLogger(ExtenalMessageSender.class);

    @Value("${cnr.abil.url}")
    private String abilUrl;
    @Value("${cnr.abil.username}")
    private String abilUsername;
    @Value("${cnr.abil.password}")
    private String abilPassword;
    @Value("${cnr.abil.loginPath}")
    private String abilLoginPath;


    @Inject
    private ExternalMessageService externalMessageService;


    @PostConstruct
    public void init() {

        // ABIL

        RestTemplate abilTemplate = new RestTemplate();
        List<ClientHttpRequestInterceptor> interceptors = abilTemplate.getInterceptors();
        interceptors.add(new AbilRequestInterceptor());
        abilTemplate.setInterceptors(interceptors);

        ExternalApplication.ABIL.setTemplate(abilTemplate);

        // GENERIC

        ExternalApplication.GENERIC.setTemplate(new RestTemplate());

    }

    @Scheduled(fixedDelay = 600000, initialDelay = 10000) // 10m
    public void sendMessages() {
        sendMessagesDo();
    }

    public void sendMessagesDo() {
        log.debug("Processo le rest ExternalMessage");
        externalMessageService.getNewExternalMessages().forEach(this::send);
    }

    @Scheduled(fixedDelay = 21600000, initialDelay = 60000) // 6h
    public void sendErrorMessages() {
        sendErrorMessagesDo();
    }

    public void sendErrorMessagesDo() {
        log.debug("Processo le rest ExternalMessage in errore");
        externalMessageService.getFailedExternalMessages().forEach(this::send);
    }

    private void send(ExternalMessage msg) {

        log.debug("Tentativo della rest {}", msg);

        ResponseEntity<String> response = null;
        try {

            RestTemplate template = msg.getApplication().getTemplate();

             response = template.exchange(
                    msg.getUrl(),
                    msg.getVerb().value(),
                    new HttpEntity<>(msg.getPayload()),
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK)
                throw new Exception();

            msg.setStatus(ExternalMessageStatus.SENT);
            msg.setLastErrorMessage(StringUtils.substring(response.getBody(), 0, 254));
            externalMessageService.save(msg);
            log.debug("Rest eseguita con successo {} ", msg);

        } catch (Exception e) {

            String responseMessage;
            if (response == null)
                responseMessage = e.getMessage();
            else if (response.getBody() == null)
                responseMessage = String.valueOf(response.getStatusCodeValue());
            else
                responseMessage = response.getBody();

            log.error("Rest fallita con messaggio {} {} ", responseMessage, msg, e);

            msg.setStatus(ExternalMessageStatus.ERROR);
            msg.setRetries(msg.getRetries() + 1);
            msg.setLastErrorMessage(StringUtils.substring(responseMessage, 0, 254));
            externalMessageService.save(msg);
        }
    }


    private class AbilRequestInterceptor implements ClientHttpRequestInterceptor {

        private String id_token = null;

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

            request.getHeaders().set("Authorization", "Bearer "+ id_token);
            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            ClientHttpResponse response = execution.execute(request, body);

            if ( response.getStatusCode() == HttpStatus.FORBIDDEN || response.getStatusCode() == HttpStatus.UNAUTHORIZED) {

                Map<String, String> auth = new HashMap<>();
                auth.put("username", abilUsername);
                auth.put("password", abilPassword);
                MultiValueMap<String, String> headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

                RequestEntity entity = new RequestEntity(
                        auth,
                        headers,
                        HttpMethod.POST,
                        URI.create(abilUrl + abilLoginPath));

                ResponseEntity<Map> resp = new RestTemplate().exchange(entity, Map.class);

                this.id_token = (String) resp.getBody().get("id_token");

                request.getHeaders().set("Authorization", "Bearer "+ id_token);
                request.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                response = execution.execute(request, body);
            }

            return response;
        }
    }
}



