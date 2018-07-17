package vn.ssdc.vnpt.user.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("user")
@Api("User")
@Produces(APPLICATION_JSON)
public class UserEndpoint extends SsdcCrudEndpoint<Long, User> {

    @Context
    private HttpServletRequest request;

    private UserService userService;

    @Autowired
    public UserEndpoint(UserService userService) {
        this.service = this.userService = userService;
    }

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page user")
    public Page<User> getPage(@ApiParam(value = "int of page") @DefaultValue("0") @QueryParam("page") int page,
            @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(value = "String of where sql", example = "") @DefaultValue("") @QueryParam("where") String where) {
        String accessToken = request.getHeader("Authorization");
        return userService.getPage(page, limit, where);
    }

    @POST
    @Path("/forgot-password")
    @ApiOperation(value = "Post forgot password")
    public Boolean forgotPassword(@ApiParam(value = "JSON format, keys allow are username", example = "") Map<String, String> request) {
        try {
            String username = request.containsKey("username") ? request.get("username") : "";
            userService.sendForgotPassword(username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //--------
    @POST
    @Path("/change-password-with-token")
    @ApiOperation(value = "Post change password with token")
    public Boolean changePasswordWithToken(@ApiParam(value = "JSON format, keys allow are userId, token, newPassword", example = "") Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.containsKey("userId") ? request.get("userId") : "");
            String token = request.containsKey("token") ? request.get("token") : "";
            String newPassword = request.containsKey("newPassword") ? request.get("newPassword") : "";
            return userService.changePasswordWithToken(userId, token, newPassword);
        } catch (Exception e) {
            return false;
        }
    }
     

    //----------
    @POST
    @Path(("/{email}/forgot-password-with-email"))
    @ApiOperation(value = "Post forgot password with email")
    public Boolean forgotPassword(@ApiParam(value = "String of email", example = "ump@vnpt-technology.vn") @PathParam("email") String email) {
        userService.sendForgotPasswordWithEmail(email);
        return true;
    }   

    @GET
    @Path("/get-by-username/{username}")
    @ApiOperation(value = "Get detail user with user name")
    public User getDetailByUsername(@ApiParam(value = "String of username", example = "ump") @PathParam("username") String username) {
        return userService.findByUserName(username);
    }

    
    //---------
    @POST
    @Path("/{userId}/reset-password")
    @ApiOperation(value = "Post reset password")
    public User resetPassword(@ApiParam(value = "Long of user if", example = "1") @PathParam("userId") Long id) {
        return userService.resetPassword(id);
    }

    @POST
    @Path("/check-current-password")
    @ApiOperation(value = "Post check current password")
    public Boolean checkCurrentPassword(@ApiParam(value = "JSON format, keys allow format username, currentPassword", example = "") Map<String, String> request) {
        try {
            String username = request.containsKey("username") ? request.get("username") : "";
            String currentPassword = request.containsKey("currentPassword") ? request.get("currentPassword") : "";

            if (!username.isEmpty() && !currentPassword.isEmpty()) {
                return userService.checkPassword(username, currentPassword);
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @POST
    @Path("/change-password")
    @ApiOperation(value = "Post change password")
    public User changePassword(@ApiParam(value = "JSON format, keys allow are username, newPassword, currentPassword", example = "") Map<String, String> request) {
        try {
            String username = request.containsKey("username") ? request.get("username") : "";
            String newPassword = request.containsKey("newPassword") ? request.get("newPassword") : "";
            String currentPassword = request.containsKey("currentPassword") ? request.get("currentPassword") : "";

            return userService.changePassword(username, currentPassword, newPassword);
        } catch (Exception e) {
            return null;
        }
    }

    @GET
    @Path("/check-by-role-id/{roleId}")
    @ApiOperation(value = "Get list users with role id")
    public List<User> checkByRoleId(@ApiParam(value = "String of role id", example = "1") @PathParam("roleId") String roleId) {
        return userService.checkByRoleId(roleId);
    }

    @GET
    @Path("/get-user-by-group")
    @ApiOperation(value = "Get list user with device group ids")
    public List<User> findUserByDeviceGroup(@ApiParam(value = "String of group device ids", example = "") @QueryParam("groupId") String groupId) {
        return userService.getListUserByDeviceGroupId(groupId);
    }

}
