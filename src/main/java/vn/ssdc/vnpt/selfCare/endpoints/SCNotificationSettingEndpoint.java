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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.notification.model.NotificationSetting;
import vn.ssdc.vnpt.notification.services.NotificationSettingService;
import vn.ssdc.vnpt.selfCare.model.SCNotificationSetting;
import vn.ssdc.vnpt.selfCare.model.searchForm.SCNotificationSettingSearchForm;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceNotificationSetting;
import vn.ssdc.vnpt.umpexception.QosException;

/**
 *
 * @author kiendt
 */
@Component
@Path("/self-care/notification-setting")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("Self-Care Notication Setting")
public class SCNotificationSettingEndpoint {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SCNotificationSettingEndpoint.class);

    @Autowired
    private NotificationSettingService notificationSettingService;

    @Autowired
    private SelfCareServiceNotificationSetting selfCareServiceNotificationSetting;

//    @POST
//    @Path("/search")
//    @ApiOperation(value = "do search")
//    public List<SCNotificationSetting> search(@RequestBody SCNotificationSettingSearchForm searchForm) {
//        return selfCareServiceNotificationSetting.search(searchForm);
//    }

    @GET
    @Path("/userId/{userId}")
    @ApiOperation(value = "do search")
    public SCNotificationSetting search(@PathParam("userId") Long userId) {
        NotificationSetting setting = notificationSettingService.getByUserId(userId);
        if (setting != null) {
            return new SCNotificationSetting(setting);
        }
        logger.info("Cannot find notification for userId {}", userId);
        return null;
    }

//    @POST
//    @Path("/count")
//    @ApiOperation(value = "do count")
//    public int count(@RequestBody SCNotificationSettingSearchForm searchForm) {
//        return selfCareServiceNotificationSetting.count(searchForm);
//    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Get infor notification setting")
    public SCNotificationSetting getNotificationSetting(@PathParam("id") Long id) {
        return new SCNotificationSetting(notificationSettingService.get(id));
    }

//    @GET
//    @Path("/getAll")
//    @ApiOperation(value = "Get all notification setting")
//    public List<SCNotificationSetting> getAll() {
//        List<SCNotificationSetting> scNotificationSettings = new ArrayList<>();
//        List<NotificationSetting> notificationSettings = notificationSettingService.getAll();
//        for (NotificationSetting notificationSetting : notificationSettings) {
//            scNotificationSettings.add(new SCNotificationSetting(notificationSetting));
//        }
//        return scNotificationSettings;
//    }

//    @POST
//    @ApiOperation(value = "create notification setting")
//    public SCNotificationSetting addNotificationSetting(@RequestBody SCNotificationSetting sCNotificationSetting) throws QosException {
//        NotificationSetting notificationSetting = sCNotificationSetting.convertToNotificationSetting();
//        return new SCNotificationSetting(notificationSettingService.create(notificationSetting));
//    }
    
    @PUT
    @Path("/{id}")
    @ApiOperation(value = "update notification setting")
    public SCNotificationSetting updateNotificationSetting(@PathParam("id") Long id, @RequestBody SCNotificationSetting sCNotificationSetting) throws QosException {
        NotificationSetting notificationSetting = sCNotificationSetting.convertToNotificationSetting();
        notificationSetting.id = id;
        return new SCNotificationSetting(notificationSettingService.update(id, notificationSetting));
    }

//    @DELETE
//    @Path("/{id}")
//    @ApiOperation(value = "delete notification setting")
//    public void deleteNotificationSetting(@PathParam("id") Long id) {
//        notificationSettingService.delete(id);
//    }
}
