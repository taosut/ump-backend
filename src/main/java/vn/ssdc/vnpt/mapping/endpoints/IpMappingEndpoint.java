package vn.ssdc.vnpt.mapping.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.mapping.model.IpMapping;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("ip-mapping")
@Api("IP Mapping")
@Produces(APPLICATION_JSON)
public class IpMappingEndpoint extends SsdcCrudEndpoint<Long, IpMapping> {

    private vn.ssdc.vnpt.mapping.services.IpMappingService ipMappingService;

    @Autowired
    public IpMappingEndpoint(vn.ssdc.vnpt.mapping.services.IpMappingService ipMappingService) {
        this.service = this.ipMappingService = ipMappingService;
    }

    @GET
    @Path("/search-ip-mapping")
    @ApiOperation(value = "Search IP Mapping")
    public List<IpMapping> search(
            @ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
            @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("offset") String indexPage,
            @ApiParam(value = "List of selected parameter") @DefaultValue("0") @QueryParam("whereExp") String whereExp) {
        return this.ipMappingService.search(limit, indexPage, whereExp);
    }

    @GET
    @Path("/count-ip-mapping")
    @ApiOperation(value = "Search IP Mapping")
    public int count(
            @ApiParam(value = "Number of returned devices, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
            @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("offset") String indexPage,
            @ApiParam(value = "List of selected parameter") @DefaultValue("0") @QueryParam("whereExp") String whereExp) {
        return this.ipMappingService.count(limit, indexPage, whereExp);
    }

    @GET
    @Path("/get-ip-by-parent-tree")
    @ApiOperation(value = "Check IP")
    @ApiResponse(code = 200, message = "Success", response = IpMappingEndpoint.class)
    public List<IpMapping> getIPByParentTree(
            @ApiParam(value = "Root Tree") @DefaultValue("") @QueryParam("parentId") String parentId) {
        return this.ipMappingService.getIPByParentTree(parentId);
    }

    @GET
    @Path("/check-ipmapping-exist")
    @ApiOperation(value = "Check IP")
    @ApiResponse(code = 200, message = "Success", response = IpMappingEndpoint.class)
    public boolean checkIPMappingExist(
            @ApiParam(value = "Root Tree") @DefaultValue("") @QueryParam("id") String id) {
        return this.ipMappingService.checkIPMappingExist(id);
    }

//    @GET
//    @Path("/hihi")
//    @ApiOperation(value = "Check IP")
//    @ApiResponse(code = 200, message = "Success", response = IpMappingEndpoint.class)
//    public void addLabel(
//            @QueryParam("deviceId") String deviceId,
//            @QueryParam("ip") String ip) {
//        this.ipMappingService.addLabel(deviceId, ip);
//    }
}
