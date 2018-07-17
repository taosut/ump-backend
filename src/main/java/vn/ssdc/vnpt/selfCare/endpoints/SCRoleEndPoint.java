/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;
import vn.ssdc.vnpt.selfCare.model.SCRole;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceRole;
import vn.ssdc.vnpt.user.model.Role;
import vn.ssdc.vnpt.user.services.RoleService;

/**
 *
 * @author Admin
 */
@Component
@Path("/self-care/roles")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care roles")
public class SCRoleEndPoint {

    @Autowired
    public RoleService roleService;

    @Autowired
    public SelfCareServiceRole selfCareServiceRole;

    @GET
    @ApiOperation(value = "Read all roles")
    @ApiResponse(code = 200, message = "Success", response = SCRole.class)
    public List<SCRole> getAll() {
        List<Role> roles = roleService.getAll();
        List<SCRole> scRoles = new ArrayList<>();
        for (Role role : roles) {
            scRoles.add(selfCareServiceRole.convertRoleToSCRole(role));
        }
        return scRoles;
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "get role by id")
    @ApiResponse(code = 200, message = "Success", response = SCRole.class)
    public SCRole getByID(@PathParam("id") long id) throws Exception {
        return selfCareServiceRole.convertRoleToSCRole(roleService.get(id));
    }

}
