package it.cnr.si.flows.ng;

import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import it.cnr.si.security.FlowsUserDetailsService;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static it.cnr.si.flows.ng.utils.Enum.SiglaList.TIPOLOGIA_ACQUISIZIONE;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.descrizione;
import static it.cnr.si.flows.ng.utils.Enum.VariableEnum.oggetto;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

/**
 * Created by cirone on 15/06/17.
 */
@Service
public class TestServices {


    public static final String TITOLO_DELL_ISTANZA_DEL_FLUSSO = "titolo dell'istanza del flusso JUnit";
    public static final String JUNIT_TEST = " JUnit test";
    //    private static final String SFD = "supportofunzionidirigenziali";
    private static final String SFD = "roberto.puccinelli";
    //    private static final String RA = "responsabileacquisti";
    private static final String DIRETTORE = "direttore";
    private static final String RA = "anna.penna";
    private static final String RA2 = "silvia.rossi";


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
    @Inject
    FlowsUserDetailsService flowsUserDetailsService;
    private String processDefinition;
    private String firstTaskId;


    public static String getRA() {
        return RA;
    }

    public static String getRA2() {
        return RA2;
    }

    public void loginAdmin() {
        logout();
        login("admin", "admin");
    }

    public void loginSpaclient() {
        logout();
        login("spaclient", "");
    }

    public void loginUser() {
        logout();
        login("user", "user");
    }

    public void loginSfd() {
        logout();
        login(SFD, SFD);
    }

    public void loginResponsabileAcquisti() {
        logout();
        login(TestServices.RA, "");
    }

    public void loginResponsabileAcquisti2() {
        logout();
        login(TestServices.RA2, "");
    }

    public void loginDirettore() {
        login(DIRETTORE, DIRETTORE);
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    private void login(String user, String psw) {
//      prendo le Authorities dell'utente che si sta loggando per settarle nel "context"
        Collection<GrantedAuthority> authorities = new ArrayList<>(flowsUserDetailsService.loadUserByUsername(user).getAuthorities());

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, psw, authorities));
    }


    public int myTearDown() {
        int processDeleted = 0;
        //cancello le Process Instance creata all'inizio del test'
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().list();
        HttpServletResponse res = new MockHttpServletResponse();
        for (ProcessInstance pi : list) {
            processInstanceResource.deleteProcessInstance(pi.getProcessInstanceId(), "TEST", res);
            assertEquals(NO_CONTENT.value(), res.getStatus());
            processDeleted++;
        }
        logout();
        return processDeleted;
    }

    public String getProcessDefinition() {
        return processDefinition;
    }

    public String getFirstTaskId() {
        return firstTaskId;
    }

    public ProcessInstanceResponse mySetUp(String processDefinitionKey) throws IOException {
        loginResponsabileAcquisti();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().latestVersion().list();

        for (ProcessDefinition pd : processDefinitions) {
            if (pd.getId().contains(processDefinitionKey)) {
                processDefinition = pd.getId();
                break;
            }
        }

        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();
        req.setParameter("processDefinitionId", processDefinition);
        if (processDefinitionKey.equals(acquisti.getValue())) {
            req.setParameter(oggetto.name(), TITOLO_DELL_ISTANZA_DEL_FLUSSO);
            req.setParameter(descrizione.name(), "descrizione");
            req.setParameter(TIPOLOGIA_ACQUISIZIONE.name(), "procedura aperta");
            req.setParameter("tipologiaAcquisizioneId", "11");
            req.setParameter("strumentoAcquisizione", "AFFIDAMENTO DIRETTO - MEPA o CONSIP\n");
            req.setParameter("strumentoAcquisizioneId", "11");
            req.setParameter("priorita", "Alta");
            req.setParameter("rup", "marco.spasiano");
            req.setParameter("rup_label", "MARCO SPASIANO (marco.spasiano)");
            req.setParameter("impegni_json", "[{\"numero\":\"1\",\"importoNetto\":100,\"importoLordo\":120,\"descrizione\":\"descrizione impegno\",\"vocedispesa\":\"11001 - Arretrati per anni precedenti corrisposti al personale a tempo indeterminato\",\"vocedispesaid\":\"11001\",\"gae\":\"spaclient\"}]");
        }
        ResponseEntity<Object> response = flowsTaskResource.completeTask(req);
        assertEquals(OK, response.getStatusCode());
        // Recupero il TaskId del primo task del flusso
//        firstTaskId = taskService.createTaskQuery().singleResult().getId();
        firstTaskId = taskService.createTaskQuery().list().get(0).getId();
        //Recupero la ProcessInstance
        return (ProcessInstanceResponse) response.getBody();
    }

    public File makeFile(ByteArrayOutputStream outputStream, String title) throws IOException {
        //metto il contenuto dell'outputStream in un summary fisico'
        File pdf = new File("./src/test/resources/summary-test/" + title);

        FileOutputStream fop = new FileOutputStream(pdf);
        byte[] byteArray = outputStream.toByteArray();
        fop.write(byteArray);
        fop.flush();
        fop.close();
        String content = new String(byteArray);
        assertFalse(content.isEmpty());

        return pdf;
    }
}