package it.cnr.si.flows.ng.service;

import it.cnr.si.config.CoolFlowsRestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.security.auth.login.LoginException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile("cnr")
public class CoolFlowsBridgeService {

    private static final String ASSIGNED_URL = "https://scrivaniadigitale.cnr.it/rest/taskinstances/mytasks?maxItems=10000&skipCount=0&where=(status=active+AND+assignee+=+'{username}')";
    private static final String CANDIDATE_URL ="https://scrivaniadigitale.cnr.it/rest/taskinstances/mytasks?maxItems=10000&skipCount=0&where=(status=active+AND+candidateUser+=+'{username}')";

    @Inject
    private CoolFlowsRestConfiguration.CoolRestTemplate coolRestTemplate;


    public List<Map> getCoolAvailableAndAssignedTasks(String username) {

        try {

            Map assignedTasks = getAssignedTasks(username);
            Map availableTasks = getAvailableTasks(username);

            List<Map> tasks = new ArrayList<>();

            tasks = (List<Map>) ((Map)assignedTasks.get("list")).get("entries");
            tasks.addAll((List<Map>) ((Map)availableTasks.get("list")).get("entries"));

            return tasks;

        } catch (LoginException e) {

            return new ArrayList<>(); //defaultvalue TODO
        }

    }

    private Map getAssignedTasks(String username) throws LoginException {

        ResponseEntity<Map> response = coolRestTemplate.loginAndExchange(ASSIGNED_URL, Map.class, username);

        if (response.getStatusCode() != HttpStatus.OK)
            throw new LoginException("" + response.getStatusCode() + response.getBody());

        return response.getBody();
    }

    private Map getAvailableTasks(String username) throws LoginException {

        ResponseEntity<Map> response = coolRestTemplate.loginAndExchange(CANDIDATE_URL, Map.class, username);

        if (response.getStatusCode() != HttpStatus.OK)
            throw new LoginException("" + response.getStatusCode() + response.getBody());

        return response.getBody();
    }
}
