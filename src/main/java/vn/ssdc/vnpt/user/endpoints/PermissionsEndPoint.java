package vn.ssdc.vnpt.user.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.user.services.PermissionsService;
import vn.ssdc.vnpt.user.model.Permission;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Lamborgini on 5/4/2017.
 */
@Component
@Path("permissions")
@Api("Permissions")
@Produces(APPLICATION_JSON)
public class PermissionsEndPoint extends SsdcCrudEndpoint<Long, Permission> {

    private PermissionsService permissionsService;

    @Autowired
    public PermissionsEndPoint(PermissionsService permissionsService) {
        this.service = this.permissionsService = permissionsService;
    }

    @GET
    @Path("/search-permission")
    public List<Permission> searchPermission(
            @ApiParam(value = "Number of returned permission, default is 20") @DefaultValue("20") @QueryParam("limit") String limit,
            @ApiParam(value = "Starting index of the returned list, default is 0") @DefaultValue("0") @QueryParam("indexPage") String indexPage) {
        return this.permissionsService.searchPermission(limit,indexPage);
    }

    @GET
    @Path("/check-group-name")
    public int checkGroupName(
            @ApiParam(value = "Check GroupName existed") @DefaultValue("") @QueryParam("addGroupName") String addGroupName,
            @ApiParam(value = "Check Name existed") @DefaultValue("") @QueryParam("addName") String addName){
        return this.permissionsService.checkGroupName(addGroupName,addName);
    }
}
