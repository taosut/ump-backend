/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.common.services.ConfigurationService;
import vn.ssdc.vnpt.selfCare.model.SCDevice;
import vn.ssdc.vnpt.selfCare.model.SCSubscriberTemplate;
import vn.ssdc.vnpt.selfCare.model.SCSubscriber;
import vn.ssdc.vnpt.selfCare.model.SCSubscriberDevice;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCSubscriberDeviceSearchForm;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCSubscriberSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceSubscriber;
import vn.ssdc.vnpt.subscriber.services.SubscriberDeviceService;
import vn.ssdc.vnpt.subscriber.services.SubscriberService;
import vn.ssdc.vnpt.subscriber.services.SubscriberTemplateService;

/**
 *
 * @author Admin
 */
@Component
@Path("/self-care/subscribers")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Subscriber")
public class SCSubscriberEndpoint {

    @Autowired
    private SubscriberTemplateService subscriberTemplateService;

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private SubscriberDeviceService subscriberDeviceService;

    @Autowired
    public ConfigurationService configurationService;

    @Autowired
    public SelfCareServiceSubscriber selfCareServiceSubscriber;

    // API for subscriber template
    @POST
    @Path("/templates/search")
    @ApiOperation(value = "Get subscriber template", notes = "chi can truyen len id hoac limit và page")
    public List<SCSubscriberTemplate> searchSubcriberTemplate(@RequestBody SCSubscriberSearchForm sCSubcriberTemplateSearchForm) {
        // tim theo subcriber template id
        return selfCareServiceSubscriber.searchSubscriberTemplate(sCSubcriberTemplateSearchForm);
    }

    @POST
    @Path("/templates/count")
    @ApiOperation(value = "count subscriber template", notes = "chi can truyen len id hoac limit và page")
    public long countSubcriberTemplate(@RequestBody SCSubscriberSearchForm sCSubcriberTemplateSearchForm) {
        // tim theo subcriber template id
        return selfCareServiceSubscriber.countSubcriberTemplate(sCSubcriberTemplateSearchForm);
    }

    @POST
    @Path("/templates")
    @ApiOperation(value = "create subscriber template")
    public SCSubscriberTemplate create(@RequestBody SCSubscriberTemplate subscriberTemplate) {
        // tim theo subscriber template id
        return SCSubscriberTemplate.convertToSCSubscriberTemplate(subscriberTemplateService.create(SCSubscriberTemplate.convertToSubscriberTemplate(subscriberTemplate)));
    }

    @DELETE
    @Path("/templates/{id}")
    @ApiOperation(value = "create subscriber template")
    public void delete(@PathParam("id") Long id) {
        // tim theo subscriber template id
        subscriberTemplateService.delete(id);
    }

    @PUT
    @Path("/templates/{id}")
    @ApiOperation(value = "create subscriber template")
    public SCSubscriberTemplate update(@PathParam("id") Long id, @RequestBody SCSubscriberTemplate subscriberTemplate) {
        // tim theo subscriber template id
        return SCSubscriberTemplate.convertToSCSubscriberTemplate(subscriberTemplateService.update(id, SCSubscriberTemplate.convertToSubscriberTemplate(subscriberTemplate)));
    }

    // API for subscriber
    @POST
    @ApiOperation(value = "create subscriber")
    public SCSubscriber createSubscriber(@RequestBody SCSubscriber scSubscriber) throws ParseException, Exception {
        selfCareServiceSubscriber.addSubscriberDevicesBySubscriber(scSubscriber);
        return SCSubscriber.convertToSCSubscriber(subscriberService.create(SCSubscriber.convertToSubscriber(scSubscriber)));
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(value = "delete subscriber")
    public void deleteSubscriber(@PathParam("id") Long id) {
        subscriberService.delete(id);
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "update subscriber")
    public SCSubscriber updateSubscriber(@PathParam("id") Long id, @RequestBody SCSubscriber scSubscriber) throws Exception {
        selfCareServiceSubscriber.updateSubscriberDevicesBySubscriber(scSubscriber);
        return SCSubscriber.convertToSCSubscriber(subscriberService.update(id, SCSubscriber.convertToSubscriber(scSubscriber)));
    }

    @POST
    @Path("/search")
    @ApiOperation(value = "search subscriber")
    public List<SCSubscriber> searchSubscriber(@RequestBody SCSubscriberSearchForm scSubscriber) {
        // neu ton tai deviceid trong cau search thi tim theo subcriber theo deviceId truoc
        return selfCareServiceSubscriber.searchSubscriber(scSubscriber);
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "count subscriber")
    public long count(@RequestBody SCSubscriberSearchForm scSubscriber) {
        String whereExp = selfCareServiceSubscriber.generateQuery(scSubscriber);
        return subscriberService.count(whereExp);
    }

    // API for subscriber device
    @POST
    @Path("/devices/search")
    @ApiOperation(value = "search subscriber devices")
    public List<SCSubscriberDevice> searchSubscriberDevice(@RequestBody SCSubscriberDeviceSearchForm sCSubscriberDeviceSearchForm) {
        return selfCareServiceSubscriber.searchSubscriberDevice(sCSubscriberDeviceSearchForm);
    }

    @DELETE
    @Path("/devices/{subscriberId}")
    @ApiOperation(value = "delete subscriber device")
    public void deleteSubscriber(@PathParam("subscriberId") String subscriberId) throws Exception {
        selfCareServiceSubscriber.deleteBySubscriberId(subscriberId);
    }
    
    @POST
    @Path("/devices/replace")
    @ApiOperation(value = "replace devices", notes = "example = { \"old_device_id\" : \"aaaa\", \"new_device_id\":\"\" }")
    public Boolean replaceDevice(@RequestBody Map<String, String> mapRequest) {
        if (!mapRequest.containsKey("old_device_id")) {
            return false;
        }
        if (!mapRequest.containsKey("new_device_id")) {
            return false;
        }
        return this.subscriberDeviceService.replaceCPE(mapRequest.get("old_device_id"), mapRequest.get("new_device_id"));
    }
    
}
