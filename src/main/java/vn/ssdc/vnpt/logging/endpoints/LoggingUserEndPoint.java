package vn.ssdc.vnpt.logging.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.logging.model.LoggingUser;
import vn.ssdc.vnpt.logging.services.LoggingUserService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("logging/user")
@Api("UserLog")
@Produces(APPLICATION_JSON)
public class LoggingUserEndPoint extends SsdcCrudEndpoint<Long, LoggingUser> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);

    private LoggingUserService loggingUserService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Autowired
    public LoggingUserEndPoint(LoggingUserService loggingUserService) {
        this.service = this.loggingUserService = loggingUserService;
    }

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page user log")
    public Page<LoggingUser> getPage(@ApiParam(value = "int of page") @DefaultValue("1") @QueryParam("page") int page,
                                     @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit,
                                     @ApiParam(value = "Sting of name for filter", example = "Reboot") @DefaultValue("") @QueryParam("name") String name,
                                     @ApiParam(value = "String of actor for filter, filter all actor if value is ACS", example = "a06518-968380GERG-VNPT00a532c2") @DefaultValue("") @QueryParam("actor") String actor,
                                     @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                     @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {
        return loggingUserService.getPage(page, limit, name, actor, fromDateTime, toDateTime);
    }

    @POST
    @Path("/remove-all")
    @ApiOperation(value = "Remove user log")
    public Boolean removeAll(@ApiParam(value = "Sting of name for filter", example = "Reboot") @DefaultValue("") @QueryParam("name") String name,
                             @ApiParam(value = "String of actor for filter, filter all actor if value is ACS", example = "a06518-968380GERG-VNPT00a532c2") @DefaultValue("") @QueryParam("actor") String actor,
                             @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                             @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime,
                             @ApiParam(value = "String of username login system", example = "ump") @DefaultValue("") @QueryParam("username") String username) {
        return loggingUserService.removeElk(name, actor, fromDateTime, toDateTime, username);
    }

    @GET
    @Path("/get-update-mysql")
    @ApiOperation(value = "Update user log to elk server")
    public List<LoggingUser> getPage(@ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                     @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime) {
        return loggingUserService.getUpdateMysql(fromDateTime, toDateTime);
    }

}
