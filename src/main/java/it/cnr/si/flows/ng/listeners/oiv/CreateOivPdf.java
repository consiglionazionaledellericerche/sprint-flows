package it.cnr.si.flows.ng.listeners.oiv;

import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.utils.Utils;
import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;

import it.cnr.si.service.OivPdfService;
import it.cnr.si.service.RelationshipService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.utils.Utils.PROCESS_VISUALIZER;



@Component
public class CreateOivPdf implements ExecutionListener {
	private static final long serialVersionUID = 686169707042367215L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateOivPdf.class);

	@Inject
	private OivPdfService oivPdfService;
	
	@Override
	public void notify(DelegateExecution execution) throws Exception {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		String processInstanceId =  execution.getProcessInstanceId();
		LOGGER.info("ProcessInstanceId: " + processInstanceId);
		//(OivPdfService oivPdfService = new OivPdfService();
		String Titolo = oivPdfService.createPdf(processInstanceId, outputStream);
		LOGGER.info("avvio la generazione del pdf: " + Titolo);
		

	}}
