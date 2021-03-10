package it.cnr.si.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import it.cnr.si.domain.ExternalMessage;
import it.cnr.si.domain.enumeration.ExternalApplication;
import it.cnr.si.domain.enumeration.ExternalMessageStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;

@EnableScheduling
@Profile("cnr")
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
	@Value("${cnr.stm.url}")
	private String stmUrl;
	@Value("${cnr.stm.username}")
	private String stmUsername;
	@Value("${cnr.stm.password}")
	private String stmPassword;
	@Value("${cnr.stm.loginPath}")
	private String stmLoginPath;
	@Value("${cnr.missioni.url}")
	private String missioniUrl;
	@Value("${cnr.missioni.username}")
	private String missioniUsername;
	@Value("${cnr.missioni.password}")
	private String missioniPassword;
	@Value("${cnr.missioni.loginPath}")
	private String missioniLoginPath;
	@Value("${cnr.missioni.client_id}")
	private String missioniClientId;
	@Value("${cnr.missioni.client_secret}")
	private String missioniClientSecret;
	@Value("${cnr.sigla.url}")
	private String siglaUrl;
	@Value("${cnr.sigla.usr}")
	private String siglaUsername;
	@Value("${cnr.sigla.psw}")
	private String siglaPassword;



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

		// STM

		RestTemplate stmTemplate = new RestTemplate();
		interceptors = stmTemplate.getInterceptors();
		interceptors.add(new StmRequestInterceptor());
		stmTemplate.setInterceptors(interceptors);
		ExternalApplication.STM.setTemplate(stmTemplate);

		// MISSIONI

		RestTemplate missioniTemplate = new RestTemplate();
		interceptors = missioniTemplate.getInterceptors();
		interceptors.add(new MissioniRequestInterceptor());
		missioniTemplate.setInterceptors(interceptors);
		ExternalApplication.MISSIONI.setTemplate(missioniTemplate);

		// MISSIONI

		RestTemplate siglaTemplate = new RestTemplate();
		interceptors = siglaTemplate.getInterceptors();
		interceptors.add(new SiglaRequestInterceptor());
		siglaTemplate.setInterceptors(interceptors);
		ExternalApplication.SIGLA.setTemplate(siglaTemplate);

		// GENERIC

		ExternalApplication.GENERIC.setTemplate(new RestTemplate());

	}

	public void sendMessages() {
		log.info("Processo le rest ExternalMessage");
		externalMessageService.getNewExternalMessages().forEach(this::send);
	}

	public void sendErrorMessages() {
		log.info("Processo le rest ExternalMessage in errore");
		externalMessageService.getFailedExternalMessages().forEach(this::send);
	}

	/* friendly */ void send(ExternalMessage msg) {
		// TODO refactor : il metodo send dovrebbe sendare, non sendare-e-salvare

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

			if ((response.getStatusCode() != HttpStatus.OK) && (response.getStatusCode() != HttpStatus.CREATED))
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


	/* ---------------- REST TEMPLATES ---------------- */

	private class AbilRequestInterceptor implements ClientHttpRequestInterceptor {

		private String id_token = null;

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			request.getHeaders().set("Authorization", "Bearer "+ id_token);
			request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
			ClientHttpResponse response = execution.execute(request, body);

			if ( response.getStatusCode() == HttpStatus.FORBIDDEN || response.getStatusCode() == HttpStatus.UNAUTHORIZED) {

				Map<String, String> auth = new HashMap<>();
				auth.put("username", abilUsername);
				auth.put("password", abilPassword);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);

				RequestEntity entity = new RequestEntity(
						auth,
						headers,
						HttpMethod.POST,
						URI.create(abilUrl + abilLoginPath));

				ResponseEntity<Map> resp = new RestTemplate().exchange(entity, Map.class);

				this.id_token = (String) resp.getBody().get("id_token");

				request.getHeaders().set("Authorization", "Bearer "+ id_token);
				request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				response = execution.execute(request, body);
			}

			return response;
		}
	}

	private class StmRequestInterceptor implements ClientHttpRequestInterceptor {

		private String id_token = null;

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			request.getHeaders().set("Authorization", "Bearer "+ id_token);
			request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
			ClientHttpResponse response = execution.execute(request, body);

			if ( response.getStatusCode() == HttpStatus.FORBIDDEN || response.getStatusCode() == HttpStatus.UNAUTHORIZED) {

				Map<String, String> auth = new HashMap<>();
				auth.put("username", stmUsername);
				auth.put("password", stmPassword);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);

				RequestEntity entity = new RequestEntity(
						auth,
						headers,
						HttpMethod.POST,
						URI.create(stmUrl + stmLoginPath));

				ResponseEntity<Map> resp = new RestTemplate().exchange(entity, Map.class);

				this.id_token = (String) resp.getBody().get("id_token");

				request.getHeaders().set("Authorization", "Bearer "+ id_token);
				request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				response = execution.execute(request, body);
			}

			return response;
		}
	}

	/**
	 * Missioni, per la login, usa /oauth/token e una richiesta POST com FORM_DATA
	 * Per questo ho delle peculiarita': devo usare una MultiValueMap
	 */
	private class MissioniRequestInterceptor implements ClientHttpRequestInterceptor {

		private String access_token = null;

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			request.getHeaders().set("Authorization", "Bearer "+ access_token);
			request.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
			ObjectMapper om = new ObjectMapper();
			String stringRepresentation = new String(body, "UTF-8");
			JsonNode jsonRepresentation = om.readTree(stringRepresentation);
			byte[] byteRepresentation = jsonRepresentation.toString().getBytes(StandardCharsets.UTF_8);

			ClientHttpResponse response = execution.execute(request, byteRepresentation);


			if ( response.getStatusCode() == HttpStatus.FORBIDDEN || response.getStatusCode() == HttpStatus.UNAUTHORIZED) {

				//                MultiValueMap<String, String> auth = new LinkedMultiValueMap<>();

				Map<String, String> auth = new HashMap<>();
				auth.put("username", missioniUsername);
				auth.put("password", missioniPassword);
				auth.put("rememberMe", "true");

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);

				RequestEntity entity = new RequestEntity(
						auth,
						headers,
						HttpMethod.POST,
						URI.create(missioniUrl + missioniLoginPath));
				ResponseEntity<Map> resp = new RestTemplate().exchange(entity, Map.class);
				this.access_token = (String) resp.getBody().get("id_token");
				request.getHeaders().set("Authorization", "Bearer "+ access_token);
				request.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
				response = execution.execute(request, byteRepresentation);
			}

			return response;
		}
	}

	/**
	 * Missioni, per la login, usa /oauth/token e una richiesta POST com FORM_DATA
	 * Per questo ho delle peculiarita': devo usare una MultiValueMap
	 */
	private class SiglaRequestInterceptor implements ClientHttpRequestInterceptor {

		private String access_token = null;

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

			LocalDate dateRif = LocalDate.now();
			String annoEsercizio = String.valueOf(dateRif.getYear());
			String encoding = Base64.getEncoder().encodeToString((siglaUsername + ":" + siglaPassword).getBytes(StandardCharsets.UTF_8));
			request.getHeaders().set("Authorization", "Basic "+ encoding);
			request.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
			request.getHeaders().set("X-sigla-cd-cdr", "999.000.000");
			request.getHeaders().set("X-sigla-cd-cds", "999");
			request.getHeaders().set("X-sigla-cd-unita-organizzativa", "999.000");
			request.getHeaders().set("X-sigla-esercizio", annoEsercizio);
			ObjectMapper om = new ObjectMapper();
			String stringRepresentation = new String(body, "UTF-8");
			JsonNode jsonRepresentation = om.readTree(stringRepresentation);
			byte[] byteRepresentation = jsonRepresentation.toString().getBytes(StandardCharsets.UTF_8);

			ClientHttpResponse response = execution.execute(request, byteRepresentation);
			return response;
		}
	}
}



