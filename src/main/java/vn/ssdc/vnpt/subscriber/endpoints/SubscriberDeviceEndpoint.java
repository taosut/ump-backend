package vn.ssdc.vnpt.subscriber.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.subscriber.model.SubscriberDevice;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("subscriber-devices")
@Api("Subscribers")
@Produces(APPLICATION_JSON)
public class SubscriberDeviceEndpoint extends SsdcCrudEndpoint<Long, SubscriberDevice> {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberDeviceEndpoint.class);

    private SubscriberDeviceService subscriberDeviceService;

    public SubscriberDeviceEndpoint(SubscriberDeviceService subscriberDeviceService) {
        this.service = subscriberDeviceService;
        this.subscriberDeviceService = subscriberDeviceService;
    }

    @GET
    @Path("/find-by-subscriber-id")
    @ApiOperation(value = "Find list subscriber device by subscriber id")
    public List<SubscriberDevice> findBySubscribeId(@ApiParam(value = "String of subscriber id", example = "subscriberid1") @QueryParam("subscriberId") String subscriberId) {
        return subscriberDeviceService.findBySubscribeId(subscriberId);
    }

    @GET
    @Path("/replace-cpe/{oldDeviceId}/{newDeviceId}")
    @ApiOperation(value = "ReplaceCPE")
    public boolean replaceCPE(@ApiParam(value = "String of device id", example = "a06518-968380GERG-VNPT00a532c2") @PathParam("oldDeviceId") String oldDeviceId,
            @ApiParam(value = "String of device id", example = "a06518-968380GERG-VNPT00a532c2") @PathParam("newDeviceId") String newDeviceId) {
        return this.subscriberDeviceService.replaceCPE(oldDeviceId, newDeviceId);
    }

    @GET
    @Path("/find-by-device-id")
    @ApiOperation(value = "Check subscriber by device id")
    public boolean findSubByDeviceId(@ApiParam(value = "String of device id", example = "a06518-968380GERG-VNPT00a532c2") @QueryParam("deviceId") String deviceId) {
        return subscriberDeviceService.findSubByDeviceId(deviceId);
    }
}
