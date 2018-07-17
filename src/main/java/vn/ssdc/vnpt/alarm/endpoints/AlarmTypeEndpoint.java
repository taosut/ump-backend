package vn.ssdc.vnpt.alarm.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.alarm.model.AlarmConfiguration;
import vn.ssdc.vnpt.alarm.model.AlarmPreset;
import vn.ssdc.vnpt.alarm.model.AlarmType;
import vn.ssdc.vnpt.alarm.services.AlarmTypeService;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by thangnc on 23-May-17.
 */
@Component
@Path("alarm-type")
@Api("AlarmType")
@Produces(APPLICATION_JSON)
public class AlarmTypeEndpoint extends SsdcCrudEndpoint<Long, AlarmType> {
    private static final Logger logger = LoggerFactory.getLogger(AlarmTypeEndpoint.class);
    public static final String PRESET = "ALARM SETTING ";
    private AlarmTypeService alarmTypeService;

    @Autowired
    public AlarmTypeEndpoint(AlarmTypeService service) {
        this.service = this.alarmTypeService = service;
    }

    @Autowired
    AcsClient acsClient;

    @GET
    @Path("/getAlarmByPage")
    @ApiOperation(value = "Get Alarm By Page")
    @ApiResponse(code = 200, message = "Success", response = AlarmTypeEndpoint.class)
    public List<AlarmType> getAlarmByPage(@ApiParam(value = "Starting index of the returned list, default is 1") @DefaultValue("1") @QueryParam("offset") int offset,
                                          @ApiParam(value = "Number of returned role, default is 20") @DefaultValue("20") @QueryParam("limit") int limit,
                                          @ApiParam(value = "User role of device groups") @DefaultValue("") @QueryParam("roleDeviceGroup") String roleDeviceGroup) {
        return alarmTypeService.findByPage(offset, limit, roleDeviceGroup);
    }

    @GET
    @Path("/findAllByRoleDeviceGroup")
    @ApiOperation(value = "Find All By Role Device Group")
    @ApiResponse(code = 200, message = "Success", response = AlarmTypeEndpoint.class)
    public List<AlarmType> findAllByRoleDeviceGroup(
            @ApiParam(value = "User role of device groups") @DefaultValue("") @QueryParam("roleDeviceGroup") String roleDeviceGroup) {
        return alarmTypeService.findAll(roleDeviceGroup);
    }

    @GET
    @Path("/getAlarmByName")
    @ApiOperation(value = "Get Alarm By Name")
    @ApiResponse(code = 200, message = "Success", response = AlarmTypeEndpoint.class)
    public List<AlarmType> getAlarmByName(@ApiParam(value = "Alarm name") @DefaultValue("") @QueryParam("name") String name) {
        return alarmTypeService.findByName(name);
    }

    @GET
    @Path("/search")
    @ApiOperation(value = "Search Alarm")
    @ApiResponse(code = 200, message = "Success", response = AlarmTypeEndpoint.class)
    public List<AlarmType> getAllTask(@ApiParam(value = "Alarm type") @DefaultValue("") @QueryParam("type") String type,
                                      @ApiParam(value = "Alarm name") @DefaultValue("") @QueryParam("name") String name,
                                      @ApiParam(value = "Level of alert") @DefaultValue("") @QueryParam("severity") String severity,
                                      @ApiParam(value = "Device groups") @DefaultValue("") @QueryParam("group") String group,
                                      @ApiParam(value = "Search all by prefix") @DefaultValue("") @QueryParam("prefix") String prefix,
                                      @ApiParam(value = "User role of device groups") @DefaultValue("") @QueryParam("roleDeviceGroup") String roleDeviceGroup) {
        return alarmTypeService.search(type, name, severity, group, prefix, roleDeviceGroup);
    }

    @POST
    public AlarmType doCreate(AlarmType entity) {
        AlarmType alarmType = service.create(entity);
        try {
            if (alarmType.notification != null && alarmType.notification == 1) {
                alarmTypeService.createQuartzJob(alarmType.id, alarmType.timeSettings);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        return alarmType;
    }

    @PUT
    @Path("/{id}")
    public AlarmType doUpdate(@PathParam("id") Long id, AlarmType entity) {
        AlarmType alarmType = service.update(id, entity);
        try {
            alarmTypeService.deleteQuartzJob(id);
            alarmTypeService.deleteTriger(id);
            if (alarmType.notification != null && alarmType.notification == 1) {
                alarmTypeService.createQuartzJob(alarmType.id, alarmType.timeSettings);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        return alarmType;
    }

    @DELETE
    @Path("/{id}")
    public void doDelete(@PathParam("id") Long id) {
        service.delete(id);
        try {
            alarmTypeService.deleteQuartzJob(id);
            alarmTypeService.deleteTriger(id);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }

    public AlarmPreset createAlarmPreset(AlarmType alarmType) {
        AlarmPreset alarmPreset = new AlarmPreset();

        alarmPreset.channel = "";
        alarmPreset.weight = 0;
        alarmPreset.schedule = "";

        Iterator<DeviceGroup> deviceGroups = alarmType.deviceGroups.iterator();
        if(alarmType.deviceGroups.size() == 1) {
            while (deviceGroups.hasNext()) {
                DeviceGroup deviceGroup = deviceGroups.next();
                alarmPreset.precondition = deviceGroup.query;
            }
        } else {
            StringBuilder sb = new StringBuilder("{\"$or\":[");
            while (deviceGroups.hasNext()) {
                DeviceGroup deviceGroup = deviceGroups.next();
                sb.append(String.format(","+deviceGroup.query));
            }
            sb.deleteCharAt(8);
            sb.append("]}");
            alarmPreset.precondition = sb.toString();
        }
        List<AlarmConfiguration> alarmConfigurationList = new LinkedList<>();
        Map<String, String> parameterValues = alarmType.parameterValues;
        for (Map.Entry<String, String> entry : parameterValues.entrySet()) {
            AlarmConfiguration alarmConfiguration = new AlarmConfiguration();
            alarmConfiguration.type = "age";
            alarmConfiguration.name = entry.getKey();
            alarmConfiguration.age = String.valueOf(alarmType.timeSettings*60);
            alarmConfigurationList.add(alarmConfiguration);
        }
        alarmPreset.configurations = alarmConfigurationList;

        Map<String,Boolean> events = new LinkedHashMap<>();
        events.put("2 PERIODIC", true);
        alarmPreset.events = events;

        return alarmPreset;
    }

    @GET
    @Path("/get-alarm-type-by-group")
    @ApiOperation(value = "Get Alarm Type By Group")
    @ApiResponse(code = 200, message = "Success", response = AlarmTypeEndpoint.class)
    public List<AlarmType> findAlarmByDeviceGroup(@ApiParam(value = "Device group id") @DefaultValue("") @QueryParam("groupId") String groupId) {
        return alarmTypeService.getListAlarmByDeviceGroupId(groupId);
    }

}

