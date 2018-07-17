/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.demo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import vn.ssdc.vnpt.elk.BaseElkService;
import vn.ssdc.vnpt.kafka.services.KafkaService;
import vn.ssdc.vnpt.notification.model.NotificationAlarmElk;
import vn.ssdc.vnpt.qos.model.QosAlarmDetail;
import vn.ssdc.vnpt.qos.model.QosAlarmDeviceNew;
import vn.ssdc.vnpt.qos.model.QosKpiDataELK;
import vn.ssdc.vnpt.qos.services.QosELKService;
import vn.ssdc.vnpt.selfCare.model.SCDeviceGroup;
import vn.ssdc.vnpt.selfCare.services.SelfCareServiceDeviceGroup;

/**
 *
 * @author kiendt
 */
@Component
@Path("/elk")
@Produces(APPLICATION_JSON)
@Consumes({APPLICATION_JSON, TEXT_PLAIN})
@Api("ELk Endpoint")
public class ELKEndpoint {

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    QosELKService qosELKService;

    @Autowired
    SelfCareServiceDeviceGroup deviceGroupService;

    @POST
    @Path("/testNotificationMeessage")
    @ApiOperation(value = "create device group data")
    public void testNotificationMeessage(@RequestBody String message) throws IOException {
        kafkaService.sendToNotificationTopic(message);
    }

    @POST
    @Path("/createDeviceGroupData")
    @ApiOperation(value = "create device group data")
    public void createDeviceGroup(@RequestBody List<QosAlarmDetail> datas) throws IOException {
        for (QosAlarmDetail tmp : datas) {
            qosELKService.createAlarmDashBoard(tmp);
        }
    }

    @POST
    @Path("/createDashBoardData")
    @ApiOperation(value = "create dash board data")
    public void createAlarmELK(@RequestBody List<QosAlarmDeviceNew> datas) throws IOException {
        for (QosAlarmDeviceNew tmp : datas) {
            qosELKService.createAlarmDemo(tmp);
        }
    }

    @POST
    @Path("/createDataUMPBackend")
    @ApiOperation(value = "create data UMP backend")
    public void createQosKpi(@RequestBody List<QosKpiDataELK> datas) throws IOException {
        for (QosKpiDataELK tmp : datas) {
            qosELKService.create(tmp);
        }
    }

    @POST
    @Path("/TestDevice")
    @ApiOperation(value = "create qos kpi ELKl")
    public List<SCDeviceGroup> createQosKpi(@RequestBody String deviceID) throws IOException {
        return deviceGroupService.findByDevice(deviceID);
    }

    @Autowired
    BaseElkService baseService;

    @Value("${spring.elk.index.notification_alarm}")
    public String INDEX_NOTIFIACTION_ALARM;

    @Value("${spring.elk.type.notification_alarm}")
    public String TYPE_NOTIFIACTION_ALARM;

    @POST
    @Path("/testCreate/{type}/{id}")
    @ApiOperation(value = "create qos kpi ELKl")
    public void testCreate(@PathParam("type") Integer type, @PathParam("id") String id, @RequestBody NotificationAlarmElk alarm) throws IOException, IllegalArgumentException, IllegalAccessException {
        switch (type) {
            case 1:
                baseService.insertDocument(alarm, INDEX_NOTIFIACTION_ALARM, TYPE_NOTIFIACTION_ALARM);
                break;
            case 2:
                baseService.updateDocument(id, alarm, INDEX_NOTIFIACTION_ALARM, TYPE_NOTIFIACTION_ALARM);
                break;
            case 4:
                baseService.deleteDoucmentById(id, alarm, INDEX_NOTIFIACTION_ALARM, TYPE_NOTIFIACTION_ALARM);
                break;
        }
    }

}
