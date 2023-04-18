package it.cnr.si.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cnr.si.domain.ExternalProblem;
import it.cnr.si.flows.ng.utils.FileMessageResource;
import it.cnr.si.flows.ng.utils.proxy.ResultProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class ProxyService implements EnvironmentAware{
    private final Logger log = LoggerFactory.getLogger(ProxyService.class);

    private Environment env;

    private Map<String, RestTemplate> restTemplateMap;

    public ResultProxy process(HttpMethod httpMethod, ExternalProblem jsonBody, String app, String url, String queryString, String authorization){
        log.debug("REST request from app ", app);
        String body = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            body = mapper.writeValueAsString(jsonBody);
        } catch (Exception ex) {
            throw new RuntimeException("Errore nella manipolazione del file JSON per la preparazione del body della richiesta REST.", ex);
        }
        return process(httpMethod, body, app, url, queryString, authorization);
    }

    public ResultProxy process(HttpMethod httpMethod, String body, String app, String url, String queryString, String authorization) {
        HttpHeaders headers = impostaAutenticazione(app, authorization);
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

        String proxyURL = impostaUrl(app, url, queryString);
        HttpEntity<String> requestEntity = new HttpEntity<String>(body, headers);
        log.info("Url: "+proxyURL);
        log.info("Header: "+headers);
        log.info("Body: "+body);    /**
     * The filename to be associated with the {@link MimeMessage} in the form data.
     */
        try {
            ResponseEntity<String> result = getRestTemplate(app).
                    exchange(proxyURL, httpMethod, requestEntity, String.class);
            ResultProxy resultProxy = new ResultProxy();
            resultProxy.setBody(result.getBody());
            resultProxy.setType(result.getHeaders().getContentType().getType());
            resultProxy.setStatus(result.getStatusCode());
            log.debug("Response for url : " + proxyURL, resultProxy);
            return resultProxy;
        } catch (HttpClientErrorException _ex) {
            String errResponse = _ex.getResponseBodyAsString();
            if (_ex.getRawStatusCode() == 404){
                ResultProxy res = new ResultProxy();
                res.setStatus(HttpStatus.OK);
                res.setBody("");
                return res;
            }
            log.error(_ex.getMessage(), _ex.getResponseBodyAsString());
            throw new ApplicationContextException(errResponse,_ex);
        } catch (HttpServerErrorException _ex) {
            String errResponse = _ex.getResponseBodyAsString();
            log.error(_ex.getMessage(), _ex.getResponseBodyAsString());
            if (_ex.getStatusCode().compareTo(HttpStatus.SERVICE_UNAVAILABLE) == 0){
                throw new ApplicationContextException(app+" temporaneamente non disponibile");
            }
            throw new ApplicationContextException(errResponse,_ex);
        } catch (Exception _ex) {
            log.error(_ex.getMessage(), _ex.getLocalizedMessage());
            throw new ApplicationContextException("Servizio REST "+ proxyURL+" Eccezione: "+ _ex.getLocalizedMessage(),_ex);
        }
    }

    public ResultProxy processWithFile(HttpMethod httpMethod, String body, String app, String url, String queryString, String authorization, MultipartFile uploadedMultipartFile) throws IOException {
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add(ExternalProblem.ALLEGATO_STRING, new FileMessageResource(uploadedMultipartFile.getBytes(), uploadedMultipartFile.getOriginalFilename()));
        HttpHeaders headers = impostaAutenticazione(app, authorization);

        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

        String proxyURL = impostaUrl(app, url, queryString);

        log.info("Url: "+proxyURL);
        log.info("Header: "+headers);
        log.info("Body: "+body);
        try {
            ResponseEntity<String> result = getRestTemplate(app).
                    exchange(proxyURL, httpMethod, requestEntity, String.class);
            ResultProxy resultProxy = new ResultProxy();
            resultProxy.setBody(result.getBody());
            resultProxy.setStatus(result.getStatusCode());
            log.debug("Response for url : " + proxyURL, resultProxy);
            return resultProxy;
        } catch (HttpClientErrorException _ex) {
            String errResponse = _ex.getResponseBodyAsString();
            log.error(_ex.getMessage(), _ex.getResponseBodyAsString());
            throw new ApplicationContextException(errResponse,_ex);
        } catch (HttpServerErrorException _ex) {
            String errResponse = _ex.getResponseBodyAsString();
            log.error(_ex.getMessage(), _ex.getResponseBodyAsString());
            throw new ApplicationContextException(errResponse,_ex);
        } catch (Exception _ex) {
            log.error(_ex.getMessage(), _ex.getLocalizedMessage());
            throw new ApplicationContextException("Servizio REST "+ proxyURL+" Eccezione: "+ _ex.getLocalizedMessage(),_ex);
        }
    }

    public String impostaUrl(String app, String url, String queryString) {
        String appUrl = env.getProperty("spring.proxy."+ app + ".url");
        String proxyURL = null;
        if (appUrl == null) {
            log.error("Cannot find properties for app: " + app + " Current profile are: ", Arrays.toString(env.getActiveProfiles()));
            throw new ApplicationContextException("Cannot find properties for app: " + app);
        }
        log.debug("proxy url is: " + appUrl);
        proxyURL = appUrl.concat(url);
        if (queryString != null){
            String valueToDelete = "proxyURL="+url;
            int numberCharacter = valueToDelete.length();
            String newValue = queryString;
            if (queryString.startsWith(valueToDelete)){
                newValue = queryString.substring(numberCharacter);
            }
            proxyURL = proxyURL.concat("?").concat(newValue);
        }
        return proxyURL;
    }

    public HttpHeaders impostaAutenticazione(String app, String authorization) {
        HttpHeaders headers = new HttpHeaders();
        String username = env.getProperty("spring.proxy."+ app + ".username"),
                password = env.getProperty("spring.proxy."+ app + ".password");
        if (username != null && password != null) {
            String plainCreds = username.concat(":").concat(password);
            byte[] plainCredsBytes = plainCreds.getBytes();
            byte[] base64CredsBytes = Base64.encode(plainCredsBytes);
            String base64Creds = new String(base64CredsBytes);
            headers.add("Authorization", "Basic " + base64Creds);
        } else {
            headers.add("Authorization", authorization);
        }
        return headers;
    }


    private RestTemplate getRestTemplate(String app) {
        if (!restTemplateMap.containsKey(app))
            restTemplateMap.put(app, new RestTemplate());
        return restTemplateMap.get(app);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
        this.restTemplateMap = new HashMap<String, RestTemplate>();
    }
}