package it.cnr.si.flows.ng.integration;

import it.cnr.si.FlowsApp;
import it.cnr.si.domain.Form;
import it.cnr.si.flows.ng.TestServices;
import it.cnr.si.flows.ng.resource.FlowsProcessDefinitionResource;
import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import it.cnr.si.web.rest.FormResource;
import it.cnr.si.web.rest.ViewResource;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.TaskService;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static it.cnr.si.flows.ng.utils.Enum.ProcessDefinitionEnum.acquisti;
import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = "native,unittests,cnr")
@EnableTransactionManagement
@RunWith(SpringRunner.class)
public class FormAndViewVersionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormAndViewVersionTest.class);

    @Inject
    private FormResource formResource;
    @Inject
    private ViewResource viewResource;
    @Inject
    private TestServices util;
    @Inject
    private FlowsTaskResource flowsTaskResource;
    @Inject
    private TaskService taskService;
    @Inject
    private FlowsProcessDefinitionResource procDefResource;
    @Inject
    private RepositoryService repositoryService;

    private ProcessInstanceResponse processInstance1;
    private ProcessInstanceResponse processInstance2;
    private ProcessInstanceResponse processInstance3;

    private Map<String, String> forms = new HashMap<>();
    private Map<String, String> views = new HashMap<>();

    @Before
    public void setUp() {
        HttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);
    }

    @Test
    public void testFormAndViewVersions () throws Exception {

        setup();

        // verifico che se chiedo una versione superiore dello start, mi da' l'ultima deployata (in questo momento 1)
        verificaVersione("acquisti", "1", "start", "acquisti-1-start", null);
        verificaVersione("acquisti", "2", "start", "acquisti-1-start", null);
        verificaVersione("acquisti", "3", "start", "acquisti-1-start", null);

        // verifico che la form per il primo flusso abbia la versione 1
        verificaVersione("acquisti", "1", "verifica-decisione", "acquisti-1-verifica-decisione", processInstance1.getId());

        /* ---------------------------------------------------------------------- */

        // Creo una nuova versione del flusso e avvio una seconda process instance
        deployNewProcessVersion("acquisti2");

        processInstance2 = util.mySetUp(acquisti);

        assertEquals("2", processInstance2.getProcessDefinitionId().split(":")[1]);

        // Verifico che la nuova process Instance carichi le form V1 (perche' non ho ancora caricato nuove versioni delle form)
        verificaVersione("acquisti", "2", "start", "acquisti-1-start", null);
        verificaVersione("acquisti", "2", "verifica-decisione", "acquisti-1-verifica-decisione", processInstance2.getId());


        // Carico le nuove form
        deployNewVersionForms("acquisti", "2", "start");
        deployNewVersionForms("acquisti", "2", "verifica-decisione");


        // verifico che la prima versione visualizzi tuttora correttamente
        verificaVersione("acquisti", "1", "start", "acquisti-1-start", null);
        verificaVersione("acquisti", "1", "verifica-decisione", "acquisti-1-verifica-decisione", processInstance1.getId());


        // verifico che la seconda versione visualizzi correttamente le form v2
        verificaVersione("acquisti", "2", "start", "acquisti-2-start", null);
        verificaVersione("acquisti", "2", "verifica-decisione", "acquisti-2-verifica-decisione", processInstance2.getId());


        /* ---------------------------------------------------------------------- */

        // Carico una terza versione della processDefinition e avvio una terza process instance
        deployNewProcessVersion("acquisti3");
        processInstance3 = util.mySetUp(acquisti);
        assertEquals("3", processInstance3.getProcessDefinitionId().split(":")[1]);

        // Verifico che la prima e la seconda versione continuino a funzionare correttamente
        verificaVersione("acquisti", "1", "start", "acquisti-1-start", null);
        verificaVersione("acquisti", "1", "verifica-decisione", "acquisti-1-verifica-decisione", processInstance1.getId());
        verificaVersione("acquisti", "2", "start", "acquisti-2-start", null);
        verificaVersione("acquisti", "2", "verifica-decisione", "acquisti-2-verifica-decisione", processInstance2.getId());

        // Verifico che la terza process Instance carichi le form V2 (perche' non ho ancora caricato nuove versioni delle form)
        verificaVersione("acquisti", "3", "start", "acquisti-2-start", null);
        verificaVersione("acquisti", "3", "verifica-decisione", "acquisti-2-verifica-decisione", processInstance3.getId());


        // Carico le nuove form V3
        deployNewVersionForms("acquisti", "3", "start");
        deployNewVersionForms("acquisti", "3", "verifica-decisione");

        // Verifico che la prima e la seconda versione continuino a funzionare correttamente
        verificaVersione("acquisti", "1", "start", "acquisti-1-start", null);
        verificaVersione("acquisti", "1", "verifica-decisione", "acquisti-1-verifica-decisione", processInstance1.getId());
        verificaVersione("acquisti", "2", "start", "acquisti-2-start", null);
        verificaVersione("acquisti", "2", "verifica-decisione", "acquisti-2-verifica-decisione", processInstance2.getId());

        // Verifico che ora invece la terza versione carichi la sua versione delle form
        verificaVersione("acquisti", "3", "start", "acquisti-3-start", null);
        verificaVersione("acquisti", "3", "verifica-decisione", "acquisti-3-verifica-decisione", processInstance3.getId());

    }

    private void verificaVersione(String nomeFlusso, String versione, String taskDefinitionId, String equalsTo, String processInstanceId) {

        LOGGER.info("verificando versione {} {} {} {}", nomeFlusso, versione, taskDefinitionId, processInstanceId);

        String formForTrittico = formResource.getFormByTrittico(nomeFlusso, versione, taskDefinitionId).getBody();
        LOGGER.info("Got formForTrittico: {}", formForTrittico);
        assertEquals(forms.get(equalsTo), formForTrittico);

        if (processInstanceId != null) {
            String currentTaskId = taskService.createTaskQuery().processInstanceId(processInstanceId).list().get(0).getId();
            String formForTask = formResource.getFormByTaskId(currentTaskId).getBody();
            LOGGER.info("Got formForTask: {}", formForTask);
            assertEquals(formForTask, formForTrittico);
        }
    }

    private void deployNewProcessVersion(String filename) throws IOException {
//        ClassLoader classLoader = getClass().getClassLoader();
//        byte[] bytes = classLoader.getResource("processes/"+ filename +".bpmn").getFile().getBytes();

//
//        MockMultipartFile procDefReq = new MockMultipartFile(filename, bytes);
//        procDefResource.updateProcessDefinition(procDefReq);

        repositoryService.createDeployment()
                .addClasspathResource("processes/"+ filename +".bpmn").deploy();
    }

    private void deployNewVersionForms(String processDefinitionId, String version, String taskDefinitionId) throws IOException, URISyntaxException {

        Form form = new Form();
        form.setProcessDefinitionKey(processDefinitionId);
        form.setVersion(version);
        form.setTaskId(taskDefinitionId);
        form.setForm(forms.get(processDefinitionId +"-"+ version +"-"+ taskDefinitionId));

        formResource.createForm(form);
    }




    private void setup() throws Exception {

        processInstance1 = util.mySetUp(acquisti);

        String formStart = formResource.getFormByTrittico("acquisti", "1", "start").getBody();
        forms.put("acquisti-1-start", formStart);
        forms.put("acquisti-2-start", formStart.replace("Responsabile Unico del Procedimento (RUP)", "RUP"));
        forms.put("acquisti-3-start", formStart.replace("Responsabile Unico del Procedimento (RUP)", "Responsabile procedimento Unico"));

        String formVerifica = formResource.getFormByTrittico("acquisti", "1", "verifica-decisione").getBody();
        forms.put("acquisti-1-verifica-decisione", formVerifica);
        forms.put("acquisti-2-verifica-decisione", formVerifica.replace("Il Supporto alle Funzioni Direzionali", "L'SFD"));
        forms.put("acquisti-3-verifica-decisione", formVerifica.replace("Il Supporto alle Funzioni Direzionali", "Il Supporto alle Funzioni Direzionali (SFD)"));

//        String formModifica = formResource.getFormByTrittico("acquisti", "1", "modifica-decisione").getBody();
//        forms.put("acquisti-1-modifica-decisione", formModifica);
//        forms.put("acquisti-2-modifica-decisione", formModifica.replace("Titolo Acquisizione", "Titolo Acquisizione2"));
//        forms.put("acquisti-3-modifica-decisione", formModifica.replace("Titolo Acquisizione", "Titolo Acquisizione3"));

    }


/*    private void respingiFlussi() throws IOException {

        util.loginSfd();
        MockMultipartHttpServletRequest req = new MockMultipartHttpServletRequest();

        String taskId = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).list().get(0).getId();
        LOGGER.info(taskId);

        req.setParameter("taskId", taskId);
        req.setParameter("sceltaUtente", "Modifica");
        req.setParameter("commento", "fghj");
        flowsTaskResource.completeTask(req);

        taskId = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).list().get(0).getId();
        LOGGER.info(taskId);
        req.setParameter("taskId", taskId);
        flowsTaskResource.completeTask(req);

        taskId = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).list().get(0).getId();
        LOGGER.info(taskId);
        req.setParameter("taskId", taskId);
        flowsTaskResource.completeTask(req);
    }*/
}
