package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.model.Tag;
import vn.ssdc.vnpt.devices.services.DeviceTypeVersionService;
import vn.ssdc.vnpt.devices.services.TagService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by vietnq on 11/1/16.
 */
@Component
@Path("device-type-versions")
@Produces(APPLICATION_JSON)
@Api("DeviceTypeVersions")
public class DeviceTypeVersionEndpoint extends SsdcCrudEndpoint<Long, DeviceTypeVersion> {
    private TagService tagService;
    private DeviceTypeVersionService deviceTypeVersionService;

    @Autowired
    public DeviceTypeVersionEndpoint(DeviceTypeVersionService deviceTypeVersionService,
                                     TagService tagService) {
        this.service = this.deviceTypeVersionService = deviceTypeVersionService;
        this.tagService = tagService;
    }

    @GET
    @Path("/{id}/tags")
    public List<Tag> findTags(@ApiParam(value = "Tag id") @PathParam("id") Long id) {
        return tagService.findByDeviceTypeVersion(id);
    }

    @GET
    @Path("/{id}/assigned-tags")
    public List<Tag> findAssignedTags(@ApiParam(value = "Assigned Tag id") @PathParam("id") Long id) {
        return tagService.findAssignedTags(id);
    }

    @POST
    @Path("/{id}/tags")
    public List<Tag> assignTags(@ApiParam(value = "Tag id") @PathParam("id") Long id, List<Long> tagsID) {
        List<Tag> tags = new ArrayList<Tag>();
        for (Long tagID : tagsID) {
            Tag tag = tagService.get(tagID);
            tag.assigned = 1;
            tagService.update(tagID, tag);
            tags.add(tag);
        }
        return tags;
    }

    @GET
    @Path("/find-by-pk")
    public DeviceTypeVersion findByPk(@ApiParam(value = "DeviceTypeId of device") @QueryParam("deviceTypeId") Long deviceTypeId,
                                      @ApiParam(value = "Version of device") @QueryParam("version") String firmwareVersion) {

        return this.deviceTypeVersionService.findByPk(deviceTypeId, firmwareVersion);

    }

    @GET
    @Path("/find-by-firmware-version")
    public DeviceTypeVersion findByFirmwareVersion(@ApiParam(value = "FirmwareVersion of device") @QueryParam("firmwareVersion") String firmwareVersion) {
        return this.deviceTypeVersionService.findByFirmwareVersion(firmwareVersion);
    }

    @GET
    @Path("/find-by-manufacturer")
    public List<DeviceTypeVersion> findByManufacturer(@ApiParam(value = "Manufacturer of device") @QueryParam("manufacturer") String manufacturer) {
        return this.deviceTypeVersionService.findByManufacturer(manufacturer);
    }

    @GET
    @Path("/find-by-device")
    public DeviceTypeVersion findByDevice(@ApiParam(value = "DeviceId") @QueryParam("deviceId") String deviceId) {
        return this.deviceTypeVersionService.findbyDevice(deviceId);
    }

    @GET
    @Path("/search-devices")
    public List<DeviceTypeVersion> searchDevices(
            @ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
            @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("indexPage") String indexPage,
            @ApiParam(value = "DeviceTypeId of device") @DefaultValue("") @QueryParam("deviceTypeId") String deviceTypeId) {
        return this.deviceTypeVersionService.searchDevices(limit, indexPage, deviceTypeId);
    }

