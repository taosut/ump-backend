package vn.ssdc.vnpt.policy.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.policy.model.PolicyJob;
import vn.ssdc.vnpt.devices.services.DeviceGroupService;
import vn.ssdc.vnpt.policy.services.PolicyJobService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Admin on 3/3/2017.
 */
@Component
@Path("policy")
@Api("Policies")
@Produces(APPLICATION_JSON)
public class PolicyJobEnpoint extends SsdcCrudEndpoint<Long,PolicyJob> {
    private static final Logger logger = LoggerFactory.getLogger(SsdcCrudEndpoint.class);

    @Autowired
    private PolicyJobService policyJobService;

    @Autowired
    private DeviceGroupService deviceGroupService;

    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    public PolicyJobEnpoint(PolicyJobService service) {
        this.service = service;
    }

    @POST
    @Path("/{policyJobId}/excute")
    @ApiOperation(value = "Execute policy")
    public void execute(@ApiParam(value = "Long of policy job id", example = "1") @PathParam("policyJobId") Long policyJobId) {
        policyJobService.execute(policyJobId);
    }

    @POST
    @Path("/{policyJobId}/stop")
    @ApiOperation(value = "Stop policy")
    public String stop(@ApiParam(value = "Long of policy id", example = "1") @PathParam("policyJobId") Long policyJobId) {
        return policyJobService.stop(policyJobId);
    }

    @GET
    @Path("/check-job-by-group")
    @ApiOperation(value = "Check job by params")
    public Boolean findJobByDeviceGroup(@ApiParam(value = "String of device group id", example = "") @QueryParam("groupId") String groupId,
                                        @ApiParam(value = "String of status", example = "") @QueryParam("status") String status) {
        return this.policyJobService.findJobExecute(groupId, status);
    }

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page policy")
    public Page<PolicyJob> getPage(@ApiParam(value = "int of page") @DefaultValue("1") @QueryParam("page") int page,
                                   @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit) {
        return this.policyJobService.getPage(page, limit);
    }

    @GET
    @Path("/get-page-with-number-of-execution")
    @ApiOperation(value = "Get page policy with number of execution")
    public Page<PolicyJob> getPageWithNumberOfExecution(@ApiParam(value = "int of page") @DefaultValue("1") @QueryParam("page") int page,
                                                        @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit) {
        return this.policyJobService.getPageWithNumberOfExecution(page, limit);
    }

    @GET
    @Path("/get-with-number-of-execution")
    @ApiOperation(value = "Get detail policy with number of execution")
    public PolicyJob getWithNumberOfExecution(@ApiParam(value = "Long of policy job id", example = "1") @QueryParam("id") @DefaultValue("0") Long id) {
        return this.policyJobService.getWithNumberOfExecution(id);
    }
}
