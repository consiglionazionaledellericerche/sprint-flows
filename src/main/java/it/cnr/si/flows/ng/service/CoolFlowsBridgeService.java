package it.cnr.si.flows.ng.service;

import it.cnr.si.config.CoolFlowsRestConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import java.net.URISyntaxException;
import java.util.Map;

@Service
public class CoolFlowsBridgeService {

    // https://scrivaniadigitale.cnr.it/rest/taskinstances/mytasks?maxItems=10000&skipCount=0&where=(status=active+AND+assignee+=+'marcinireneusz.trycz')
    // https://scrivaniadigitale.cnr.it/rest/taskinstances/mytasks?maxItems=10000&skipCount=0&where=(status=active+AND+candidateUser+=+'marcinireneusz.trycz')
    // https://scrivaniadigitale.cnr.it/rest/taskinstances/mytasks?maxItems=1000&skipCount=0&where=(includeTaskVariables+=+true+AND+includeProcessVariables+=+true+AND+status+=+active+AND+assignee+=+'marcinireneusz.trycz')
    // https://scrivaniadigitale.cnr.it/rest/taskinstances/mytasks?maxItems=1000&skipCount=0&where=(includeTaskVariables+=+true+AND+includeProcessVariables+=+true+AND+status+=+active+AND+assignee+=+'marcinireneusz.trycz')

    @Inject
    private CoolFlowsRestConfiguration.CoolRestTemplate coolRestTemplate;


    public Object getCoolAvailableAndAssignedTasks(String username) {

        try {

            Map tasks = getAvailableAndAssignedTasks(username);

            return tasks;

        } catch (URISyntaxException | LoginException e) {

            return null; //defaultvalue
        }

    }

    private Map getAvailableAndAssignedTasks(String username) throws LoginException, URISyntaxException {

        String url = "https://scrivaniadigitale.cnr.it/rest/taskinstances/mytasks?maxItems=10000&skipCount=0&where=(status=active+AND+candidateUser+=+'"+ username +"')";

        ResponseEntity<Map> response = coolRestTemplate.loginAndExchange(url, Map.class);

        if (response.getStatusCode() != HttpStatus.OK)
            throw new LoginException("" + response.getStatusCode() + response.getBody());

        return response.getBody();

    }
}