    @POST
    @Path("/get-device-type-id-for-sort-and-search")
    public List<DeviceTypeVersion> getDeviceTypeIDForSortAndSearch(
            @ApiParam(value = "Include manufacturer,modelName") Map<String, String> requestParam,
            @ApiParam(value = "Sorting option, in mongodb syntax") @DefaultValue("created:-1") @QueryParam("sort") String sort,
            @ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
            @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("indexPage") String indexPage) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        return this.deviceTypeVersionService.getDeviceTypeIDForSortAndSearch(manufacturer, modelName, sort, limit, indexPage);
    }

    @POST
    @Path("/count-device-type-id-for-sort-and-search")
    public int countDeviceTypeIDForSortAndSearch(
            @ApiParam(value = "Include manufacturer,modelName") Map<String, String> requestParam,
            @ApiParam(value = "Sorting option, in mongodb syntax") @DefaultValue("created:-1") @QueryParam("sort") String sort,
            @ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
            @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("indexPage") String indexPage) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        return this.deviceTypeVersionService.countDeviceTypeIDForSortAndSearch(manufacturer, modelName, sort, limit, indexPage);
    }

    //unused
    @GET
    @Path("/find-all-device-type-id")
    public Map<String, Long> getWithDeviceTypeId() {
        return this.deviceTypeVersionService.generateDeviceTypeVersionWithDeviceId();
    }

    @GET
    @Path("/get-page")
    public Page<DeviceTypeVersion> getPage(
            @ApiParam(value = "Starting index of the returned list, default is 0") @QueryParam("page") @DefaultValue("0") int page,
            @ApiParam(value = "Number of returned devices, default is 20") @QueryParam("limit") @DefaultValue("20") int limit) {
        return this.deviceTypeVersionService.getPage(page, limit);
    }

    @POST
    @Path("/find-by-manufacturer-and-modelName")
    public List<DeviceTypeVersion> findByManufacturerAndModelName(
            @ApiParam(value = "Include manufacturer,modelName") Map<String, String> requestParam) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        return this.deviceTypeVersionService.findByManufacturerAndModelName(manufacturer, modelName);
    }

    @POST
    @Path("/find-by-manu-and-model-and-firm")
    public List<DeviceTypeVersion> findByManufacturerAndModelNameAndFirmware(
            @ApiParam(value = "Include manufacturer,modelName,firmware") Map<String, String> requestParam) {
        String manufacturer = requestParam.get("manufacturer");
        String modelName = requestParam.get("modelName");
        String firmware = requestParam.get("firmware");
        return this.deviceTypeVersionService.findByManufacturerAndModelNameAndFrimware(manufacturer, modelName, firmware);
    }

    @POST
    @Path("/pingDevice")
    public String pingDevice(@ApiParam(value = "Ping ipDevice") Map<String, String> requestParam) {
        return this.deviceTypeVersionService.pingDevice(requestParam.get("ipDevice"));
    }

    @GET
    @Path("/find-by-list-deviceGroup")
    public List<DeviceTypeVersion> findByListDeviceGroup(@ApiParam(value = "List device groups id") @DefaultValue("") @QueryParam("listDeviceGroup") String listDeviceGroup) {
        return this.deviceTypeVersionService.findByListDeviceGroup(listDeviceGroup);
    }

    @GET
    @Path("/get-list-by-device-group-id")
    public List<DeviceTypeVersion> getListByDeviceGroupId(@ApiParam(value = "Device group id") @DefaultValue("") @QueryParam("deviceGroupId") Long deviceGroupId) {
        return this.deviceTypeVersionService.getListByDeviceGroupId(deviceGroupId);
    }

    @GET
    @Path("/find-by-oui-and-productClass")
    public String findByOUIAndProductClass(
            @QueryParam("productClass") String productClass,
            @QueryParam("oui") String oui,
            @QueryParam("version") String version) {
        return this.deviceTypeVersionService.findByOUIAndProductClass(oui, productClass, version);
    }

    @POST
    @Path("/{deviceTypeVersionId}/copy")
    @ApiOperation(value = "Copy data model")
    public DeviceTypeVersion copy(@ApiParam(value = "Long of device type version id", example = "1") @PathParam("deviceTypeVersionId") Long deviceTypeVersionId,
                                  Map<String, String> params) {
        return deviceTypeVersionService.copy(deviceTypeVersionId, params.get("firmwareVersion"));
    }

}
