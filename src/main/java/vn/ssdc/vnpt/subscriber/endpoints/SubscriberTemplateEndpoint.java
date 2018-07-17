package vn.ssdc.vnpt.subscriber.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import vn.ssdc.vnpt.subscriber.model.SubscriberTemplate;
import vn.ssdc.vnpt.subscriber.services.SubscriberTemplateService;
import vn.vnpt.ssdc.core.SsdcCrudEndpoint;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Component
@Path("subscriber-templates")
@Api("Subscribers")
@Produces(APPLICATION_JSON)
public class SubscriberTemplateEndpoint extends SsdcCrudEndpoint<Long, SubscriberTemplate> {

    private SubscriberTemplateService subscriberTemplateService;

    @Autowired
    public SubscriberTemplateEndpoint(SubscriberTemplateService subscriberTemplateService) {
        this.service = subscriberTemplateService;
        this.subscriberTemplateService = subscriberTemplateService;
    }

    @GET
    @Path("/get-page")
    @ApiOperation(value = "Get page subscriber template")
    public Page<SubscriberTemplate> getPage(@ApiParam(value = "int of page") @DefaultValue("0") @QueryParam("page") int page,
                                            @ApiParam(value = "int of limit") @DefaultValue("20") @QueryParam("limit") int limit) {
        return subscriberTemplateService.getPage(page, limit);
    }

}