package vn.ssdc.vnpt.subscriber.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.subscriber.model.Subscriber;
import vn.ssdc.vnpt.subscriber.services.SubscriberService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("subscribers")
@Api("Subscribers")
@Produces(APPLICATION_JSON)
public class SubscriberEndpoint extends SsdcCrudEndpoint<Long, Subscriber> {

    private static final Logger logger = LoggerFactory.getLogger(SubscriberEndpoint.class);

    private SubscriberService mSubscriberService;

    @Autowired
    public SubscriberEndpoint(SubscriberService subscriberService) {
        this.service = subscriberService;
        mSubscriberService = subscriberService;
    }

    @GET
    @Path("/find-by-subscriber-id")
    @ApiOperation(value = "Get detail subscriber by subcriber id")
    public Subscriber findBySubscribeId(@ApiParam(value = "String of subscriber id", example = "subscriberid1") @QueryParam("subscriberId") String subscriberId) {
        return mSubscriberService.findBySubscribeId(subscriberId);
    }

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page subscriber")
    public Page<Subscriber> getPage(@ApiParam(value = "int of page") @DefaultValue("0") @QueryParam("page") int page,
            @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit) {
        return mSubscriberService.getPage(page, limit);
    }

    @GET
    @Path("/get-by-subscriber-template-id")
    @ApiOperation(value = "Get detail subscriber by subscriber template id")
    public Subscriber findBySubscriberTemplateId(@ApiParam(value = "Long of subscriber template id", example = "1") @DefaultValue("0") @QueryParam("subscriberTemplateId") Long subscriberTemplateId) {
        return mSubscriberService.findBySubscriberTemplateId(subscriberTemplateId);
    }

    @POST
    @Path("/create")
    @ApiOperation(value = "Post create subscriber")
    public Map<String, Object> createSubscriber(@ApiParam(value = "JSON format", example = "") Subscriber subscriber) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();

        try {
            Set<String> messageInvalid = mSubscriberService.validateSubscriber(subscriber);
            if (messageInvalid.isEmpty()) {
                mSubscriberService.postCreate(subscriber);
                response.put("status", "success");
                response.put("data", subscriber);
            } else {
                response.put("status", "invalid");
                response.put("message", messageInvalid);
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Create error");
        }

        return response;
    }

}
