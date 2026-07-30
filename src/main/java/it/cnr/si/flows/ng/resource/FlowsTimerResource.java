package it.cnr.si.flows.ng.resource;


import com.codahale.metrics.annotation.Timed;

import it.cnr.si.flows.ng.dto.TimerSettings;
import it.cnr.si.flows.ng.service.FlowsTimerService;

import org.activiti.engine.runtime.Job;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


import javax.inject.Inject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;



@Controller
@RequestMapping("api")
public class FlowsTimerResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowsTimerResource.class);

    public static final String BYTES = "bytes";
    @Inject
    private FlowsTimerService flowsTimerService;
    @Inject
    private RestResponseFactory restResponseFactory;

    @RequestMapping(value = "/timer/{processId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId)")
    @Timed
    public ResponseEntity<Object>  getProcessTimers(@PathVariable("processId") String processInstanceId) throws IOException, ParseException {
        List<Job> timerList = flowsTimerService.getTimers(processInstanceId);
        LOGGER.info("timerList.size(): " + timerList.size());
        DataResponse response = new DataResponse();
        response.setSize(timerList.size());// numero di timer restituiti
        response.setData(restResponseFactory.createJobResponseList(timerList));
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/timer/{processId}/{timerId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId)")
    @Timed
    public ResponseEntity<DataResponse> getProcessSingleTimer(@PathVariable("processId") String processInstanceId,
                                                              @PathVariable("timerId") String timerId) throws IOException, ParseException {

        List<Job> timerList = flowsTimerService.getTimer(processInstanceId, timerId);
        LOGGER.info("timerList.size(): " + timerList.size());
        DataResponse response = new DataResponse();
        response.setSize(timerList.size());// numero di timer restituiti
        response.setData(restResponseFactory.createJobResponseList(timerList));
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/timer/setTimerValuesFromNow", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId)")
    @Timed
    public  ResponseEntity<TimerSettings> setTimerValuesFromNow(
            //HttpServletRequest req,
            @RequestBody TimerSettings timer) {


        DataResponse response = new DataResponse();

        try {
            flowsTimerService.setTimerValuesFromNow(timer.getProcessInstanceId(), timer.getTimerId(), timer.getYearAddValue(), timer.getMonthAddValue(), timer.getDayAddValue(), timer.getHourAddValue(), timer.getMinuteAddValue());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //return ResponseEntity.ok(response);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @RequestMapping(value = "/timer/setTimer", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN') OR @permissionEvaluator.canVisualizeTask(#taskId)")
    @Timed
    public  ResponseEntity<TimerSettings> setTimer(
            //HttpServletRequest req,
            @RequestBody TimerSettings timer) {


        DataResponse response = new DataResponse();

        try {
            flowsTimerService.setTimer(timer.getProcessInstanceId(), timer.getTimerId(), timer.getNewDate());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //return ResponseEntity.ok(response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}