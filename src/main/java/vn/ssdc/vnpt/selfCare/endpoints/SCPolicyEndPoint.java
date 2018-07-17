/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.policy.model.PolicyJob;
import vn.ssdc.vnpt.policy.services.PolicyJobService;
import vn.ssdc.vnpt.selfCare.model.SCPolicy;
import vn.ssdc.vnpt.selfCare.model.SCPolicyLog;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicyLogSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCPolicySearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServicePolicy;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Admin
 */
@Component
@Path("/self-care/policies")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Policies")
public class SCPolicyEndPoint {

    @Autowired
    PolicyJobService policyJobService;

    @Autowired
    SelfCareServicePolicy selfCareServicePolicy;

    @POST
    @ApiOperation(value = "Create a new policy")
    @ApiResponse(code = 200, message = "Success", response = SCPolicy.class)
    public SCPolicy create(@RequestBody SCPolicy scPolicyJob) {
        return selfCareServicePolicy.create(scPolicyJob);
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Read policy by id")
    public SCPolicy read(@PathParam("id") Long id) {
        return selfCareServicePolicy.convertToSCPolicy(policyJobService.get(id), new SCPolicy());
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "Update policy")
    @ApiResponse(code = 200, message = "Success", response = SCPolicy.class)
    public SCPolicy update(@PathParam("id") Long id, @RequestBody SCPolicy scPolicyJob) {
        return selfCareServicePolicy.update(id, scPolicyJob);
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "Delete policy by id")
    public void delete(@PathParam("id") Long id) {
        policyJobService.delete(id);
    }

    /**
     * Search policies
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count")
    @ApiOperation(value = "Count policies")
    @ApiResponse(code = 200, message = "Success")
    public int count(@RequestBody SCPolicySearchForm searchParameter) {
        return selfCareServicePolicy.count(searchParameter);
    }

    /**
     * Search policies
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search")
    @ApiOperation(value = "Search policies")
    @ApiResponse(code = 200, message = "Success", response = SCPolicy.class)
    public List<SCPolicy> search(@RequestBody SCPolicySearchForm searchParameter) {
        return selfCareServicePolicy.search(searchParameter);
    }

    @POST
    @Path("/execute/{id}")
    @ApiOperation(value = "Execute policy by id")
    public void execute(@PathParam("id") Long id) {
        policyJobService.execute(id);
        PolicyJob policyJob = policyJobService.get(id);
        policyJob.status = "EXECUTE";
        policyJobService.update(id, policyJob);
    }

    @POST
    @Path("/stop/{id}")
    @ApiOperation(value = "Stop policy by id")
    public void stop(@PathParam("id") Long id) {
        policyJobService.stop(id);
        PolicyJob policyJob = policyJobService.get(id);
        policyJob.status = "STOP";
        policyJobService.update(id, policyJob);
    }

    /**
     * Search policy logs
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/count-log/{id}")
    @ApiOperation(value = "Count policy logs")
    @ApiResponse(code = 200, message = "Success")
    public int countPolicyLog(@PathParam("id") Long id, @RequestBody SCPolicyLogSearchForm searchParameter) {
        return selfCareServicePolicy.countPolicyLog(id, searchParameter);
    }

    /**
     * Search policies
     *
     * @param searchParameter
     * @return
     */
    @POST
    @Path("/search-log/{id}")
    @ApiOperation(value = "Search policy logs")
    @ApiResponse(code = 200, message = "Success", response = SCPolicyLog.class)
    public List<SCPolicyLog> searchPolicyLog(@PathParam("id") Long id, @RequestBody SCPolicyLogSearchForm searchParameter) {
        return selfCareServicePolicy.searchPolicyLog(id, searchParameter);
    }

}
