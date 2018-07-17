package vn.ssdc.vnpt.quartz.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.quartz.model.QrtzTriggers;
import vn.ssdc.vnpt.quartz.services.QrtzTriggersService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;
import javax.ws.rs.*;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Created by Admin on 11/9/2017.
 */
@Component
@Path("quartz")
@Api("Quartz")
@Produces(APPLICATION_JSON)
public class QurtzTriggersEndpoint extends SsdcCrudEndpoint<String, QrtzTriggers> {
    private QrtzTriggersService qrtzTriggersService;
    private static final Logger logger = LoggerFactory.getLogger(QurtzTriggersEndpoint.class);

    @Autowired
    public QurtzTriggersEndpoint(QrtzTriggersService qrtzTriggersService) {
        this.service = this.qrtzTriggersService = qrtzTriggersService;
    }

    @GET
    @Path("/getAll")
    public List<QrtzTriggers> getAll() {
        List<QrtzTriggers> lstQtzTrigger = new ArrayList<>();
        try {
            lstQtzTrigger = qrtzTriggersService.connectionToQuartzDB();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return lstQtzTrigger;
    }

    @POST
    @Path("/deleteQuartz/{jobName}/{triggerName}")
    @ApiResponse(code = 200, message = "Success", response = QurtzTriggersEndpoint.class)
    public String deleteQuartz(
            @ApiParam(value = "quartz job name") @PathParam("jobName") String jobName,
            @ApiParam(value = "quartz trigger name")@PathParam("triggerName") String triggerName) {
        try {
            qrtzTriggersService.deleteQuartzJob(jobName);
            qrtzTriggersService.deleteTriger(triggerName);

            return "200";
        } catch (SchedulerException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    @POST
    @Path("/restartQuartz/{jobName}/{triggerName}")
    @ApiResponse(code = 200, message = "Success", response = QurtzTriggersEndpoint.class)
    public String restartQuartz(
            @ApiParam(value = "quartz job name") @PathParam("jobName") String jobName,
            @ApiParam(value = "quartz trigger name")@PathParam("triggerName") String triggerName) {
        try {
            qrtzTriggersService.deleteQuartzJob(jobName);
            qrtzTriggersService.deleteTriger(triggerName);
            //
            qrtzTriggersService.restartTrigger(triggerName);
            return "200";
        } catch (SchedulerException e) {
            e.printStackTrace();
            return e.toString();
        }
    }
}
