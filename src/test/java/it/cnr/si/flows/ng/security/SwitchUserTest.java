package it.cnr.si.flows.ng.security;

import it.cnr.si.FlowsApp;
import it.cnr.si.flows.ng.config.SwitchUserSecurityConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,showcase,unittests")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class SwitchUserTest {

    private String SERVER;
    private static final String LOGIN_URL = "/oauth/token";
    private static final String ACCOUNT_URL = "/api/ldap-account";
    private static final String IMPERSONATE_URL = SwitchUserSecurityConfiguration.IMPERSONATE_START_URL;
    private static final String EXIT_IMPERSONATE_URL = SwitchUserSecurityConfiguration.IMPERSONATE_EXIT_URL;

    @LocalServerPort
    private String port;
    @Autowired
    private TestRestTemplate template;


    @Before
    public void setUp() {}


    @Test
    public void testAdminAbleToSwitchToDatabaseUser() throws URISyntaxException {

        SERVER = "http://localhost:"+ port;

        String token = "Bearer " + loginAs("admin");
        Map<String, Object> account = getAccount(token, null);
        assertThat(account).containsEntry("login", "admin");

        ResponseEntity<Void> impersonateResponse = impersonate(token, "user");
        assertThat(impersonateResponse.getStatusCode())
        .isEqualTo(HttpStatus.OK);
        assertThat(impersonateResponse.getHeaders().get("Set-Cookie")).contains("cnr_impersonate=user;path=/");

        Map<String, Object> impersonatedAccount = getAccount(token, "user");
        assertThat(impersonatedAccount).containsEntry("login", "user");
        assertThat(impersonatedAccount.get("authorities")).asList().contains("ROLE_PREVIOUS_ADMINISTRATOR");

        ResponseEntity<Void> exitImpersonateResponse = exitImpersonate(token, "user");
        assertThat(exitImpersonateResponse.getStatusCode())
        .isEqualTo(HttpStatus.OK);
        assertThat(exitImpersonateResponse.getHeaders().get("Set-Cookie")).contains("cnr_impersonate=;Max-Age=0;path=/");

        Map<String, Object> exitAccount = getAccount(token);
        assertThat(exitAccount).containsEntry("login", "admin");
        assertThat(exitAccount.get("authorities")).asList().doesNotContain("ROLE_PREVIOUS_ADMINISTRATOR");
    }


    @Test
    // TODO refactor per concentrarsi sulla logica e esternalizzare il boilerpalte
    public void testAdminAbleToSwitchToLdapUser() throws URISyntaxException {

        SERVER = "http://localhost:"+ port + "/";

        String token = "Bearer " + loginAs("admin");
        Map<String, Object> account = getAccount(token, null);
        assertThat(account).containsEntry("login", "admin");

        ResponseEntity<Void> impersonateResponse = impersonate(token, "marcinireneusz.trycz");
        assertThat(impersonateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(impersonateResponse.getHeaders().get("Set-Cookie")).contains("cnr_impersonate=marcinireneusz.trycz;path=/");

        Map<String, Object> impersonatedAccount = getAccount(token, "marcinireneusz.trycz");
        assertThat(impersonatedAccount).containsEntry("login", "marcinireneusz.trycz");
        assertThat(impersonatedAccount.get("authorities")).asList()
        .contains("ROLE_PREVIOUS_ADMINISTRATOR")
        .contains("DEPARTMENT_641100");

        Map<String, Object> exitAccount = getAccount(token);
        assertThat(exitAccount).containsEntry("login", "admin");
        assertThat(exitAccount.get("authorities")).asList().doesNotContain("ROLE_PREVIOUS_ADMINISTRATOR");
    }

    @Test
    public void testDatabaseUserUnableToSwitch() throws URISyntaxException {

        SERVER = "http://localhost:"+ port + "/";

        String token = "Bearer " + loginAs("user");
        Map<String, Object> account = getAccount(token, null);
        assertThat(account).containsEntry("login", "user");

        ResponseEntity<Void> impersonateResponse = impersonate(token, "admin");
        assertThat(impersonateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(impersonateResponse.getHeaders().get("Set-Cookie")).doesNotContain("cnr_impersonate=admin;path=/");

        Map<String, Object> impersonatedAccount = getAccount(token, "admin");
        assertThat(impersonateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(impersonatedAccount).doesNotContainEntry("login", "admin");
        assertThat(impersonatedAccount.get("authorities")).isNull();
    }

    @Test
    public void testLdapUserUnableToSwitch() throws URISyntaxException {

        SERVER = "http://localhost:"+ port + "/";

        String token = "Bearer " + loginAs("user");
        Map<String, Object> account = getAccount(token, null);
        assertThat(account).containsEntry("login", "user");

        ResponseEntity<Void> impersonateResponse = impersonate(token, "admin");
        assertThat(impersonateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(impersonateResponse.getHeaders().get("Set-Cookie")).doesNotContain("cnr_impersonate=admin;path=/");

        Map<String, Object> impersonatedAccount = getAccount(token, "admin");
        assertThat(impersonateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(impersonatedAccount).doesNotContainEntry("login", "admin");
        assertThat(impersonatedAccount.get("authorities")).isNull();
    }

    private ResponseEntity<Void> impersonate(String token, String user) throws URISyntaxException {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, new URI(SERVER + IMPERSONATE_URL + "?impersonate_username="+ user));

        ResponseEntity<Void> exchange = template.exchange( request , Void.class );
        return exchange;
    }


    private Map<String, Object> getAccount(String token)  throws URISyntaxException {
        return getAccount(token, null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAccount(String token, String asUser) throws URISyntaxException {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        if (asUser != null && !asUser.equals(""))
            headers.add("Cookie", "cnr_impersonate="+ asUser);

        RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, new URI(SERVER + ACCOUNT_URL));

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
                template.postForEntity( SERVER + LOGIN_URL, request , response.getClass() );

        return responseEntity.getBody().get("access_token");

    }


    private ResponseEntity<Void> exitImpersonate(String token, String asUser) throws URISyntaxException {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", token);
        if (asUser != null && !asUser.equals(""))
            headers.add("Cookie", "cnr_impersonate="+ asUser);
        RequestEntity<Void> request = new RequestEntity<>(headers, HttpMethod.GET, new URI(SERVER + EXIT_IMPERSONATE_URL));

        ResponseEntity<Void> exchange = template.exchange( request , Void.class );
        return exchange;
    }

}
