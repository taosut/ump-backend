package vn.ssdc.vnpt.alarm.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.services.AlarmDetailsService;
import vn.ssdc.vnpt.alarm.services.AlarmService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import vn.ssdc.vnpt.alarm.model.AlarmGraphs;
import vn.ssdc.vnpt.alarm.services.AlarmELKService;

/**
 * Created by Lamborgini on 5/24/2017.
 */
@Component
@Path("alarm")
@Api("Alarm")
@Produces(APPLICATION_JSON)
public class AlarmEndPoint extends SsdcCrudEndpoint<Long, Alarm> {

    private AlarmService alarmService;
    private AlarmDetailsService alarmDetailsService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private AlarmELKService alarmELKService;

    @Autowired
    public AlarmEndPoint(AlarmService alarmService) {
        this.service = this.alarmService = alarmService;

    }

    @GET
    @ApiOperation(value = "Delete Quartz Job")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    @Path("delete-quartz-job")
    public void deleteQuartzJob() throws SchedulerException {
        String strJob = "Logging User Job";
        JobKey jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        String strTrigger = "Logging User Trigger";
        TriggerKey triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);

        strJob = "Alarm Job";
        jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        strTrigger = "Alarm Trigger";
        triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);

        strJob = "Alarm Detail Job";
        jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        strTrigger = "Alarm Detail Trigger";
        triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);

        strJob = "Monitoring Job";
        jobKey = new JobKey(strJob);
        scheduler.deleteJob(jobKey);

        strTrigger = "Monitoring Trigger";
        triggerKey = new TriggerKey(strTrigger);
        scheduler.unscheduleJob(triggerKey);
    }

    @GET
    @ApiOperation(value = "Processing Value Change")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    @Path("/processing-value-change")
    public boolean processingValueChange(
            @ApiParam(value = "From Date") @DefaultValue("yyyy-MM-dd HH:mm:ss") @QueryParam("fromDateTime") String fromDateTime,
            @ApiParam(value = "To Date") @DefaultValue("yyyy-MM-dd HH:mm:ss") @QueryParam("toDateTime") String toDateTime
    ) throws IOException, ParseException, SchedulerException {
        this.alarmService.monitoringCWMPLog(fromDateTime, toDateTime);
        return true;
    }

    @GET
    @Path("/processing-alarm-detail")
    @ApiOperation(value = "Processing Alarm Details")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public boolean processingAlarmDetail(
            @ApiParam(value = "From Date") @DefaultValue("yyyy-MM-dd HH:mm:ss") @QueryParam("fromDateTime") String fromDateTime,
            @ApiParam(value = "To Date") @DefaultValue("yyyy-MM-dd HH:mm:ss") @QueryParam("toDateTime") String toDateTime
    ) throws IOException, ParseException, SchedulerException {
        this.alarmService.processingAlarmDetail(fromDateTime, toDateTime);
        return true;
    }

    @GET
    @Path("/processing-alarm")
    @ApiOperation(value = "Processing Alarm")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public boolean processingAlarm(
            @ApiParam(value = "From Date") @DefaultValue("yyyy-MM-dd HH:mm:ss") @QueryParam("fromDateTime") String fromDateTime,
            @ApiParam(value = "To Date") @DefaultValue("yyyy-MM-dd HH:mm:ss") @QueryParam("toDateTime") String toDateTime
    ) throws IOException, ParseException, SchedulerException {
        this.alarmService.processAlarm(fromDateTime, toDateTime);
        return true;
    }

    @GET
    @Path("/search-alarm")
    @ApiOperation(value = "Search Alarm")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<Alarm> searchAlarm(
            @ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
            @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("indexPage") String indexPage,
            @ApiParam(value = "List of selected parameter") @DefaultValue("0") @QueryParam("whereExp") String whereExp,
            @QueryParam("deviceGroupIds") String deviceGroupIds) {
        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return this.alarmService.searchAlarm(limit, indexPage, whereExp);
    }

    @POST
    @Path("/search-alarm-elk")
    @ApiOperation(value = "Search Alarm")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<Alarm> searchAlarmElk(Map<String, String> mapParam) throws IOException, ParseException {
        return this.alarmELKService.searchAlarm(mapParam, true);
    }

    @GET
    @Path("/count-alarm")
    @ApiOperation(value = "Count Alarm")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public int countAlarmType(@ApiParam(value = "List of selected parameter") @DefaultValue("") @QueryParam("whereExp") String whereExp,
            @QueryParam("deviceGroupIds") String deviceGroupIds) {
        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return this.alarmService.countAlarm(whereExp);
    }

    @POST
    @Path("/count-alarm-elk")
    @ApiOperation(value = "Count Alarm elk")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public int countAlarmTypeELK(Map<String, String> mapParam) throws IOException, ParseException {
        return this.alarmELKService.searchAlarm(mapParam, false).size();
    }

    @POST
    @Path("/clear-alarm-elk")
    @ApiOperation(value = "Clear Alarm elk")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public Alarm clearAlarm(Map<String, String> mapParam) throws IOException, ParseException {
        return this.alarmELKService.clearAlarm(mapParam.get("id"));
    }

    @POST
    @Path("/remove-alarm-elk")
    @ApiOperation(value = "Clear Alarm elk")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public Alarm removeAlarm(Map<String, String> mapParam) throws IOException, ParseException {
        return this.alarmELKService.removeAlarm(mapParam.get("id"));
    }

    @GET
    @Path("/get-alarm-name-by-alarm-type")
    @ApiOperation(value = "Get Alarm By Alarm Type")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<Alarm> getAlarmNameByAlarmType(@ApiParam(value = "Request String AlarmType") @DefaultValue("") @QueryParam("alarmType") String alarmType) {
        return this.alarmService.getAlarmNameByAlarmType(alarmType);
    }

    @GET
    @Path("/get-alarm-name-by-alarm-type-id")
    @ApiOperation(value = "Get Alarm Name By Alarm Type Id")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<Alarm> getAlarmNameByAlarmType(@ApiParam(value = "Request String AlarmID") @DefaultValue("") @QueryParam("alarmId") Long alarmId) {
        return this.alarmService.getAlarmById(alarmId);
    }

    @GET
    @Path("/view-graph-severity-alarm")
    @ApiOperation(value = "Get Alarm Name By Alarm Type Id")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<Alarm> viewGraphSeverityAlarm(@ApiParam(value = "List of selected parameter") @DefaultValue("") @QueryParam("whereExp") String whereExp,
            @QueryParam("deviceGroupIds") String deviceGroupIds) {
        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return this.alarmService.viewGraphSeverityAlarm(whereExp);
    }

    @POST
    @Path("/view-graph-severity-alarm-elk")
    @ApiOperation(value = "Get Alarm Name By Alarm Type Id")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<AlarmGraphs> viewGraphSeverityAlarmElk(Map<String, String> mapParam) throws ParseException {
//        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return this.alarmELKService.viewGraphSeverityAlarmElk(mapParam);
    }

    @GET
    @Path("/view-graph-number-of-alarm-type")
    @ApiOperation(value = "View graph number of alarm type")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<Alarm> viewGraphNumberOfAlarmType(@ApiParam(value = "List of selected parameter") @DefaultValue("") @QueryParam("whereExp") String whereExp,
            @QueryParam("deviceGroupIds") String deviceGroupIds) {
        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return this.alarmService.viewGraphNumberOfAlarmType(whereExp);
    }

    @POST
    @Path("/view-graph-number-of-alarm-type-elk")
    @ApiOperation(value = "View graph number of alarm type")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<AlarmGraphs> viewGraphNumberOfAlarmTypeElk(Map<String, String> mapParam) throws ParseException {
//        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return this.alarmELKService.viewGraphNumberOfAlarmTypeElk(mapParam);
    }

    private String addQueryDeviceGroup(String whereExp, String deviceGroupIds) {

        if (deviceGroupIds.contains(",")) {
            String temp = "";
            String[] groups = deviceGroupIds.split(",");
            for (int i = 0; i < groups.length; i++) {
                if (i > 0) {
                    temp += " or device_groups like '%\"id\":" + groups[i].replaceAll("\\s+", "") + ",%'";
                } else {
                    temp += " device_groups like '%\"id\":" + groups[i].replaceAll("\\s+", "") + ",%'";
                }
            }
            if (!whereExp.isEmpty()) {
                whereExp += " and (" + temp + ")";
            } else {
                whereExp += temp;
            }

        } else {
            if (!whereExp.isEmpty()) {
                whereExp += " and device_groups like '%\"id\":" + deviceGroupIds + ",%'";
            } else {
                whereExp += " device_groups like '%\"id\":" + deviceGroupIds + ",%'";
            }

        }
        return whereExp;
    }
}
