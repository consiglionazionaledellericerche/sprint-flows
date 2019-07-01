package it.cnr.si.config;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import it.cnr.si.service.ExternalMessageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
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
public class ExternalMessageSender {

    private final Logger log = LoggerFactory.getLogger(ExternalMessageSender.class);

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
    @Inject
    private HazelcastInstance hazelcastInstance;

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

        // Soltanto un nodo dovrebbe effettuare l'invio degli ExternalMessage
        // Verifico che il nodo corrente sia il master del cluster
        // prendendo il primo dei member e confrontando se e' il member corrente
        // https://github.com/hazelcast/hazelcast/issues/3760#issuecomment-57928166
        Member master = hazelcastInstance.getCluster().getMembers().iterator().next();
        if (master == hazelcastInstance.getCluster().getLocalMember()) {
            sendMessagesDo();
        } else {
            log.debug("Non sono il master, non processo le rest ExternalMessage");
        }
    }

    public void sendMessagesDo() {
        log.info("Processo le rest ExternalMessage");
        externalMessageService.getNewExternalMessages().forEach(this::send);
    }

    @Scheduled(fixedDelay = 21600000, initialDelay = 60000) // 6h
    public void sendErrorMessages() {

        Member master = hazelcastInstance.getCluster().getMembers().iterator().next();
        if (master == hazelcastInstance.getCluster().getLocalMember()) {
            sendErrorMessagesDo();
        } else {
            log.debug("Non sono il master, non processo le rest ExternalMessage in errore");
        }
    }

    public void sendErrorMessagesDo() {
        log.info("Processo le rest ExternalMessage in errore");
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
            log.info("Rest eseguita con successo {} ", msg);

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



