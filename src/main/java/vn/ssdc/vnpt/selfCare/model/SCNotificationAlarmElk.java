/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import vn.ssdc.vnpt.devices.model.DateUtils;
import vn.ssdc.vnpt.notification.model.NotificationAlarmElk;

/**
 *
 * @author kiendt
 */
public class SCNotificationAlarmElk {

    public Long userId;
    public String content;
    public Boolean isSeen;
    public String deviceId;
    public String id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date timestamp;
    public String type;

    public SCNotificationAlarmElk(NotificationAlarmElk alarm) {
        this.userId = alarm.userId;
        this.content = alarm.content;
        this.isSeen = alarm.status != null && alarm.status == 1 ? Boolean.TRUE : Boolean.FALSE;
        this.deviceId = alarm.deviceId;
        this.id = alarm._id;
        this.timestamp = DateUtils.convertStringToIsoDate(alarm.timestamp);
        switch (alarm.type) {
            case 0:
                this.type = "total";
                break;
            case 1:
                this.type = "critical";
                break;
            case 2:
                this.type = "major";
                break;
            case 3:
                this.type = "minor";
                break;
            default:
                this.type = "none";

        }
    }

}
