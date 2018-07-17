package vn.ssdc.vnpt.provisioning.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.provisioning.services.ProvisioningService;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("provisioning")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api("Provisioning")
public class ProvisioningEndPoint {

    private static final Logger logger = LoggerFactory.getLogger(ProvisioningEndPoint.class);
    @Autowired
    private ProvisioningService provisioningService;

    @GET
    @Path("/createProvisioningTasks/{deviceId}")
    @ApiOperation(value = "Create provisioning tasks")
    public void createProvisioningTasks(@ApiParam(value = "String of device id", example = "a06518-968380GERG-VNPT00a532c2") @PathParam("deviceId") String deviceId) {
        provisioningService.createProvisioningTasks(deviceId);
    }
}
