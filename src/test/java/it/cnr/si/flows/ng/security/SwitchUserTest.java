package it.cnr.si.flows.ng.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import it.cnr.si.FlowsApp;
import static org.assertj.core.api.Assertions.assertThat;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FlowsApp.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public class SwitchUserTest {

    @Value("${server.port}")
    private String port;

    private String SERVER;
    private static final String LOGIN_URL = "/oauth/token";
    private static final String ACCOUNT_URL = "/api/ldap-account";
    private static final String IMPERSONATE_URL = "/login/impersonate";

    @Autowired
    private TestRestTemplate template;


    @Test
    public void testAdminAbleToSwitch() throws URISyntaxException {

        SERVER = "http://localhost:"+ port + "/";

        String token = "Bearer " + loginAs("admin");
        Map<String, Object> account = getAccount(token, null);
        assertThat(account).containsEntry("login", "admin");

        ResponseEntity<Void> impersonateResponse = impersonate(token, "user");
        assertThat(impersonateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(impersonateResponse.getHeaders().get("Set-Cookie")).contains("cnr_impersonate=user;path=/");

        Map<String, Object> impersonatedAccount = getAccount(token, "user");
        assertThat(impersonatedAccount).containsEntry("login", "user");
        assertThat(impersonatedAccount.get("authorities")).asList().contains("ROLE_PREVIOUS_ADMINISTRATOR");

    }

    @Test
    public void testUserUnableToSwitch() throws URISyntaxException {

        SERVER = "http://localhost:"+ port + "/";

        String token = "Bearer " + loginAs("user");
        Map<String, Object> account = getAccount(token, null);
        assertThat(account).containsEntry("login", "user");

        ResponseEntity<Void> impersonateResponse = impersonate(token, "admin");
//        assertThat(impersonateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
//        assertThat(impersonateResponse.getHeaders().get("Set-Cookie")).doesNotContain("cnr_impersonate=user;path=/");

        Map<String, Object> impersonatedAccount = getAccount(token, "admin");
        assertThat(impersonatedAccount).doesNotContainEntry("login", "admin");
        assertThat(impersonatedAccount.get("authorities")).asList().doesNotContain("ROLE_PREVIOUS_ADMINISTRATOR");
    }

    private ResponseEntity<Void> impersonate(String token, String user) throws URISyntaxException {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, new URI(SERVER + IMPERSONATE_URL + "?impersonate_username="+ user));

        ResponseEntity<Void> exchange = template.exchange( request , Void.class );
        return exchange;
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> getAccount(String token, String asUser) throws URISyntaxException {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        if (asUser != null && !asUser.equals(""))
            headers.add("Cookie", "cnr_impersonate="+ asUser);

        RequestEntity request = new RequestEntity(headers, HttpMethod.GET, new URI(SERVER + ACCOUNT_URL));

        Map<String, Object> response = new HashMap<>();
        ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>)
                template.exchange( request , response.getClass() );

        return responseEntity.getBody();
    }

    @SuppressWarnings("unchecked")
    private String loginAs(String user) throws URISyntaxException {

        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("client_id", "sprintapp");
        request.add("client_secret", "my-secret-token-to-change-in-production");
        request.add("grant_type", "password");
        request.add("scope", "read write");
        request.add("username", user);
        request.add("password", user);

        Map<String, String> response = new HashMap<>();
        ResponseEntity<Map<String, String>> responseEntity = (ResponseEntity<Map<String, String>>)
                template.postForEntity( LOGIN_URL, request , response.getClass() );

        return responseEntity.getBody().get("access_token");

    }

}
