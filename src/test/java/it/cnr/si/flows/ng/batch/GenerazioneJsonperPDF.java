package it.cnr.si.flows.ng.batch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.time.LocalDate;

import it.cnr.si.FlowsApp;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntity;
import org.activiti.engine.impl.variable.LongStringType;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.spring.boot.AbstractProcessEngineAutoConfiguration;
import org.activiti.spring.boot.DataSourceProcessEngineAutoConfiguration;
import org.activiti.spring.boot.EndpointAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration;
import org.activiti.spring.boot.RestApiAutoConfiguration;
import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.activiti.spring.boot.JpaProcessEngineAutoConfiguration.JpaConfiguration;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.CompletionStageReturnValueHandler;

import com.opencsv.CSVParser;

import feign.FeignException;
import it.cnr.si.SprintApp;
import it.cnr.si.config.Constants;
import it.cnr.si.config.DefaultProfileUtil;
import it.cnr.si.config.JHipsterProperties;
import it.cnr.si.flows.ng.exception.UnexpectedResultException;
import it.cnr.si.flows.ng.service.AceBridgeService;
import it.cnr.si.flows.ng.service.SiperService;
import it.cnr.si.service.AceService;
import it.cnr.si.service.dto.anagrafica.base.PageDto;
import it.cnr.si.service.dto.anagrafica.letture.EntitaOrganizzativaWebDto;
import it.cnr.si.service.dto.anagrafica.letture.PersonaWebDto;

@SpringBootTest(classes = FlowsApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles(profiles = "dev,cnr")
@ActiveProfiles(profiles = "dev,cnr")
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class GenerazioneJsonperPDF {

	private static final Logger log = LoggerFactory.getLogger(GenerazioneJsonperPDF.class);

	@Inject
	private AceService aceService;
	@Inject
	private AceBridgeService aceBridgeService;
	@Inject
	private SiperService siperService;
	@Inject
	private HistoryService historyService;
	@Inject
	private RuntimeService runtimeService;
	
	
	
	private final Map<String, String> errors = new HashMap<>();
	int personNr = 0;

	//@Test questa riga non va mai messa su git
	//@Test
	public void runBatch() throws IOException {
		String processInstanceId = "25001";
		//Sotituisco la lista di variabili da quelle storiche (historicProcessInstance.getProcessVariables() )a quelle attuali (variableInstanceJson)
		JSONObject variableInstanceJson = new JSONObject();

		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
				.includeProcessVariables()
				.processInstanceId(processInstanceId)
				.singleResult();

		// Verifico se il workflow sia terminato
		if((historicProcessInstance != null) && (historicProcessInstance.getEndTime() != null)){
			//carico le processVariables e rimappo in formato json il campo stringa "valutazioneEsperienze_json"
			variableInstanceJson = new JSONObject(historicProcessInstance.getProcessVariables());
		} else {
			Map<String, VariableInstance> tutteVariabiliMap = runtimeService.getVariableInstances(processInstanceId);
			for (Map.Entry<String, VariableInstance> entry : tutteVariabiliMap.entrySet()) {
				String key = entry.getKey();
				VariableInstance value = entry.getValue();
				//le variabili di tipo serializable (file) non vanno inseriti nel json delle variabili che verranno inseriti nel pdf
				//(ho testato valutazioni esperienze_Json fino a 11000 caratteri ed a questo livello appare come longString)
				if((!(((VariableInstanceEntity) value).getType() instanceof SerializableType)) || (((VariableInstanceEntity) value).getType() instanceof LongStringType)){
					if(key.toString().equals("startDate")) {
						Date startDate = (Date)value.getValue();
						SimpleDateFormat sdf = new  SimpleDateFormat("dd/MM/yyyy HH:mm");
						sdf.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
						variableInstanceJson.put(key, sdf.format(startDate));
					} else {
						// ...
						String valueEscaped = StringEscapeUtils.escapeHtml(value.getValue().toString());
						variableInstanceJson.put(key, valueEscaped);
					}
				}	
			}
			log.info("variableInstanceJson: {}", variableInstanceJson);
		}
	}


}





















