/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import vn.ssdc.vnpt.notification.model.NotificationSetting;

/**
 *
 * @author kiendt
 */
public class SCNotificationSetting {

    public Long id;
    public Integer alarmTotal;
    public Integer criticalTotal;
    public Integer majorTotal;
    public Integer minorTotal;
    public boolean isSendEmail;
    public boolean isSendNotifyList;
    public boolean isSendNotify;
    public boolean isSendSms;
    public Long userId;
    public boolean isActive;
    public Set<Long> deviceGroups;

    public SCNotificationSetting() {
    }

    public SCNotificationSetting(NotificationSetting notificationSetting) {
        this.id = notificationSetting.id;
        this.alarmTotal = notificationSetting.alarmTotal;
        this.criticalTotal = notificationSetting.criticalTotal;
        this.majorTotal = notificationSetting.majorTotal;
        this.minorTotal = notificationSetting.minorTotal;
        this.isSendEmail = notificationSetting.isSendEmail != null && notificationSetting.isSendEmail == 1 ? Boolean.TRUE : Boolean.FALSE;
        this.isSendNotifyList = notificationSetting.isSendNotifyList != null && notificationSetting.isSendNotifyList == 1 ? Boolean.TRUE : Boolean.FALSE;
        this.isSendNotify = notificationSetting.isSendNotify != null && notificationSetting.isSendNotify == 1 ? Boolean.TRUE : Boolean.FALSE;
        this.isSendSms = notificationSetting.isSendSms != null && notificationSetting.isSendSms == 1 ? Boolean.TRUE : Boolean.FALSE;
        this.userId = notificationSetting.userId;
        if (notificationSetting.active != null && notificationSetting.active == 1) {
            this.isActive = Boolean.TRUE;
        } else {
            this.isActive = Boolean.FALSE;
        }
        if (notificationSetting.deviceGroups == null) {
            this.deviceGroups = new HashSet<>();
        } else {
            this.deviceGroups = notificationSetting.deviceGroups;
        }
    }

    public NotificationSetting convertToNotificationSetting() {
        NotificationSetting notificationSetting = new NotificationSetting();
        notificationSetting.id = this.id;
        notificationSetting.alarmTotal = this.alarmTotal;
        notificationSetting.criticalTotal = this.criticalTotal;
        notificationSetting.majorTotal = this.majorTotal;
        notificationSetting.minorTotal = this.minorTotal;
        notificationSetting.userId = this.userId;
        notificationSetting.isSendEmail = this.isSendEmail ? 1 : 0;
        notificationSetting.isSendNotifyList = this.isSendNotifyList ? 1 : 0;
        notificationSetting.isSendNotify = this.isSendNotify ? 1 : 0;
        notificationSetting.isSendSms = this.isSendSms ? 1 : 0;
        notificationSetting.active = this.isActive ? 1 : 0;
        if (this.deviceGroups == null) {
            notificationSetting.deviceGroups = new HashSet<>();
        } else {
            notificationSetting.deviceGroups = this.deviceGroups;
        }
        return notificationSetting;
    }

}
