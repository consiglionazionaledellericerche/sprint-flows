package it.cnr.si.flows.ng.utils;

import it.cnr.si.flows.ng.service.FlowsProcessInstanceService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.task.TaskInfoQuery;
import org.activiti.rest.service.api.engine.variable.RestVariable;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static it.cnr.si.flows.ng.service.FlowsTaskService.*;


@Component("utils")
public final class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static final String INITIATOR = "initiator";
    public static final String TITOLO = "titolo";
    public static final String DESCRIZIONE = "descrizione";
    public static final String STATO = "stato";

    public static final String TASK_EXECUTOR = "esecutore";
    public static final String PROCESS_VISUALIZER = "visualizzatore";

    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    public static final String ALL_PROCESS_INSTANCES = "all";
    public static final String LESS = "Less";
    public static final String ERRORE_NEL_PARSING_DELLA_DATA = "Errore nel parsing della data {} - ";
    public static final String GREAT = "Great";
    public static final SimpleDateFormat formatoDataUF = new SimpleDateFormat("dd-MM-yyyy");
    private static final String TEXT_EQUAL = "textEqual";
    private static final String BOOLEAN = "boolean";
    private static final String ROLE = "ROLE_";
    private static DateFormat formatoData = new SimpleDateFormat("yyyy-MM-dd");
    private static DateFormat formatoDataOra = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
    @Inject
    private RuntimeService runtimeService;
    @Inject
    private FlowsProcessInstanceService flowsProcessInstanceService;


    @Inject
    private Environment env;

    public static boolean isEmpty(String in) {
        return in == null || in.equals("");
    }

    public static boolean isNotEmpty(String in) {
        return !isEmpty(in);
    }


    public static String replaceStruttura(String groupRelationship, String struttura) {
        return groupRelationship.replace("@STRUTTURA", struttura);
    }

    public static String removeLeadingRole(String in) {
        return in.startsWith(ROLE) ? in.substring(5) : in;
    }
    
    public static String removeImportoSpesa(String in) {
    	if (!in.contains("$"))
    		return in;
    	
    	return in.substring(0, in.indexOf('$')) + in.substring(in.indexOf('@'));
    }

    public static double getImportoSpesa(String in) {
    	return Double.parseDouble(in.substring(in.indexOf('$')+1, in.indexOf('@')));
    }
    
    public static String addLeadingRole(String in) {
        return in.startsWith(ROLE) ? in : ROLE + in;
    }

    public static Integer parseInt(String in) {
        try {
            return Integer.parseInt(in);
        } catch (Exception e) {
            return null;
        }

    }

    public static HttpStatus getStatus(Integer errCode) {
        try {
            return HttpStatus.valueOf(errCode);
        } catch (Exception e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    public static HttpStatus getStatus(String errCode) {
        return getStatus(parseInt(errCode));
    }

    public static Map<String, Object> mapOf(String key, String value) {
        HashMap<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    public static String filterProperties(List<RestVariable> properties, String property) {
        List<RestVariable> list = properties.stream()
                .filter(a -> a.getName().equals(property))
                .collect(Collectors.toList());
        String ret = "";
        if (!list.isEmpty()) {
            ret = ret + list.get(0).getValue();
        }
        return ret;
    }

    public String formattaDataOra(Date in) {
        if (in != null)
            return formatoDataOra.format(in);
        else
            return "";
    }

    public String formattaData(Date in) {
        return formatoData.format(in);
    }

    public static Date parsaData(String in) throws ParseException {
        return formatoData.parse(in);
    }

    public void init() {
        formatoDataOra.setTimeZone(TimeZone.getTimeZone("Europe/Rome"));
    }


    public TaskInfoQuery orderTasks(String order, TaskInfoQuery query) {
        if (order.equals(ASC))
            query.orderByTaskCreateTime().asc();
        else if (order.equals(DESC))
            query.orderByTaskCreateTime().desc();

        return query;
    }


    public HistoricProcessInstanceQuery orderProcess(String order, HistoricProcessInstanceQuery historicProcessQuery) {
        if (order.equals(ASC))
            historicProcessQuery.orderByProcessInstanceStartTime().asc();
        else if (order.equals(DESC))
            historicProcessQuery.orderByProcessInstanceStartTime().desc();
        return historicProcessQuery;
    }


    public String[] getArray(List<String> tupla) {
        String[] entries = new String[tupla.size()];
        entries = tupla.toArray(entries);
        return entries;
    }


    public boolean isProfileActive(String profile) {
        return Arrays.asList(env.getActiveProfiles()).contains(profile);
    }

    public TaskInfoQuery searchParams(JSONArray taskParams, TaskInfoQuery taskQuery) {

        for (int i = 0; i < taskParams.length(); i++) {
            JSONObject appo = taskParams.optJSONObject(i);
            String key = appo.getString("key");
            String value = appo.getString("value");
            String type = appo.getString("type");

            //solo per le HistoricTaskInstanceQuery si fa la query in base alla data di completamento del task
            if (taskQuery instanceof HistoricTaskInstanceQuery) {
                try {
                    if ("taskCompletedGreat".equals(key)) {
                        ((HistoricTaskInstanceQuery) taskQuery).taskCompletedAfter(formatoData.parse(value));
                        break;
                    } else if ("taskCompletedLess".equals(key)) {
                        ((HistoricTaskInstanceQuery) taskQuery).taskCompletedBefore(formatoData.parse(value));
                        break;
                    }
                } catch (ParseException e) {
                    LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
                }
            }

            switch (key) {
                case "Fase":
                    //la "Fase" equivale al nome del task - quindi bisogna fare una ricerca "a parte" (non in base al "type") ...
                    taskQuery.taskNameLikeIgnoreCase("%" + value + "%");
                    break;
                case "businessKey":
                    //... stesso discorso per la "businessKey"
                    taskQuery.processInstanceBusinessKeyLike("%" + value + "%");
                    break;
                case "processInstanceId":
                    //... stesso discorso per la "processInstanceId"
                    taskQuery.processInstanceId(value);
                    break;
                default:
                    //wildcard ("%") di default ma non a TUTTI i campi
                    switch (type) {
                        case TEXT_EQUAL:
                            taskQuery.or()
                                    .taskVariableValueEquals(key, value)
                                    .processVariableValueEquals(key, value).endOr();
                            break;
                        case BOOLEAN:
                            // gestione variabili booleane
                            taskQuery.or()
                                    .taskVariableValueEquals(key, Boolean.valueOf(value))
                                    .processVariableValueEquals(key, Boolean.valueOf(value)).endOr();
                            break;
                        case "date":
                            taskQuery = historicTaskDate(taskQuery, key, value);
                            break;
                        default:
                            //variabili con la wildcard  (%value%)
                            taskQuery.or()
                                    .taskVariableValueLikeIgnoreCase(key, "%" + value + "%")
                                    .processVariableValueLikeIgnoreCase(key, "%" + value + "%").endOr();
                            break;
                    }
                    break;
            }
        }
        return taskQuery;
    }


    private static TaskInfoQuery historicTaskDate(TaskInfoQuery taskQuery, String key, String value) {
        try {
            Date date = parsaData(value);

            if (key.contains(LESS)) {
                taskQuery.or()
                        .taskVariableValueLessThanOrEqual(key.replace(LESS, ""), date)
                        .processVariableValueLessThanOrEqual(key.replace(LESS, ""), date).endOr();
            } else if (key.contains(GREAT))
                taskQuery.or()
                        .taskVariableValueGreaterThanOrEqual(key.replace(GREAT, ""), date)
                        .processVariableValueLessThanOrEqual(key.replace(GREAT, ""), date).endOr();
        } catch (ParseException e) {
            LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
        }
        return taskQuery;
    }


    public static class SearchResult {
        String value;
        String label;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SearchResult that = (SearchResult) o;
            return Objects.equals(value, that.value) && Objects.equals(label, that.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, label);
        }

        public SearchResult(String v, String l) {
            value = v;
            label = l;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }


    public static class associazioneRuoloPersonaCDSUO {
        String ruolo;
        String persona;
        String cdsuo;



        public String getRuolo() {
            return ruolo;
        }

        public String getPersona() {
            return persona;
        }

        public String getCdsuo() {
            return cdsuo;
        }

        public void setRuolo(String ruolo) {
            this.ruolo = ruolo;
        }

        public void setCdsuo(String cdsuo) {
            this.cdsuo = cdsuo;
        }

        public void setPersona(String persona) {
            this.persona = persona;
        }
    }

    public String getString(Map<String, String> params, String paramName, String defaultValue) {
        String value = params.get(paramName);
        return value != null ? value : defaultValue;
    }


    public int getInteger(Map<String, String> params, String paramName, int defaultValue) {
        try {
            return Integer.parseInt( getString(params, paramName, String.valueOf(defaultValue)) ) ;
        } catch (NumberFormatException e) {
            LOGGER.info("Number Format Exception per il parametro {} con valore {}", paramName, params.get(paramName));
            return defaultValue;
        }
    }


    public boolean getBoolean(Map<String, String> params, String paramName, boolean defaultValue) {
        return Boolean.parseBoolean( getString(params, paramName, String.valueOf(defaultValue)) ) ;
    }

    public static boolean isFullPath(String path) {
        try {
            String regex = "^.*/.+[.]\\D{3,6}$";
            return path.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }



    /**
     * Aggiorna il json con i campi che servono per velocizzare
     * le ricerce (stato, initiator, titolo e descrizione)
     * all'interno del name della ProcessInstance.
     *
     * @param executionId       the execution id
     * @param processInstanceId the process instance id
     * @param stato             the stato
     */
    public void updateJsonSearchTerms(String executionId, String processInstanceId, String stato) {

        try {
            String initiator = "";
            String titolo = "";
            String descrizione = "";
    
            if(executionId == null)
                executionId = flowsProcessInstanceService.getCurrentTaskOfProcessInstance(processInstanceId).getExecutionId();
    
            if (runtimeService.getVariable(executionId , INITIATOR) != null)
                initiator = runtimeService.getVariable(executionId , INITIATOR).toString();
    
            if (runtimeService.getVariable(executionId , TITOLO) != null)
                titolo =   runtimeService.getVariable(executionId , TITOLO).toString();
    
            if (runtimeService.getVariable(executionId , DESCRIZIONE) != null)
                descrizione =   runtimeService.getVariable(executionId , DESCRIZIONE).toString();
    
            org.json.JSONObject name = new org.json.JSONObject();
            name.put(DESCRIZIONE, ellipsis(descrizione, LENGTH_DESCRIZIONE));
            name.put(TITOLO, ellipsis(titolo, LENGTH_TITOLO));
    
            //Se il campo "stato" è vuoto ==> riscrivo nel json lo stato che aveva ...
            if(stato.isEmpty()){
                String vecchioStato = new org.json.JSONObject(flowsProcessInstanceService.getProcessInstance(processInstanceId).getName()).getString(STATO);
                name.put(STATO, ellipsis(vecchioStato, LENGTH_STATO) );
            }else {
                // ... altrimenti aggiorno con il nuovo stato
                name.put(STATO, ellipsis(stato, LENGTH_STATO));
            }
            name.put(INITIATOR, initiator);
    
            runtimeService.setProcessInstanceName(processInstanceId, name.toString());
        } catch (Exception e) {
            LOGGER.error("Errore nell'aggiornamento del titolo della processInstance {}: {}", processInstanceId, e.getMessage(), e);
        }
    }



    public static String ellipsis(String in, int length) {
        if (in!= null) {
            if (in.length() < length) {
                return in;
            } else
            {
                return in.substring(0, length - 3) + "...";
            }
        }else {
            return "";
        }
    }
    
    public static String sanitizeHtml(Object in) {
        if (in == null) return "";
        if (!(in instanceof String)) return "";
        
        String inVal = (String) in;
        inVal = inVal.replaceAll("strong>", "b>");
        inVal = inVal.replaceAll("em>", "i>");
        
        return Jsoup.clean(inVal, Whitelist.relaxed());
    }
}
