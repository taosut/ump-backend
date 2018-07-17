/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
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
import vn.ssdc.vnpt.notification.model.NotificationAlarmElk;
import vn.ssdc.vnpt.selfCare.model.SCNotificationAlarmElk;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCNotificationElkSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceNotificationElk;

/**
 *
 * @author kiendt
 */
@Component
@Path("/self-care/notification-elk")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Notication ELK")
public class SCNotificationElkEndpoint {

    @Autowired
    SelfCareServiceNotificationElk selfCareServiceNotificationElk;

    @POST
    @Path("/search")
    @ApiOperation(value = "do search")
    public List<SCNotificationAlarmElk> search(@RequestBody SCNotificationElkSearchForm searchForm) {
        List<SCNotificationAlarmElk> sCNotificationAlarmElks = new ArrayList<>();
        List<NotificationAlarmElk> notificationAlarmElks = selfCareServiceNotificationElk.search(searchForm);
        for (NotificationAlarmElk tmp : notificationAlarmElks) {
            System.out.println(new SCNotificationAlarmElk(tmp).timestamp);
            sCNotificationAlarmElks.add(new SCNotificationAlarmElk(tmp));
        }
        return sCNotificationAlarmElks;
    }

    @POST
    @Path("/count")
    @ApiOperation(value = "do search")
    public Integer count(@RequestBody SCNotificationElkSearchForm searchForm) {
        searchForm.limit = null;
        searchForm.page = null;
        List<SCNotificationAlarmElk> sCNotificationAlarmElks = new ArrayList<>();
        List<NotificationAlarmElk> notificationAlarmElks = selfCareServiceNotificationElk.search(searchForm);
        for (NotificationAlarmElk tmp : notificationAlarmElks) {
            System.out.println(new SCNotificationAlarmElk(tmp).timestamp);
            sCNotificationAlarmElks.add(new SCNotificationAlarmElk(tmp));
        }
        return sCNotificationAlarmElks.size();
    }

    @PUT
    @Path("/{id}")
    @ApiOperation(value = "do update")
    public SCNotificationAlarmElk search(@PathParam("id") String id, @RequestBody NotificationAlarmElk notificationAlarmElk) {
        return new SCNotificationAlarmElk(selfCareServiceNotificationElk.update(id, notificationAlarmElk));
    }

    @PUT
    @Path("/changeStatus/{id}")
    @ApiOperation(value = "do update")
    public SCNotificationAlarmElk changeStatus(@PathParam("id") String id) {
        return new SCNotificationAlarmElk(selfCareServiceNotificationElk.updateStatus(id));
    }

}
