package it.cnr.si.flows.ng;

import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResource;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

/**
 * Created by cirone on 15/06/17.
 */
@Service
public class TestServices {


    public static final String TITOLO_DELL_ISTANZA_DEL_FLUSSO = "titolo dell'istanza del flusso JUnit";
    public static final String JUNIT_TEST = " JUnit test";


    @Inject
    @Lazy
    private RepositoryService repositoryService;
    @Inject
    private TaskService taskService;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private FlowsTaskResource flowsTaskResource;
    @Inject
    private ProcessInstanceResource processInstanceResource;
    private String firstTaskId;
    private String processDefinition;


    public void loginAdmin() {
        login("admin", "admin");
    }

    public void loginSpaclient() {
        login("spaclient", "sp@si@n0");
    }

    public void loginUser() {
        login("user", "user");
    }

    public void loginSfd() {
        login("supportofunzionidirigenziali", "supportofunzionidirigenziali");
    }

    public void loginSfd2() {
        login("supportofunzionidirigenziali2", "supportofunzionidirigenziali2");
    }

    public void loginResponsabileAcquisti() {
        login("responsabileacquisti", "responsabileacquisti");
    }

    public void loginDirettore() {
        login("direttore", "direttore");
    }


    private void login(String user, String psw) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, psw));
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    public ProcessInstanceResponse mySetUp(String processDefinitionKey) {
        loginAdmin();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().latestVersion().list();

        for (ProcessDefinition pd : processDefinitions) {
            if (pd.getId().contains(processDefinitionKey)) {
                processDefinition = pd.getId();
                break;
            }
        }
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("processDefinitionId", processDefinition);
        if (processDefinitionKey.equals("acquisti-trasparenza")) {
            req.setParameter("titoloIstanzaFlusso", TITOLO_DELL_ISTANZA_DEL_FLUSSO);
            req.setParameter("descrizioneAcquisizione", "descrizione");
            req.setParameter("tipologiaAcquisizioneI", "procedura aperta");
            req.setParameter("tipologiaAcquisizioneId", "11");
            req.setParameter("strumentoAcquisizione", "AFFIDAMENTO DIRETTO - MEPA o CONSIP\n");
            req.setParameter("strumentoAcquisizioneId", "11");
            req.setParameter("priorita", "Alta");
        }
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());
        // Recupero il TaskId del primo task del flusso
        firstTaskId = taskService.createTaskQuery().singleResult().getId();
        //Recupero la ProcessInstance
        return (ProcessInstanceResponse) response.getBody();
    }


    public void myTearDown() {
        //cancello le Process Instance creata all'inizio del test'
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().list();
        HttpServletResponse res = new MockHttpServletResponse();
        for (ProcessInstance pi : list) {
            processInstanceResource.deleteProcessInstance(pi.getProcessInstanceId(), "TEST", res);
            assertEquals(NO_CONTENT.value(), res.getStatus());
        }
        logout();
    }

    public String getProcessDefinition() {
        return processDefinition;
    }

    public String getFirstTaskId() {
        return firstTaskId;
    }
}