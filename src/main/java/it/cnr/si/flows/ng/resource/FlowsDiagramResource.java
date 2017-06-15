package it.cnr.si.flows.ng.resource;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.flows.ng.service.FlowsProcessDiagramGenerator;
import it.cnr.si.flows.ng.service.FlowsProcessDiagramService;
import org.activiti.engine.*;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;


@Controller
@RequestMapping("rest")
public class FlowsDiagramResource {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowsProcessDiagramGenerator pdg;
    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;
    @Autowired
    private HistoryService historyService;
    @Inject
    private FlowsProcessDiagramService flowsProcessDiagramService;

    @RequestMapping(value = "diagram/processDefinition/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramForProcessDefinition(
            @PathVariable String id)
            throws IOException {

        InputStream resourceAsStream = repositoryService.getProcessDiagram(id);

        return ResponseEntity.ok(new InputStreamResource(resourceAsStream));
    }

    @RequestMapping(value = "diagram/processInstance/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource> getDiagramForProcessInstance(
            @PathVariable String id, String font)
            throws IOException {
        InputStream resource = flowsProcessDiagramService.getDiagramForProcessInstance(id, font);
        return ResponseEntity.ok(new InputStreamResource(resource));
    }


    @RequestMapping(value = "diagram/taskInstance/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramPerTaskInstanceId(
            @PathVariable String id)
            throws IOException {

        Task task = taskService.createTaskQuery().taskId(id).singleResult();

        return getDiagramForProcessInstance(task.getProcessInstanceId(), null);

    }

    @RequestMapping(value = "/diagram/taskInstance/{id}/{font}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @Timed
    public ResponseEntity<InputStreamResource>
    getDiagramPerTaskInstanceId(
            @PathVariable String id,
            @PathVariable String font)
            throws IOException {

        Task task = taskService.createTaskQuery().taskId(id).singleResult();

        return getDiagramForProcessInstance(task.getProcessInstanceId(), font);

    }
}
