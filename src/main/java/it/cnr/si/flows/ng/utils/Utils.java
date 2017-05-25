package it.cnr.si.flows.ng.utils;

import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskInfoQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class Utils {

    public static final String PROCESS_PARAMS = "processParams";
    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    public static final String ALL_PROCESS_INSTANCES = "all";
    public static final String LESS = "Less";
    public static final String ERRORE_NELLA_LETTURE_DELLO_STREAM_DELLA_REQUEST = "Errore nella letture dello stream della request";
    private static final String TASK_PARAMS = "taskParams";
    private static final String GREAT = "Great";
    private static final String ERRORE_NEL_PARSING_DELLA_DATA = "Errore nel parsing della data {} - ";
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    @Autowired
    static RestResponseFactory restResponseFactory;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static boolean isEmpty(String in) {
        return in == null || in.equals("");
    }

    public static boolean isNotEmpty(String in) {
        return !isEmpty(in);
    }

    //    todo: rifattorizzare metodi in FlowsProcessDefinitionsResource, FlowsProcessInstanceResource, FlowsTaskResource
    public static ResponseEntity<DataResponse> getDataResponseResponseEntity(List<Task> listraw) {
        List<TaskResponse> list = restResponseFactory.createTaskResponseList(listraw);

        DataResponse response = new DataResponse();
        response.setStart(0);
        response.setSize(list.size());
        response.setTotal(list.size());
        response.setData(list);

        return ResponseEntity.ok(response);
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

    public TaskInfoQuery extractSearchParams(HttpServletRequest req, TaskInfoQuery query) {
        try {
            JSONObject json = new JSONObject(IOUtils.toString(req.getReader()));

            if (json.has(PROCESS_PARAMS))
                query = extractProcessSearchParams(query, json.getJSONArray(PROCESS_PARAMS));
            if (json.has(TASK_PARAMS))
                query = extractTaskSearchParams(query, json.getJSONArray(TASK_PARAMS));
        } catch (Exception e) {
            LOGGER.error(ERRORE_NELLA_LETTURE_DELLO_STREAM_DELLA_REQUEST, e);
        }
        return query;
    }

    public TaskInfoQuery extractProcessSearchParams(TaskInfoQuery taskQuery, JSONArray params) {

        for (int i = 0; i < params.length(); i++) {
            JSONObject appo = params.optJSONObject(i);
            String key = appo.getString("key");
            String value = appo.getString("value");
            String type = appo.getString("type");
            //wildcard ("%") di default ma non a TUTTI i campi
            switch (type) {
                case "textEqual":
                    taskQuery.processVariableValueEquals(key, value);
                    break;
                case "boolean":
                    // gestione variabili booleane
                    taskQuery.processVariableValueEquals(key, Boolean.valueOf(value));
                    break;
                case "date":
                    taskQuery = historicProcesskDate(taskQuery, key, value);
                    break;
                default:
                    //variabili con la wildcard  (%value%)
                    taskQuery.processVariableValueLikeIgnoreCase(key, "%" + value + "%");
                    break;
            }
        }
        return taskQuery;
    }

    public TaskInfoQuery extractTaskSearchParams(TaskInfoQuery taskQuery, JSONArray taskParams) {

        for (int i = 0; i < taskParams.length(); i++) {
            JSONObject appo = taskParams.optJSONObject(i);
            String key = appo.getString("key");
            String value = appo.getString("value");
            String type = appo.getString("type");

            //solo per le HistoricTaskInstanceQuery si fa la query in base alla data di completamento del task
            if (taskQuery instanceof HistoricTaskInstanceQuery) {
                try {
                    if (key.equals("taskCompletedGreat")) {
                        ((HistoricTaskInstanceQuery) taskQuery).taskCompletedAfter(sdf.parse(value));
                        break;
                    }
                    if (key.equals("taskCompletedLess")) {
                        ((HistoricTaskInstanceQuery) taskQuery).taskCompletedBefore(sdf.parse(value));
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
                    case "textEqual":
                        taskQuery.taskVariableValueEquals(key, value);
                        break;
                    case "boolean":
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

    private TaskInfoQuery historicProcesskDate(TaskInfoQuery taskQuery, String key, String value) {
        try {
            Date date = sdf.parse(value);

            if (key.contains(LESS)) {
                taskQuery.processVariableValueLessThanOrEqual(key.replace(LESS, ""), date);
            } else if (key.contains(GREAT))
                taskQuery.processVariableValueGreaterThanOrEqual(key.replace(GREAT, ""), date);
        } catch (ParseException e) {
            LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
        }
        return taskQuery;
    }

    private TaskInfoQuery historicTaskDate(TaskInfoQuery taskQuery, String key, String value) {
        try {
            Date date = sdf.parse(value);

            if (key.contains(LESS)) {
                taskQuery.taskVariableValueLessThanOrEqual(key.replace(LESS, ""), date);
            } else if (key.contains(GREAT))
                taskQuery.taskVariableValueGreaterThanOrEqual(key.replace(GREAT, ""), date);
        } catch (ParseException e) {
            LOGGER.error(ERRORE_NEL_PARSING_DELLA_DATA, value, e);
        }
        return taskQuery;
    }
}
