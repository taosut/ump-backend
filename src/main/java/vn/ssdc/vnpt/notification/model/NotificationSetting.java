/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.notification.model;

import java.util.HashSet;
import java.util.Set;
import vn.vnpt.ssdc.jdbc.SsdcEntity;

/**
 *
 * @author kiendt
 */
public class NotificationSetting extends SsdcEntity<Long> {

    public Integer alarmTotal;
    public Integer criticalTotal;
    public Integer majorTotal;
    public Integer minorTotal;
    public Integer isSendEmail;
    public Integer isSendNotifyList;
    public Integer isSendNotify;
    public Integer isSendSms;
    public Long userId;
    public Set<Long> deviceGroups;
    public Integer active;
    public String timeCountTotal;
    public String timeCountCritical;
    public String timeCountMajor;
    public String timeCountMinor;

    public NotificationSetting(Long userId) {
        this.alarmTotal = null;
        this.criticalTotal = null;
        this.majorTotal = null;
        this.minorTotal = null;
        this.isSendEmail = null;
        this.isSendNotifyList = null;
        this.isSendNotify = null;
        this.isSendSms = null;
        this.userId = userId;
        this.deviceGroups = new HashSet<>();
        this.active = 0;
        this.timeCountTotal = null;
        this.timeCountCritical = null;
        this.timeCountMajor = null;
        this.timeCountMinor = null;
    }

    public NotificationSetting() {
    }

}
