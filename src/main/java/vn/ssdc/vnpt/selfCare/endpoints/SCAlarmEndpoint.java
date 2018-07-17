package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.alarm.model.AlarmType;
import vn.ssdc.vnpt.alarm.services.AlarmELKService;
import vn.ssdc.vnpt.alarm.services.AlarmTypeService;
import vn.ssdc.vnpt.selfCare.model.SCAlarmSetting;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmSeachForm;
import vn.ssdc.vnpt.selfCare.model.SCAlarm;

import javax.ws.rs.*;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import vn.ssdc.vnpt.alarm.endpoints.AlarmEndPoint;
import vn.ssdc.vnpt.alarm.model.AlarmGraphs;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCAlarmSettingSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceAlarm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDevice;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceRole;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceUser;

/**
 * Created by THANHLX on 11/22/2017.
 */
@Component
@Path("/self-care/alarms")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Alarms")
public class SCAlarmEndpoint {

    @Autowired
    AlarmELKService alarmELKService;

    @Autowired
    SelfCareServiceDevice selfCareServiceDevice;

    @Autowired
    SelfCareServiceUser selfCareServiceUser;

    @Autowired
    AlarmTypeService alarmTypeService;

    @Autowired
    SelfCareServiceAlarm selfCareServiceAlarm;

    @Autowired
    private ConfigurationService configurationService;

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete alarm from elastic+ by id")
    public void delete(@PathParam("id") String id) {
        alarmELKService.removeAlarm(id);
    }

    @POST
    @Path("/complete/{id}")
    @ApiOperation(value = "Delete alarm from elastic+ by id")
    public void complete(@PathParam("id") String id) {
        alarmELKService.clearAlarm(id);
    }

