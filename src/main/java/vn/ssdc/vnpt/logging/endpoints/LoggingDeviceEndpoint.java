package vn.ssdc.vnpt.logging.endpoints;

import io.searchbox.client.JestClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.common.model.Configuration;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.logging.model.LoggingDevice;
import vn.ssdc.vnpt.logging.services.LoggingDeviceService;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("logging/device")
@Api("DeviceLog")
@Produces(APPLICATION_JSON)
public class LoggingDeviceEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LoggingDeviceEndpoint.class);

    @Autowired
    JestClient elasticSearchClient;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    LoggingDeviceService loggingDeviceService;

    @Value("${elasticSearchUrl}")
    public String elasticSearchUrl;

    @Value("${tmpDir}")
    private String tmpDir;

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page device log")
    public List<LoggingDevice> getPageLoggingDevice(@ApiParam(value = "int of page") @DefaultValue("1") @QueryParam("page") int page,
                                                    @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit,
                                                    @ApiParam(value = "Sting of name for filter", example = "Reboot") @DefaultValue("") @QueryParam("name") String name,
                                                    @ApiParam(value = "String of actor for filter, filter all actor if value is ACS", example = "a06518-968380GERG-VNPT00a532c2") @DefaultValue("") @QueryParam("actor") String actor,
                                                    @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                                    @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime,
                                                    @ApiParam(value = "String of username login system", example = "ump") @DefaultValue("") @QueryParam("username") String username) {

        if ("acs".equals(actor.toLowerCase())) {
            actor = ""; // Search all if actor is ACS
        }
        return loggingDeviceService.getPage(page, limit, name, actor, fromDateTime, toDateTime, username);
    }

    @GET
    @Path("/get-total-pages")
    @ApiOperation(value = "Get total page device log")
    public Long getTotalPages(@ApiParam(value = "int of page") @DefaultValue("1") @QueryParam("page") int page,
                              @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit,
                              @ApiParam(value = "Sting of name for filter", example = "Reboot") @DefaultValue("") @QueryParam("name") String name,
                              @ApiParam(value = "String of actor for filter, filter all actor if value is ACS", example = "a06518-968380GERG-VNPT00a532c2") @DefaultValue("") @QueryParam("actor") String actor,
                              @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                              @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime,
                              @ApiParam(value = "String of username login system", example = "ump") @DefaultValue("") @QueryParam("username") String username) {

        if ("acs".equals(actor.toLowerCase())) {
            actor = ""; // Search all if actor is ACS
        }
        return loggingDeviceService.getTotalPages(page, limit, name, actor, fromDateTime, toDateTime, username);
    }


    @POST
    @Path("/remove-elk")
    @ApiOperation(value = "Remove device log")
    public Boolean removeLoggingDevice(@ApiParam(value = "Sting of name for filter", example = "Reboot") @DefaultValue("") @QueryParam("name") String name,
                                       @ApiParam(value = "String of actor for filter, filter all actor if value is ACS", example = "a06518-968380GERG-VNPT00a532c2") @DefaultValue("") @QueryParam("actor") String actor,
                                       @ApiParam(value = "String of from date time to filter", example = "2016-11-11 11:12:32") @DefaultValue("") @QueryParam("fromDateTime") String fromDateTime,
                                       @ApiParam(value = "String of to date time to filter", example = "2017-11-11 11:12:32") @DefaultValue("") @QueryParam("toDateTime") String toDateTime,
                                       @ApiParam(value = "String of username login system", example = "ump") @DefaultValue("") @QueryParam("username") String username) {
        if ("acs".equals(actor.toLowerCase())) {
            actor = ""; // Search all if actor is ACS
        }
        return loggingDeviceService.removeElk(name, actor, fromDateTime, toDateTime, username);
    }

    @POST
    @Path("/post-save-time-expire")
    @ApiOperation(value = "Update time expire")
    public Boolean postSaveTimeExpire(@ApiParam(value = "JSON format, keys allow are timeExpire") Map<String, String> request) {

        Boolean result = false;
        try {
            String timeExpire = request.containsKey("timeExpire") ? request.get("timeExpire") : "";
            Configuration configuration = configurationService.get("timeExpire");
            configuration.value = timeExpire;
            configurationService.update(configuration.id, configuration);
            result = true;

        } catch (Exception e) {
            logger.error("postSaveTimeExpire", e);
        }

        return result;
    }

    @GET
    @Path("/export/{session}")
    @ApiOperation(value = "Export device log to xml")
    public Response exportDataModelXML(@ApiParam(value = "String of session", example = "c9be363dacbd2dfa") @PathParam("session") String session) {
        String path = loggingDeviceService.exportXML(session);
        File file = new File(path);
        Response.ResponseBuilder builder = Response.ok(file);
        builder.header("Content-Disposition", "attachment; filename=" + file.getName());
        return builder.build();
    }

}
