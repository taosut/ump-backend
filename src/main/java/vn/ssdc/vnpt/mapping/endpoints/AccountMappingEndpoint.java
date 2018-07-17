/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.mapping.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.mapping.services.AccountMappingService;

/**
 *
 * @author kiendt
 */
@Component
@Path("account-mapping")
@Api("Account Mapping")
@Produces(APPLICATION_JSON)
public class AccountMappingEndpoint {

    @Autowired
    private AccountMappingService accountMappingService;

    @POST
    @Path("/check-exist/{accountName}")
    @ApiOperation(value = "Check Account")
    @ApiResponse(code = 200, message = "Success", response = Boolean.class)
    public boolean checkIPMappingExist(@PathParam("accountName") String accountName) {
        return false;
//        return accountMappingService.checkDuplicateAccountMapping(accountName);
    }
}