    /**
     * Search alarms
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search")
    @ApiOperation(value = "Get alarms from elastic+")
    @ApiResponse(code = 200, message = "Success", response = SCAlarm.class)
    public List<SCAlarm> search(@RequestBody SCAlarmSeachForm searchParameter) throws IOException, ParseException {
        Map<String, String> mapParams = convertAlarmSearchForm(searchParameter);
        List<Alarm> listAlarms = new ArrayList<>();
        if (searchParameter.limit == null) {
            listAlarms = alarmELKService.searchAlarm(mapParams, false);
        } else {
            listAlarms = alarmELKService.searchAlarm(mapParams, true);
        }
        List<SCAlarm> listSCAlarms = new ArrayList<>();
        for (Alarm alarm : listAlarms) {
            listSCAlarms.add(new SCAlarm(alarm));
        }
        return listSCAlarms;
    }

    /**
     * Count alarms
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count")
    @ApiOperation(value = "Count alarms from elastic+")
    @ApiResponse(code = 200, message = "Success")
    public int count(@RequestBody SCAlarmSeachForm searchParameter) throws IOException, ParseException {
        Map<String, String> mapParams = convertAlarmSearchForm(searchParameter);
        return alarmELKService.countAlarm(mapParams, true);
    }

    @POST
    @Path("/graph/severity")
    @ApiOperation(value = "Get Data Graph by severity")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<AlarmGraphs> viewGraphBySeverity(@RequestBody SCAlarmSeachForm searchParameter) throws ParseException {
        return this.alarmELKService.viewGraphSeverityAlarmElk(convertAlarmSearchForm(searchParameter));
    }

    @POST
    @Path("/graph/alarm-type")
    @ApiOperation(value = "Get data graph by alarm type")
    @ApiResponse(code = 200, message = "Success", response = AlarmEndPoint.class)
    public List<AlarmGraphs> viewGraphByAlarmType(@RequestBody SCAlarmSeachForm searchParameter) throws ParseException {
        return this.alarmELKService.viewGraphNumberOfAlarmTypeElk(convertAlarmSearchForm(searchParameter));
    }

    public Map<String, String> convertAlarmSearchForm(SCAlarmSeachForm searchParameter) {
        Map<String, String> mapParams = new HashMap<>();
        if (searchParameter.limit != null) {
            if (searchParameter.page == null) {
                searchParameter.page = Integer.valueOf(configurationService.get("page_default").value);
            }
            mapParams.put(AlarmELKService.PAGE_LIMIT, Integer.toString(searchParameter.limit));
            mapParams.put(AlarmELKService.PAGE_INDEX, Integer.toString(searchParameter.page - 1));
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (searchParameter.alarmName != null) {
            mapParams.put(AlarmELKService.PAGE_ALARM_NAME, searchParameter.alarmName);
        }
        if (searchParameter.raisedFrom != null) {
            mapParams.put(AlarmELKService.PAGE_RAISED_FROM, String.valueOf(searchParameter.raisedFrom.getTime()));
        }
        if (searchParameter.raisedTo != null) {
            mapParams.put(AlarmELKService.PAGE_RAISED_TO, String.valueOf(searchParameter.raisedTo.getTime()));
        }
        if (searchParameter.alarmTypeName != null) {
            mapParams.put(AlarmELKService.PAGE_ALARM_TYPE_NAME, searchParameter.alarmTypeName);
        }
        if (searchParameter.deviceGroupIds != null) {
            mapParams.put(AlarmELKService.PAGE_GROUP_FILTER, searchParameter.deviceGroupIds);
        }
        if (searchParameter.device_id != null) {
            mapParams.put(AlarmELKService.PAGE_SERIALNUMBER, searchParameter.device_id);
        }
        if (searchParameter.severity != null) {
            mapParams.put(AlarmELKService.PAGE_SEVERITY, searchParameter.severity);
        }
        if (searchParameter.status != null) {
            mapParams.put(AlarmELKService.PAGE_STATUS, searchParameter.status);
        }
        if (null != searchParameter.userName && !"".equals(searchParameter.userName)) {
            String deviceGroupIds = selfCareServiceUser.getAllDeviceGroupIds(searchParameter.userName).toString().replaceAll("[\"\\[\\]]", "").replaceAll(" ", "");
            mapParams.put(AlarmELKService.PAGE_ROLE_GROUP, deviceGroupIds);
        }

        return mapParams;
    }

    @POST
    @Path("/create-setting/{deviceGroupId}")
    @ApiOperation(value = "Create a new alarm setting")
    @ApiResponse(code = 200, message = "Success", response = SCAlarmSetting.class)
    public SCAlarmSetting create(@PathParam("deviceGroupId") Long deviceGroupId, @RequestBody SCAlarmSetting scPolicyJob) {
        return selfCareServiceAlarm.create(deviceGroupId, scPolicyJob);
    }

    @POST
    @Path("/read-setting/{id}")
    @ApiOperation(value = "Read alarm setting by id")
    public SCAlarmSetting read(@PathParam("id") Long id) {
        return selfCareServiceAlarm.convertToSCAlarmSetting(alarmTypeService.get(id));
    }

    @POST
    @Path("/update-setting/{id}")
    @ApiOperation(value = "Update alarm setting")
    @ApiResponse(code = 200, message = "Success", response = SCAlarmSetting.class)
    public SCAlarmSetting update(@PathParam("id") Long id, @RequestBody SCAlarmSetting scPolicyJob) {
        return selfCareServiceAlarm.update(id, scPolicyJob);
    }

    @POST
    @Path("/delete-setting/{id}")
    @ApiOperation(value = "Delete alarm setting by id")
    public void delete(@PathParam("id") Long id) {
        alarmTypeService.delete(id);
    }

    /**
     * Search policies
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search-setting")
    @ApiOperation(value = "Search alarm setting")
    @ApiResponse(code = 200, message = "Success", response = SCAlarmSetting.class)
    public List<SCAlarmSetting> search(@RequestBody SCAlarmSettingSearchForm searchParameter) {
        return selfCareServiceAlarm.search(searchParameter);
    }

    /**
     * Search policies
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count-setting")
    @ApiOperation(value = "Count alarm setting")
    @ApiResponse(code = 200, message = "Success")
    public int count(@RequestBody SCAlarmSettingSearchForm searchParameter) {
        return selfCareServiceAlarm.count(searchParameter);
    }
}
