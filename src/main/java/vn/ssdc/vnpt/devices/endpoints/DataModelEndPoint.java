package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceTypeVersion;
import vn.ssdc.vnpt.devices.services.DataModelService;
import vn.ssdc.vnpt.dto.AcsResponse;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.io.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("data-model")
@Produces(APPLICATION_JSON)
@Api("DataModels")
public class DataModelEndPoint extends SsdcCrudEndpoint<Long, DeviceTypeVersion> {
    private static final Logger logger = LoggerFactory.getLogger(DataModelEndPoint.class);

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    public DataModelEndPoint(DataModelService dataModelService) {
        this.service = this.dataModelService = dataModelService;
    }

    @GET
    @Path("/smart-create/{deviceId}")
    @ApiOperation(value = "Smart create data model")
    public void createDataModel(@ApiParam(value = "String of Device Id", example = "a06518-968380GERG-VNPT00a532c2") @PathParam("deviceId") String deviceId) {
        // create deviceType
        dataModelService.addDataModel(deviceId);
    }

    @GET
    @Path("/smart-clone/{deviceId}/{cloneDeviceId}")
    @ApiOperation(value = "Smart create data model")
    public void cloneDataModel(@ApiParam(value = "String of Device Id", example = "a06518-968380GERG-VNPT00a532c2") @PathParam("deviceId") String deviceId,
                               @ApiParam(value = "String of Clone Device Id", example = "a06518-968380GERG-VNPT00a532c2") @PathParam("cloneDeviceId") String cloneDeviceId
                               ) {
        // create deviceType
        dataModelService.cloneDataModel(deviceId, cloneDeviceId);
    }

    @GET
    @Path("/export/{deviceTypeVersionId}")
    @ApiOperation(value = "Export data model to xml")
    public Response exportDataModelXML(@ApiParam(value = "Long of device type version id", example = "1") @PathParam("deviceTypeVersionId") Long deviceTypeVersionId) {
        String path = dataModelService.exportDataModelXML(deviceTypeVersionId);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }

    @GET
    @Path("/exportJson/{deviceTypeVersionId}")
    @ApiOperation(value = "Export data model to json")
    public Response exportDataModelJson(@ApiParam(value = "Long of device type version id", example = "1") @PathParam("deviceTypeVersionId") Long deviceTypeVersionId) {
        String path = dataModelService.exportDataModelJson(deviceTypeVersionId);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }
}