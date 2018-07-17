package vn.ssdc.vnpt.performance.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.alarm.endpoints.AlarmTypeEndpoint;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.performance.model.*;
import vn.ssdc.vnpt.performance.sevices.PerformanceSettingService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.File;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by thangnc on 21-Jun-17.
 */
@Component
@Path("performance")
@Api("Performance")
@Produces(APPLICATION_JSON)
public class PerformanceSettingEndpoint extends SsdcCrudEndpoint<Long,PerformanceSetting> {

    public static final String PRESET = "PERFORMANCE SETTING ";

    private static final Logger logger = LoggerFactory.getLogger(PerformanceSettingEndpoint.class);

    @Autowired
    private PerformanceSettingService performanceSettingService;

    @Autowired
    public PerformanceSettingEndpoint(PerformanceSettingService service) {
        this.service = service;
    }


    @POST
    public PerformanceSetting doCreate(PerformanceSetting entity) {
        PerformanceSetting performanceSetting = service.create(entity);
        try {
            performanceSettingService.createQuartzJob(performanceSetting);
            performanceSettingService.createQuartzJobRefreshParameter(performanceSetting);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        return performanceSetting;
    }

    @PUT
    @Path("/{id}")
    public PerformanceSetting doUpdate(@ApiParam(value = "Performance traffic id") @PathParam("id") Long id, PerformanceSetting entity) {
        PerformanceSetting performanceSetting = service.update(id,entity);
        try {
            performanceSettingService.deleteQuartzJob(performanceSetting.id);
            performanceSettingService.deleteTriger(performanceSetting.id);
            performanceSettingService.createQuartzJob(performanceSetting);

            performanceSettingService.deleteQuartzJobRefreshParameter(performanceSetting.id);
            performanceSettingService.deleteTrigerRefreshParameter(performanceSetting.id);
            performanceSettingService.createQuartzJobRefreshParameter(performanceSetting);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        return performanceSetting;
    }

    @DELETE
    @Path("/{id}")
    public void doDelete(@ApiParam(value = "Performance traffic id") @PathParam("id") Long id) {
        try {
            performanceSettingService.deleteQuartzJob(id);
            performanceSettingService.deleteTriger(id);

            performanceSettingService.deleteQuartzJobRefreshParameter(id);
            performanceSettingService.deleteTrigerRefreshParameter(id);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
        service.delete(id);
    }

    @GET
    @Path("/getPerformanceByPage")
    @ApiOperation(value = "Get Performance By Page")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public List<PerformanceSetting> getPerformanceByPage(
            @ApiParam(value = "Starting index of the returned list, default is 1") @DefaultValue("1") @QueryParam("offset") int offset,
            @ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(value = "Device group of user") @QueryParam("roleDeviceGroup") @DefaultValue("") String roleDeviceGroup,
            @ApiParam(value = "Device ID") @DefaultValue("") @QueryParam("deviceID") String deviceID) {
        return performanceSettingService.findByPage(offset, limit, roleDeviceGroup, deviceID);
    }

    @GET
    @Path("/getPerformanceSettingByName")
    @ApiOperation(value = "Get PerformanceSetting By Name")
    @ApiResponse(code = 200, message = "Success", response = AlarmTypeEndpoint.class)
    public List<PerformanceSetting> getPerformanceSettingByName(@ApiParam(value = "PerformanceSetting name") @DefaultValue("") @QueryParam("name") String name) {
        return performanceSettingService.findByName(name);
    }

    @GET
    @Path("/search")
    @ApiOperation(value = "Search Performance Setting")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public List<PerformanceSetting> searchPerformance(
        @ApiParam(value = "Traffic of performance : Received, Transmitted") @DefaultValue("") @QueryParam("traffic") String traffic,
        @ApiParam(value = "Device ID") @DefaultValue("") @QueryParam("deviceID") String deviceID,
        @ApiParam(value = "Start date of performance") @DefaultValue("") @QueryParam("startDate") String startDate,
        @ApiParam(value = "End date of performance") @DefaultValue("") @QueryParam("endDate") String endDate,
        @ApiParam(value = "Search all performance") @DefaultValue("") @QueryParam("prefix") String prefix,
        @ApiParam(value = "Device group of user") @DefaultValue("") @QueryParam("roleDeviceGroup") String roleDeviceGroup) {
        return performanceSettingService.search(traffic, deviceID, startDate, endDate, prefix, roleDeviceGroup);
    }

    @GET
    @Path("/findAllByRoleDeviceGroup")
    @ApiOperation(value = "Find All By Role Device Group")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public List<PerformanceSetting> findAllByRoleDeviceGroup(
        @ApiParam(value = "Device group of user") @DefaultValue("") @QueryParam("roleDeviceGroup") String roleDeviceGroup,
        @ApiParam(value = "Device ID") @DefaultValue("") @QueryParam("deviceID") String deviceID) {
        return performanceSettingService.findAll(roleDeviceGroup, deviceID);
    }

    @GET
    @Path("/searchPerformanceStatitics")
    @ApiOperation(value = "Search Performance Statitics")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public List<PerformanceStatiticsELK> searchPerformanceStatitics(
        @ApiParam(value = "Device id in mongodb") @QueryParam("deviceId") String deviceId,
        @ApiParam(value = "PerformanceSettingId in mysql db") @QueryParam("performanceSettingId") String performanceSettingId,
        @ApiParam(value = "Start date of performance") @QueryParam("startDate") String startDate,
        @ApiParam(value = "End date of performance") @QueryParam("endDate") String endDate) {
        return performanceSettingService.searchPerformanceStatitics(deviceId, performanceSettingId, startDate, endDate);
    }

    @POST
    @Path("/deleteStatiticsInterface")
    @ApiOperation(value = "Delete Performance Statitics")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public boolean deleteStatiticsInterface(
        @ApiParam(value = "Include deviceId of device, performanceSettingId, stasticsInterface (interface of traffic)") Map<String, String> request) {
        return performanceSettingService.deleteStatiticsInterface(request.get("deviceId"),
                request.get("performanceSettingId"), request.get("stasticsInterface"));
    }

    @GET
    @Path("/get-performance-by-group")
    @ApiOperation(value = "Get Performance By Group")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public List<PerformanceSetting> findPerformanceByDeviceGroup(
        @ApiParam(value = "GroupId of device") @QueryParam("groupId") String groupId) {
        return performanceSettingService.getListPerformanceByDeviceGroupId(groupId);
    }

    @GET
    @Path("/exportExcel/{deviceGroupId}/{performanceSettingId}/{type}/{startTime}/{endTime}/{wanMode}/{manufacturer}/{modelName}/{serialNumber}/{monitoring}/{parameterNames}")
    @ApiOperation(value = "Export Excel Performance")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public Response exportExcel(
        @ApiParam(value = "GroupId of performance") @PathParam("deviceGroupId") String deviceGroupId,
        @ApiParam(value = "Id of performance") @PathParam("performanceSettingId") String performanceSettingId,
        @ApiParam(value = "Traffic Statistic of performance") @PathParam("type") String type,
        @ApiParam(value = "Start date of performance") @PathParam("startTime") String startTime,
        @ApiParam(value = "End date of performance") @PathParam("endTime") String endTime,
        @ApiParam(value = "Traffic type of performance ") @PathParam("wanMode") String wanMode,
        @ApiParam(value = "Manufacturer of single device") @PathParam("manufacturer") String manufacturer,
        @ApiParam(value = "ModelName of single device") @PathParam("modelName") String modelName,
        @ApiParam(value = "SerialNumber of single device") @PathParam("serialNumber") String serialNumber,
        @ApiParam(value = "Monitoring CPE of performance") @PathParam("monitoring") String monitoring,
        @ApiParam(value = "Monitoring CPE of performance") @PathParam("parameterNames") String parameterNames) {
        String path = performanceSettingService.exportExcel(deviceGroupId, performanceSettingId, type,
                startTime.replace("T"," "), endTime.replace("T"," "),
                wanMode, manufacturer, modelName, serialNumber, monitoring);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }

    @GET
    @Path("/excute/{performanceSettingId}")
    @ApiOperation(value = "Excute statatics data")
    @ApiResponse(code = 200, message = "Success", response = PerformanceSettingEndpoint.class)
    public void excute(
            @ApiParam(value = "Id of performance") @PathParam("performanceSettingId") Long performanceSettingId,
            @ApiParam(value = "Include startTime & endTime")Map<String, String> request){
        performanceSettingService.statiticsData(performanceSettingId, request.get("startTime"), request.get("endTime"));
    }

//    @GET
//    @Path("/test")
//    public void test(@QueryParam("deviceId") String deviceId,
//                     @QueryParam("parameter_names") String parameter_names,
//                     @QueryParam("value_changes") String value_changes,
//                     @QueryParam("performanceSettingId") String performanceSettingId) {
//        performanceSettingService.createStatiticsELK(deviceId, parameter_names.split(","), value_changes.split(","), performanceSettingId);
//    }

}
