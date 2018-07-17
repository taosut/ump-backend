package vn.ssdc.vnpt.devices.endpoints;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.ParameterDetail;
import vn.ssdc.vnpt.devices.services.ParameterDetailService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("parameter-detail")
@Produces(APPLICATION_JSON)
@Api("ParameterDetails")
public class ParameterDetailEndpoint extends SsdcCrudEndpoint<Long, ParameterDetail> {

    private ParameterDetailService parameterDetailService;

    @Autowired
    public ParameterDetailEndpoint(ParameterDetailService parameterDetailService) {
        this.service = this.parameterDetailService = parameterDetailService;
    }

    @GET
    @Path("/find-by-device-type-version/{deviceTypeVersionId}")
    public List<ParameterDetail> findByDeviceTypeVersion(@PathParam("deviceTypeVersionId") Long deviceTypeVersionId) {
        return parameterDetailService.findByDeviceTypeVersion2(deviceTypeVersionId);
    }

    @GET
    @Path("/find-by-params")
    public ParameterDetail findByDeviceTypeVersion(@QueryParam("deviceTypeVersionId") Long deviceTypeVersionId,
                                                   @QueryParam("path") String path) {
        return parameterDetailService.findByParams(path, deviceTypeVersionId);
    }

    @GET
    @Path("/find-parameters")
    public List<ParameterDetail> findParameters() {
        return parameterDetailService.findParameters();
    }

    @GET
    @Path("/find-by-tr069-name")
    public List<ParameterDetail> findByTr069Name(@QueryParam("tr069Name") String tr069Name) {
        return parameterDetailService.findByTr069name(tr069Name);
    }

    @GET
    @Path("/delete-by-device-type-version-id")
    public void deleteByDeviceTypeVersionId(@QueryParam("deviceTypeVersionId") @DefaultValue("0") Long deviceTypeVersionId) {
        parameterDetailService.deleteByDeviceTypeVersionId(deviceTypeVersionId);
    }

    @POST
    @Path("/create-new-parameter")
    public ParameterDetail createNewParameter(Map<String, String> request) {

        ParameterDetail response = new ParameterDetail();

        Long deviceTypeVersionId = Long.valueOf(request.getOrDefault("deviceTypeVersionId", null));
        Long tagId = Long.valueOf(request.getOrDefault("tagId", null));
        String path = request.getOrDefault("path", null);
        String defaultValue = request.getOrDefault("defaultValue", "");
        String dataType = request.getOrDefault("dataType", "string");
        String access = request.getOrDefault("access", "true");
        if (deviceTypeVersionId != null && tagId != null && path != null) {
            response = parameterDetailService.createNewParameter(deviceTypeVersionId, tagId, path, defaultValue, dataType, access);
        }

        return response;
    }

}
