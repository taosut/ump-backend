package vn.ssdc.vnpt.policy.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import vn.ssdc.vnpt.logging.model.LoggingDeviceActivity;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.policy.model.PolicyTask;
import vn.ssdc.vnpt.policy.services.PolicyTaskService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by THANHLX on 4/14/2017.
 */
@Component
@Path("policy-task")
@Api("Policies")
@Produces(APPLICATION_JSON)
public class PolicyTaskEnpoint extends SsdcCrudEndpoint<Long, PolicyTask> {
    private static final Logger logger = LoggerFactory.getLogger(PolicyTaskEnpoint.class);

    @Autowired
    private PolicyTaskService policyTaskService;

    @Autowired
    private LoggingPolicyService loggingPolicyService;

    @Autowired
    public PolicyTaskEnpoint(PolicyTaskService policyTaskService) {
        this.service = this.policyTaskService = policyTaskService;
    }

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page policy log")
    public List<PolicyTask> getPage(@ApiParam(value = "Long of policy job id", example = "1") @QueryParam("policyJobId") Long policyJobId,
                                    @ApiParam(value = "int of page") @DefaultValue("0") @QueryParam("page") int page,
                                    @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit) {
        return loggingPolicyService.getPage(page, limit, policyJobId);
    }

    @GET
    @Path("/get-page-device-activity")
    @ApiOperation(value = "Get page device activity")
    public List<LoggingDeviceActivity> getPageDeviceActivity(@ApiParam(value = "int of page") @DefaultValue("1") @QueryParam("page") int page,
                                                             @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit,
                                                             @ApiParam(value = "String of device id", example = "a06518-968380GERG-VNPT00a532c2") @QueryParam("deviceId") String deviceId,
                                                             @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                             @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {

        return loggingPolicyService.getPageDeviceActivity(page, limit, deviceId, fromDateTime, toDateTime);
    }

    @GET
    @Path("/get-summary-device-activity")
    @ApiOperation(value = "Get summary in list device actity")
    public Map<String, Long> getSummaryDeviceActivity(@ApiParam(value = "String of device id", example = "a06518-968380GERG-VNPT00a532c2") @DefaultValue("") @QueryParam("deviceId") String deviceId,
                                                      @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                      @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {
        return loggingPolicyService.getSummaryDeviceActivity(deviceId, fromDateTime, toDateTime);
    }

    @GET
    @Path("/get-summary")
    @ApiOperation(value = "Get summary in log policy")
    public Map<String, Long> getSummary(@ApiParam(value = "Long of policy job id", example = "1") @DefaultValue("0") @QueryParam("policyJobId") Long policyJobId) {
        return loggingPolicyService.getSummary(policyJobId);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "Count log policy with params")
    public long count(@ApiParam(value = "String of query sql", example = "") @RequestParam String query) {
        return this.policyTaskService.count(query);
    }

    @GET
    @Path("/find-by-taskId")
    @ApiOperation(value = "Find log policy with task id")
    public PolicyTask findByTaskId(@ApiParam(value = "String of task id", example = "") @QueryParam("taskId") String taskId) {
        return this.policyTaskService.findByTaskId(taskId);
    }

    @GET
    @Path("/group-by-status")
    @ApiOperation(value = "Get list log policy by group status")
    public List<PolicyTask> groupByStatus(){
        return this.policyTaskService.groupByStatus();
    }

    @POST
    @Path("/delete-device-activity")
    @ApiOperation(value = "Remove device activity by id")
    public Boolean deleteDeviceActivity(@ApiParam(value = "JSON format, keys allow are id", example = "") Map<String, String> request) {
        Boolean result = false;
        if (request.containsKey("id")) {
            result = loggingPolicyService.removeById(request.get("id"));
        }
        return result;
    }
}
