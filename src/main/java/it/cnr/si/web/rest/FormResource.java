package it.cnr.si.web.rest;

import com.codahale.metrics.annotation.Timed;
import it.cnr.si.domain.Form;

import it.cnr.si.repository.FormRepository;
import it.cnr.si.security.AuthoritiesConstants;
import it.cnr.si.web.rest.util.HeaderUtil;
import it.cnr.si.web.rest.util.PaginationUtil;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing Form.
 */
@RestController
@RequestMapping("/api")
public class FormResource {

    private final Logger log = LoggerFactory.getLogger(FormResource.class);

    @Inject
    private FormRepository formRepository;
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private TaskService taskService;

    /**
     * POST  /forms : Create a new form.
     *
     * @param form the form to create
     * @return the ResponseEntity with status 201 (Created) and with body the new form, or with status 400 (Bad Request) if the form has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/forms",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Form> createForm(@Valid @RequestBody Form form) throws URISyntaxException {
        log.debug("REST request to save Form : {}", form);
        if (form.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("form", "idexists", "A new form cannot already have an ID")).body(null);
        }
        Form result = formRepository.save(form);
        return ResponseEntity.created(new URI("/api/forms/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert("form", result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /forms : Updates an existing form.
     *
     * @param form the form to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated form,
     * or with status 400 (Bad Request) if the form is not valid,
     * or with status 500 (Internal Server Error) if the form couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @RequestMapping(value = "/forms",
        method = RequestMethod.PUT,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Form> updateForm(@Valid @RequestBody Form form) throws URISyntaxException {
        log.debug("REST request to update Form : {}", form);
        if (form.getId() == null) {
            return createForm(form);
        }
        Form result = formRepository.save(form);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert("form", form.getId().toString()))
            .body(result);
    }

    /**
     * GET  /forms : get all the forms.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of forms in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/forms",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<List<Form>> getAllForms(Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Forms");
        Page<Form> page = formRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/forms");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /forms/:id : get the "id" form.
     *
     * @param id the id of the form to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the form, or with status 404 (Not Found)
     */
    @RequestMapping(value = "/forms/{id}",
        method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<Form> getForm(@PathVariable Long id) {
        log.debug("REST request to get Form : {}", id);
        Form form = formRepository.findById(id).get();
        return Optional.ofNullable(form)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /forms/:id : delete the "id" form.
     *
     * @param id the id of the form to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @RequestMapping(value = "/forms/{id}",
        method = RequestMethod.DELETE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @Secured(AuthoritiesConstants.USER)
    public ResponseEntity<Void> deleteForm(@PathVariable Long id) {
        log.debug("REST request to delete Form : {}", id);
        formRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("form", id.toString())).build();
    }

    @RequestMapping(value = "/forms/{processDefinitionKey}/{version}/{taskId}",
        method = RequestMethod.GET,
        produces = MediaType.TEXT_HTML_VALUE)
    @Timed
    public ResponseEntity<String> getFormByTrittico(
            @PathVariable String processDefinitionKey,
            @PathVariable String version,
            @PathVariable String taskId) {

        log.debug("REST request to get Form : {}/{}/{}", processDefinitionKey, version, taskId);

        int intVersion = Integer.parseInt(version);

        while (intVersion > 0) {
            Form form = formRepository.findOneByProcessDefinitionKeyAndVersionAndTaskId(processDefinitionKey, String.valueOf(intVersion), taskId);
            if (form != null)
                return new ResponseEntity<>(form.getForm(), HttpStatus.OK);
            else
                intVersion--;
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/forms/task/{taskId}",
        method = RequestMethod.GET,
        produces = MediaType.TEXT_HTML_VALUE)
    @Timed
    public ResponseEntity<String> getFormByTaskId(
            @PathVariable String taskId) {

        log.debug("REST request to get Form for task: {}",  taskId);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ProcessInstance process = runtimeService.createProcessInstanceQuery().processDefinitionId(task.getProcessDefinitionId()).list().get(0);

        return getFormByTrittico(
                process.getProcessDefinitionKey(),
                process.getProcessDefinitionVersion().toString(),
                task.getTaskDefinitionKey());
    }

}
