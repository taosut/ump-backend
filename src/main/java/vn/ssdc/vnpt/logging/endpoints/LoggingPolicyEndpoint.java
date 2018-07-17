package vn.ssdc.vnpt.logging.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.logging.services.LoggingPolicyService;
import vn.ssdc.vnpt.policy.model.PolicyTask;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("logging/policy")
@Api("PolicyLog")
@Produces(APPLICATION_JSON)
public class LoggingPolicyEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LoggingPolicyEndpoint.class);

    @Autowired
    LoggingPolicyService loggingPolicyService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Value("${tmpDir}")
    private String tmpDir;

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page policy log")
    public List<PolicyTask> getPageLoggingPolicy(@ApiParam(value = "int of page") @DefaultValue("1") @QueryParam("page") int page,
                                                 @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit,
                                                 @ApiParam(value = "Long of policy job id", example = "1") @DefaultValue("0") @QueryParam("policyJobId") Long policyJobId,
                                                 @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                 @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {

        return loggingPolicyService.getPage(page, limit, policyJobId);
    }

    @GET
    @Path("/get-summary")
    @ApiOperation(value = "Get summary in list policy log")
    public Map<String, Long> getSummary(@ApiParam(value = "Long of policy job id", example = "1") @DefaultValue("0") @QueryParam("policyJobId") Long policyJobId) {
        return loggingPolicyService.getSummary(policyJobId);
    }


    @POST
    @Path("/remove-all")
    @ApiOperation(value = "Remove all policy log")
    public Boolean removeAllLoggingDevice() {
        return loggingPolicyService.removeAllElk();
    }

}
