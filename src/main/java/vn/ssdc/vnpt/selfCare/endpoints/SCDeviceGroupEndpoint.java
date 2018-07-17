package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceGroupSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDeviceGroup;

/**
 * Created by THANHLX on 11/30/2017.
 */
@Component
@Path("/self-care/device-groups")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Device Groups")
public class SCDeviceGroupEndpoint {

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    private SelfCareServiceDeviceGroup selfCareServiceDeviceGroup;

    //<editor-fold desc="Get All Device Group">
    // TODO list groups
    @GET
    @ApiOperation(value = "Read all device groups")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceGroup.class)
    public List<SCDeviceGroup> getAll() {
        List<DeviceGroup> listDeviceGroups = deviceGroupService.getAll();
        List<SCDeviceGroup> listSCDeviceGroups = new ArrayList<>();
        for (DeviceGroup deviceGroup : listDeviceGroups) {
            listSCDeviceGroups.add(new SCDeviceGroup(deviceGroup));
        }
        return listSCDeviceGroups;
    }
    //</editor-fold>

    @POST
    @ApiOperation(value = "Search device groups", notes = "get 1 group -> truyên vao deviceGroupId \n get nhiều devicegroup thì truyền theo các điều kiện khác")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceGroup.class)
    @Path("/search")
    public List<SCDeviceGroup> search(@RequestBody SCDeviceGroupSearchForm sCDeviceGroupSearchForm) {
        return selfCareServiceDeviceGroup.search(sCDeviceGroupSearchForm);
    }

    @POST
    @ApiOperation(value = "Count total device groups by query")
    @ApiResponse(code = 200, message = "Success", response = Long.class)
    @Path("/count")
    public long count(@RequestBody SCDeviceGroupSearchForm sCDeviceGroupSearchForm) {
        return selfCareServiceDeviceGroup.count(sCDeviceGroupSearchForm);
    }

    @DELETE
    @ApiOperation(value = "delete device groups", notes = " truyen vao deviceGroup ID")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceGroup.class)
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Exception {
        if (!selfCareServiceDeviceGroup.checkDeviceGroupInUsed(id)) {
            deviceGroupService.delete(id);
        }
    }

    @POST
    @ApiOperation(value = "Create new device groups")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceGroup.class)
    public SCDeviceGroup create(@RequestBody SCDeviceGroup scDeviceGroup) throws Exception {
        return selfCareServiceDeviceGroup.create(scDeviceGroup);
    }

    @PUT
    @ApiOperation(value = "update device groups")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceGroup.class)
    @Path("/{id}")
    public SCDeviceGroup update(@PathParam("id") Long id, @RequestBody SCDeviceGroup scDeviceGroup) throws Exception {
        return selfCareServiceDeviceGroup.update(id, scDeviceGroup);
    }

    @POST
    @Path("/check-in-used/{id}")
    @ApiOperation(value = "check device group is in used")
    public boolean checkGroupIsUsed(@PathParam("id") Long id) {
        return selfCareServiceDeviceGroup.checkDeviceGroupInUsed(id);
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "get device group by id")
    @ApiResponse(code = 200, message = "Success", response = SCDeviceGroup.class)
    public SCDeviceGroup getByID(@PathParam("id") long id) throws Exception {
        return selfCareServiceDeviceGroup.convertDeviceGroupToSCDeviceGroup(deviceGroupService.get(id));
    }
}
