package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.Device;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by thangnc on 06-Feb-17.
 */
@Component
@Path("device-group")
@Api("DeviceGroups")
@Produces(APPLICATION_JSON)
public class DeviceGroupEndpoint extends SsdcCrudEndpoint<Long, DeviceGroup> {
    private DeviceGroupService deviceGroupService;

    @Autowired
    public DeviceGroupEndpoint(DeviceGroupService service) {
        this.service = this.deviceGroupService = service;
    }

    @GET
    @Path("/find-by-name")
    public List<DeviceGroup> findByName(@QueryParam("name") String name) {
        return this.deviceGroupService.findByName(name);
    }

    @GET
    @Path("/find-by-page")
    public List<DeviceGroup> getAllTask(@ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
                                        @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("offset") String indexPage,
                                        @ApiParam(value = "List of selected parameter") @DefaultValue("0") @QueryParam("whereExp") String whereExp,
                                        @QueryParam("deviceGroupIds") String deviceGroupIds) {
        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return deviceGroupService.findByPage(limit, indexPage, whereExp);
    }

    @GET
    @Path("/count-by-page")
    public int countAllTask(@ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
                                        @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("offset") String indexPage,
                                        @ApiParam(value = "List of selected parameter") @DefaultValue("0") @QueryParam("whereExp") String whereExp,
                                        @QueryParam("deviceGroupIds") String deviceGroupIds) {
        whereExp = addQueryDeviceGroup(whereExp, deviceGroupIds);
        return deviceGroupService.countAllTask(whereExp);
    }

    @GET
    @Path("/find-by-group-id")
    public List<Device> getListDeviceByGroup(@QueryParam("deviceGroupId") String deviceGroupId) {
        return deviceGroupService.getAllListDeviceByGroup(Long.valueOf(deviceGroupId));
    }

    @GET
    @Path("/find-all")
    public List<DeviceGroup> findAllByDeviceGroupIds(@QueryParam("deviceGroupIds") String deviceGroupIds) {
        return deviceGroupService.findAllByDeviceGroupIds(deviceGroupIds);
    }

    @GET
    @Path("/build-mongo-query")
    public String buildMongoQuery(@QueryParam("manufacturer") String manufacturer,
                                  @QueryParam("modelName") String modelName,
                                  @QueryParam("firmwareVersion") String firmwareVersion,
                                  @QueryParam("label") String label) {
        DeviceGroup deviceGroup = new DeviceGroup();
        deviceGroup.manufacturer = manufacturer;
        deviceGroup.modelName = modelName;
        deviceGroup.firmwareVersion = firmwareVersion;
        deviceGroup.label = label;
        return deviceGroupService.buildMongoQuery(deviceGroup, false);
    }

    @GET
    @Path("/check-by-id")
    public boolean checkLabel(@QueryParam("labelId") String labelId) {
        return this.deviceGroupService.checkLabel(labelId);
    }

    private String addQueryDeviceGroup(String whereExp, String deviceGroupIds) {
        if (!whereExp.isEmpty()) {
            whereExp += " and id IN (" + deviceGroupIds + ")";
        } else {
            whereExp += " id IN (" + deviceGroupIds + ")";
        }
        return whereExp;
    }

}
