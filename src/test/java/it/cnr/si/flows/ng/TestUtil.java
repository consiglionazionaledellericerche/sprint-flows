package it.cnr.si.flows.ng;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import it.cnr.si.flows.ng.resource.FlowsProcessDefinitionResource;
import it.cnr.si.flows.ng.resource.FlowsProcessInstanceResource;
import it.cnr.si.flows.ng.resource.FlowsTaskResource;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.repository.ProcessDefinitionResponse;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.cnr.si.config.JacksonConfiguration.ISO_DATE_OPTIONAL_TIME;
import static it.cnr.si.config.JacksonConfiguration.ISO_FIXED_FORMAT;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

/**
 * Utility class for testing REST controllers.
 */
@Service
public class TestUtil {


    /** MediaType for JSON UTF8 */
    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    public static final String TITOLO_DELL_ISTANZA_DEL_FLUSSO = "titolo dell'istanza del flusso JUnit";

    @Autowired
    FlowsProcessInstanceResource flowsProcessInstanceResource;
    @Autowired
    private FlowsProcessDefinitionResource flowsProcessDefinitionResource;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private FlowsTaskResource flowsTaskResource;

    private String firstTaskId;
    private String processDefinition;
//    private ProcessInstanceResponse processInstance;

    /**
     * Convert an object to JSON byte array.
     *
     * @param object
     *            the object to convert
     * @return the JSON byte array
     * @throws IOException
     */
    public static byte[] convertObjectToJsonBytes(Object object)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer(ISO_FIXED_FORMAT));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(ISO_DATE_OPTIONAL_TIME));
        mapper.registerModule(module);

        return mapper.writeValueAsBytes(object);
    }

    /**
     * Create a byte array with a specific size filled with specified data.
     *
     * @param size the size of the byte array
     * @param data the data to put in the byte array
     */
    public static byte[] createByteArray(int size, String data) {
        byte[] byteArray = new byte[size];
        for (int i = 0; i < size; i++) {
            byteArray[i] = Byte.parseByte(data, 2);
        }
        return byteArray;
    }

    public void loginAdmin() {
        login("admin", "admin");
    }

    public void loginSpaclient() {
        login("spaclient", "sp@si@n0");
    }

    public void loginUser() {
        login("user", "user");
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
        DataResponse ret = (DataResponse) flowsProcessDefinitionResource.getAllProcessDefinitions();

        ArrayList<ProcessDefinitionResponse> processDefinitions = (ArrayList<ProcessDefinitionResponse>) ret.getData();
        for (ProcessDefinitionResponse pd : processDefinitions) {
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
//        processInstance = (ProcessInstanceResponse) response.getBody();
        return (ProcessInstanceResponse) response.getBody();
    }


    public void myTearDown() {
        //cancello le Process Instance creata all'inizio del test'
        List<ProcessInstance> list = runtimeService.createProcessInstanceQuery().list();
        HttpServletResponse res = new MockHttpServletResponse();
        for (ProcessInstance pi : list) {
            flowsProcessInstanceResource.delete(res, pi.getProcessInstanceId(), "TEST");
            assertEquals(NO_CONTENT.value(), res.getStatus());
        }
        logout();
    }

    public String getProcessDefinition() {
        return processDefinition;
    }

//    public ProcessInstanceResponse getProcessInstance() {
//        return processInstance;
//    }

    public String getFirstTaskId() {
        return firstTaskId;
    }
}
