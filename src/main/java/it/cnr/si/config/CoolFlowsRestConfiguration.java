package it.cnr.si.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Profile("cnr")
@Configuration
public class CoolFlowsRestConfiguration {

    @Value("${cnr.coolflows.username}")
    private String username;
    @Value("${cnr.coolflows.password}")
    private String password;

    @Bean(name = "coolRestTemplate")
    public CoolRestTemplate coolRestTemplate() {
        return new CoolRestTemplate();
    }

    public class CoolRestTemplate extends RestTemplate {

        @Deprecated
        private String ticket;

        @PostConstruct
        public void init() {
            try {
                login();
            } catch (LoginException e) {}
        }

        @Deprecated
        private void login() throws LoginException {
            RequestEntity<Map> loginRequest = null;

            try {
                loginRequest = RequestEntity
                        .post(new URI("https://scrivaniadigitale.cnr.it/rest/security/login"))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new HashMap<String, String>() {{
                            put("username", username);
                            put("password", password);
                        }});

                ResponseEntity<Map> response = super.exchange(loginRequest, Map.class);

                if (response.getStatusCode() != HttpStatus.OK)
                    throw new LoginException("" + response.getStatusCode() + response.getBody());

                this.ticket = (String) response.getBody().get("ticket");
            } catch (URISyntaxException e) {};
        }

        public <T> ResponseEntity<T> loginAndExchange(String url, Class<T> responseType, String username)
                throws RestClientException {

            try {
                login();
                ResponseEntity<T> response = doTheExchange(url, responseType, username);

                return response;

            } catch (URISyntaxException | LoginException e) {
                throw new RuntimeException(e);
            }
        }

        private <T> ResponseEntity<T> doTheExchange(String url, Class<T> responseType, String username) throws URISyntaxException {
            RequestEntity<Void> payloadRequest = RequestEntity
                    .get(new URI(url.replace("{username}", username)))
                    .header("cookie", String.valueOf("ticket=" + ticket))
                    .accept(MediaType.APPLICATION_JSON)
                    .build();

            return super.exchange(payloadRequest, responseType);
        }
    }
}
