package vn.ssdc.vnpt.reports.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.reports.model.DeviceTempBirt;
import vn.ssdc.vnpt.reports.services.DeviceTempBirtService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("report")
@Api("report")
@Produces(APPLICATION_JSON)
public class DeviceTempBirtEndpoints extends SsdcCrudEndpoint<Long, DeviceTempBirt> {

    private DeviceTempBirtService deviceTempBirtService;

    @Autowired
    public DeviceTempBirtEndpoints(DeviceTempBirtService deviceTempBirtService) {
        this.service = this.deviceTempBirtService = deviceTempBirtService;
    }

    @GET
    @Path("/mergeDevice")
    public String checkName() {
        deviceTempBirtService.createDeviceTempBirt();
        return "200";
    }
}
