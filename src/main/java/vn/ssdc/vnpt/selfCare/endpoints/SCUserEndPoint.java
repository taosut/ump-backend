/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.selfCare.model.SCUser;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCUserSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceUser;
import vn.ssdc.vnpt.umpexception.UserNotFoundException;
import vn.ssdc.vnpt.user.model.User;
import vn.ssdc.vnpt.user.services.UserService;
import vn.vnpt.ssdc.jdbc.exceptions.EntityNotFoundException;

/**
 *
 * @author kiendt
 */
@Component
@Path("/self-care/users")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Users")
public class SCUserEndPoint {

    @Autowired
    public SelfCareServiceUser selfCareServiceUser;

    @Autowired
    public UserService userService;

    @POST
    @Path("/search")
    @ApiOperation(value = "Search user by usersearchForm")
    public List<SCUser> search(@RequestBody SCUserSearchForm scUserSearchForm) {
        return selfCareServiceUser.search(scUserSearchForm);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "Count user by usersearchForm")
    public long count(@RequestBody SCUserSearchForm scUserSearchForm) {
        return selfCareServiceUser.count(scUserSearchForm);
    }

    @GET
    @Path("/{userName}")
    @ApiOperation(value = "Get user by username")
    public SCUser getByUsername(@PathParam("userName") String userName) throws EntityNotFoundException {
        try {
            User user = userService.findByUserName(userName);
            if (user != null) {
                return selfCareServiceUser.convertFromUserToSCUser(user);
            }
        } catch (EntityNotFoundException e) {
            throw e;
        }
        return null;
    }

    @PUT
    @Path("/{userName}")
    @ApiOperation(value = "update username")
    public SCUser update(@PathParam("userName") String userName, @RequestBody SCUser scUser) {
        User currentUser = userService.findByUserName(userName);

        User user = selfCareServiceUser.convertFromSCUserToUser(scUser);
        user.password = currentUser.password;
        user.forgotPwdToken = currentUser.forgotPwdToken;
        user.forgotPwdTokenRequested = currentUser.forgotPwdTokenRequested;

        return selfCareServiceUser.convertFromUserToSCUser(userService.update(currentUser.id, user));
    }

    @DELETE
    @Path("/{userName}")
    @ApiOperation(value = "delete username")
    public void delete(@PathParam("userName") String userName) {
        userService.delete(userService.findByUserName(userName).id);
    }

    @POST
    @ApiOperation(value = "create username")
    public SCUser create(@RequestBody SCUser scUser) {
        return selfCareServiceUser.convertFromUserToSCUser(userService.create(selfCareServiceUser.convertFromSCUserToUser(scUser)));
    }

    @POST
    @Path("/forgot-password")
    @ApiOperation(value = "forgot password")
    public void forgotPassword(@RequestBody SCUserSearchForm scUserSearchForm) throws UserNotFoundException, Exception {
        try {
            if (!Strings.isNullOrEmpty(scUserSearchForm.email)) {
                userService.sendForgotPasswordWithEmail2(scUserSearchForm.redirectUrl, scUserSearchForm.email);
            } else if (!Strings.isNullOrEmpty(scUserSearchForm.userName)) {
                userService.sendForgotPassword2(scUserSearchForm.redirectUrl, scUserSearchForm.userName);
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @POST
    @Path("/check-current-password")
    @ApiOperation(value = "check current password")
    public void checkCurrentPassword(@RequestBody SCUserSearchForm scUserSearchForm) throws Exception {
        try {
            if (!userService.checkPassword(scUserSearchForm.userName, scUserSearchForm.currentPassword)) {
                throw new Exception("Wrong Pass!");
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @POST
    @Path("/change-password")
    @ApiOperation(value = "change password")
    public SCUser changePass(@RequestBody SCUserSearchForm scUserSearchForm) throws Exception {
        return selfCareServiceUser.convertFromUserToSCUser(userService.changePassword(scUserSearchForm.userName, scUserSearchForm.currentPassword, scUserSearchForm.newPassword));
    }

    @POST
    @Path("/change-password-with-token")
    @ApiOperation(value = "change pass with token")
    public void changePassWithToken(@RequestBody SCUserSearchForm scUserSearchForm) throws Exception {
        if (!userService.changePasswordWithToken(scUserSearchForm.userId, scUserSearchForm.token, scUserSearchForm.newPassword)) {
            throw new Exception("Change Pass Failed!");
        }
    }

}
