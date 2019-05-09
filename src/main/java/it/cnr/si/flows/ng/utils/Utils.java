package it.cnr.si.flows.ng.utils;

import com.google.common.base.Strings;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.query.Query;
import org.activiti.engine.task.TaskInfoQuery;
import org.activiti.rest.service.api.engine.variable.RestVariable;
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


@Component("utils")
public final class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static final String INITIATOR = "initiator";
    public static final String TITOLO = "titolo";
    public static final String DESCRIZIONE = "descrizione";

    public static final String TASK_EXECUTOR = "esecutore";
    public static final String PROCESS_VISUALIZER = "visualizzatore";

    private static final String TASK_PARAMS = "taskParams";
    public static final String PROCESS_PARAMS = "processParams";
    private static final String ERRORE_NELLA_LETTURE_DELLO_STREAM_DELLA_REQUEST = "Errore nella letture dello stream della request";
    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    public static final String ALL_PROCESS_INSTANCES = "all";
    public static final String LESS = "Less";
    public static final String ERRORE_NEL_PARSING_DELLA_DATA = "Errore nel parsing della data {} - ";
    public static final String GREAT = "Great";
    private static final String TEXT_EQUAL = "textEqual";
    private static final String BOOLEAN = "boolean";
    private static final String ROLE = "ROLE_";
    private static DateFormat formatoData = new SimpleDateFormat("yyyy-MM-dd");
    private static DateFormat formatoDataOra = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);

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

    public static List<String> getCurrentUserAuthorities() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Utils::removeLeadingRole)
                .collect(Collectors.toList());
    }

    public static String removeLeadingRole(String in) {
        return in.startsWith(ROLE) ? in.substring(5) : in;
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


    public static TaskInfoQuery orderTasks(String order, TaskInfoQuery query) {
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


    public static TaskInfoQuery searchParamsForTasks(JSONObject json, TaskInfoQuery query) {
        try {
            if (json.has(PROCESS_PARAMS))
                query = extractProcessSearchParams(query, json.getJSONArray(PROCESS_PARAMS));
            if (json.has(TASK_PARAMS))
                query = extractTaskSearchParams(query, json.getJSONArray(TASK_PARAMS));
        } catch (Exception e) {
            LOGGER.error(ERRORE_NELLA_LETTURE_DELLO_STREAM_DELLA_REQUEST, e);
        }
        return query;
    }


    public Query searchParamsForProcess(JSONArray processParams, HistoricProcessInstanceQuery processQuery) {
        try {
//            todo: controllare

            for (int i = 0; i < processParams.length(); i++) {
                JSONObject appo = processParams.optJSONObject(i);
                String key = appo.getString("key");
                String value = appo.getString("value");
                String type = appo.getString("type");
                //wildcard ("%") di default ma non a TUTTI i campi
                switch (type) {
                    case TEXT_EQUAL:
                    case BOOLEAN:
                        // gestione variabili booleane e dei valori testuali "perfettamente uguali"
                        processQuery.variableValueEquals(key, value);
                        break;
                    case "date":
                        Date date = formatoData.parse(value);

                        if (key.contains(LESS)) {
                            processQuery.variableValueLessThanOrEqual(key.replace(LESS, ""), date);
                        } else if (key.contains(GREAT))
                            processQuery.variableValueGreaterThanOrEqual(key.replace(GREAT, ""), date);
                        break;
                    default:
                        //variabili con la wildcard  (%value%)
                        processQuery.variableValueLike(key, "%" + value + "%");
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.error(ERRORE_NELLA_LETTURE_DELLO_STREAM_DELLA_REQUEST, e);
        }
        return processQuery;
    }

    public static TaskInfoQuery extractProcessSearchParams(TaskInfoQuery taskQuery, JSONArray params) {

        for (int i = 0; i < params.length(); i++) {
            JSONObject appo = params.optJSONObject(i);
            String key = appo.getString("key");
            String value = appo.getString("value");
            String type = appo.getString("type");
            //wildcard ("%") di default ma non a TUTTI i campi
            switch (type) {
                case TEXT_EQUAL:
                    taskQuery.processVariableValueEquals(key, value);
                    break;
                case BOOLEAN:
                    // gestione variabili booleane
                    taskQuery.processVariableValueEquals(key, Boolean.valueOf(value));
                    break;
                case "date":
                    try {
                        Date date = formatoData.parse(value);
                        if (key.contains(LESS)) {
                            taskQuery.processVariableValueLessThanOrEqual(key.replace(LESS, ""), date);
                        } else if (key.contains(GREAT))
                            taskQuery.processVariableValueGreaterThanOrEqual(key.replace(GREAT, ""), date);
                    } catch (ParseException e) {
                        LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
                    }
                    break;
                default:
                    //variabili con la wildcard  (%value%)
                    taskQuery.processVariableValueLikeIgnoreCase(key, "%" + value + "%");
                    break;
            }
        }
        return taskQuery;
    }

    public String[] getArray(List<String> tupla) {
        String[] entries = new String[tupla.size()];
        entries = tupla.toArray(entries);
        return entries;
    }

    public boolean isProfileActive(String profile) {
        return Arrays.asList(env.getActiveProfiles()).contains(profile);
    }

    private static TaskInfoQuery extractTaskSearchParams(TaskInfoQuery taskQuery, JSONArray taskParams) {

        for (int i = 0; i < taskParams.length(); i++) {
            JSONObject appo = taskParams.optJSONObject(i);
            String key = appo.getString("key");
            String value = appo.getString("value");
            String type = appo.getString("type");

            //solo per le HistoricTaskInstanceQuery si fa la query in base alla data di completamento del task
            if (taskQuery instanceof HistoricTaskInstanceQuery) {
                try {
                    if (key.equals("taskCompletedGreat")) {
                        ((HistoricTaskInstanceQuery) taskQuery).taskCompletedAfter(formatoData.parse(value));
                        break;
                    }
                    if (key.equals("taskCompletedLess")) {
                        ((HistoricTaskInstanceQuery) taskQuery).taskCompletedBefore(formatoData.parse(value));
                        break;
                    }
                } catch (ParseException e) {
                    LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
                }
            }

            //la "Fase" equivale al nome del task - quindi bisogna fare una ricerca "a parte" (non in base al "type")
            if (key.equals("Fase")) {
                taskQuery.taskNameLikeIgnoreCase("%" + value + "%");
            } else {
                //wildcard ("%") di default ma non a TUTTI i campi
                switch (type) {
                    case TEXT_EQUAL:
                        taskQuery.taskVariableValueEquals(key, value);
                        break;
                    case BOOLEAN:
                        // gestione variabili booleane
                        taskQuery.taskVariableValueEquals(key, Boolean.valueOf(value));
                        break;
                    case "date":
                        taskQuery = historicTaskDate(taskQuery, key, value);
                        break;
                    default:
                        //variabili con la wildcard  (%value%)
                        taskQuery.taskVariableValueLikeIgnoreCase(key, "%" + value + "%");
                        break;
                }
            }
        }
        return taskQuery;
    }


    private static TaskInfoQuery historicTaskDate(TaskInfoQuery taskQuery, String key, String value) {
        try {
            Date date = parsaData(value);

            if (key.contains(LESS)) {
                taskQuery.taskVariableValueLessThanOrEqual(key.replace(LESS, ""), date);
            } else if (key.contains(GREAT))
                taskQuery.taskVariableValueGreaterThanOrEqual(key.replace(GREAT, ""), date);
        } catch (ParseException e) {
            LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
        }
        return taskQuery;
    }

	public static class SearchResult {
		String value;
        String label;

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
}
