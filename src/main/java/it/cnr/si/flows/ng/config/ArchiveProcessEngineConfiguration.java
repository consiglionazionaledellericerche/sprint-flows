package it.cnr.si.flows.ng.config;

import static org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl.DEFAULT_GENERIC_MAX_LENGTH_STRING;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.bpmn.data.ItemInstance;
import org.activiti.engine.impl.bpmn.webservice.MessageInstance;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.impl.variable.BooleanType;
import org.activiti.engine.impl.variable.ByteArrayType;
import org.activiti.engine.impl.variable.CustomObjectType;
import org.activiti.engine.impl.variable.DateType;
import org.activiti.engine.impl.variable.DefaultVariableTypes;
import org.activiti.engine.impl.variable.DoubleType;
import org.activiti.engine.impl.variable.IntegerType;
import org.activiti.engine.impl.variable.JsonType;
import org.activiti.engine.impl.variable.LongJsonType;
import org.activiti.engine.impl.variable.LongStringType;
import org.activiti.engine.impl.variable.LongType;
import org.activiti.engine.impl.variable.NullType;
import org.activiti.engine.impl.variable.SerializableType;
import org.activiti.engine.impl.variable.ShortType;
import org.activiti.engine.impl.variable.StringType;
import org.activiti.engine.impl.variable.UUIDType;
import org.activiti.engine.impl.variable.ValueFields;
import org.activiti.engine.impl.variable.VariableType;
import org.activiti.engine.impl.variable.VariableTypes;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@AutoConfigureAfter(FlowsProcessEngineConfigurations.class)
@Profile("cnr")
public class ArchiveProcessEngineConfiguration {

    private static final int VARIABLE_LIMIT = 200000;

    @Value("${cnr.archive.datasource.url}")
    private String archiveUrl;
    @Value("${cnr.archive.datasource.username}")
    private String archiveUsername;
    @Value("${cnr.archive.datasource.password}")
    private String archivePassword;
    
    @Autowired
    private ApplicationContext appContext;
    
    
    @Bean(name = "archiveProcessEngine")
    public ProcessEngine getArchiveProcessEngine() throws Exception {
        
        StandaloneProcessEngineConfiguration conf = new StandaloneProcessEngineConfiguration();

        conf.setJdbcDriver("org.postgresql.Driver");
        conf.setJdbcUrl(archiveUrl);
        conf.setJdbcUsername(archiveUsername);
        conf.setJdbcPassword(archivePassword);
        conf.setDatabaseType(ProcessEngineConfigurationImpl.DATABASE_TYPE_POSTGRES);        
        
        conf.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE); // TODO PERIGRO

        //IMPORTANTE: aggiungo un nuovo tipo di dato specifico SOLO SE è NON VERRA' MODIFICATO (per non creare problemi al DB)
        // https://blog.progs.be/628/activiti-variables-json
        VariableTypes variableTypes = new DefaultVariableTypes();

        //Aggiungo il tipo json nel db (per le variabili di tipo json, se si riesce ad usare dovrebbe gestirle meglio delle stringhe e degli stream)
        variableTypes.addType(new JsonType(DEFAULT_GENERIC_MAX_LENGTH_STRING, conf.getObjectMapper()));
        variableTypes.addType(new LongJsonType(DEFAULT_GENERIC_MAX_LENGTH_STRING + 1, conf.getObjectMapper()));
        variableTypes.addType(new LongStringType(DEFAULT_GENERIC_MAX_LENGTH_STRING));

        variableTypes.addType(new NullType());
        variableTypes.addType(new StringType(DEFAULT_GENERIC_MAX_LENGTH_STRING));
        variableTypes.addType(new BooleanType());
        variableTypes.addType(new ShortType());
        variableTypes.addType(new IntegerType());
        variableTypes.addType(new LongType());
        variableTypes.addType(new DateType());
        variableTypes.addType(new DoubleType());
        variableTypes.addType(new UUIDType());
        // Risolvono il problema delle variabili "troppo" lunghe (ad es.: "valutazioneEsperienze_json")
        variableTypes.addType(new ByteArrayType());
        variableTypes.addType(new SerializableType());
        variableTypes.addType(new CustomObjectType("item", ItemInstance.class));
        variableTypes.addType(new CustomObjectType("message", MessageInstance.class));
        variableTypes.addType(new AlfrescoScriptNodeType());

        conf.setVariableTypes(variableTypes);

        //Serve per recuperare molte process istances/task nelle search dell'app (si riferisce al numero di variabili recuperabili nelle query (default 20000))
        conf.setHistoricProcessInstancesQueryLimit(VARIABLE_LIMIT);
        conf.setHistoricTaskQueryLimit(VARIABLE_LIMIT);

        ProcessEngineFactoryBean factory = new ProcessEngineFactoryBean();
        factory.setApplicationContext(appContext);
        factory.setProcessEngineConfiguration(conf);

        return factory.getObject();
    }

    public class AlfrescoScriptNodeType implements VariableType {

        @Override
        public String getTypeName() {
            return "alfrescoScriptNode";
        }

        public boolean isCachable() {
            return true;
        }

        public boolean isAbleToStore(Object value) {
            if (value==null) {
                return true;
            }
            if (String.class.isAssignableFrom(value.getClass())) {
                String stringValue = (String) value;
                return stringValue.length() <= DEFAULT_GENERIC_MAX_LENGTH_STRING;
            }
            return false;
        }

        public Object getValue(ValueFields valueFields) {
            return valueFields.getTextValue();
        }

        public void setValue(Object value, ValueFields valueFields) {
            valueFields.setTextValue((String) value);
        }
    }

}
