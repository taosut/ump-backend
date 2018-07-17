/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.selfCare.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import org.apache.commons.lang3.StringUtils;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.devices.model.DeviceGroup;
import vn.ssdc.vnpt.qos.model.QosAlarmDeviceNew;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/**
 *
 * @author kiendt
 */
public class SCQosAlarmDevice implements Serializable {
    public long qosKpiId;
    public String qosKpiIndex;
    public String qosKpiType;
    public String deviceId;
    //
    public String serialNumber;
    //
    public Set<Long> deviceGroups;
//    public Long qosKpiValue;
//    public String qosKpiValueText;

    public String kpiSeverity;
    public String condition;
    //
    //1.SUM
    //2.AVG
    //3.MAX
    //4.MIN
    public long qosDeviceGroupType;
    //
    public String status;
    public long kpiSeverityNumber;

    public String id;
    public Date timestamp;
    public String qosKpiValue;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    TimeZone tz = TimeZone.getTimeZone("GMT+0");
    public SCQosAlarmDevice(QosAlarmDeviceNew qosAlarmDeviceNew) throws ParseException {

        this.qosKpiId = qosAlarmDeviceNew.qosKpiId;
        this.qosKpiIndex = qosAlarmDeviceNew.qosKpiIndex;
        this.qosKpiType = qosAlarmDeviceNew.qosKpiType;
        this.deviceId = qosAlarmDeviceNew.deviceId;
        //
        this.serialNumber = qosAlarmDeviceNew.serialNumber;
        //
        this.deviceGroups = qosAlarmDeviceNew.deviceGroups;
//    public Long qosKpiValue;
//    public String qosKpiValueText;

        this.kpiSeverity = qosAlarmDeviceNew.kpiSeverity;
        this.condition = qosAlarmDeviceNew.condition;
        //
        //1.SUM
        //2.AVG
        //3.MAX
        //4.MIN
        this.qosDeviceGroupType = qosAlarmDeviceNew.qosDeviceGroupType;
        //
        this.status = qosAlarmDeviceNew.status;
        this.kpiSeverityNumber = qosAlarmDeviceNew.kpiSeverityNumber;
        this.id = qosAlarmDeviceNew._id;
        //
        sdf.setTimeZone(tz);
        //
        this.timestamp = sdf.parse(qosAlarmDeviceNew.timestamp) ;

        if(qosAlarmDeviceNew.qosKpiValue!=null){
            this.qosKpiValue = String.valueOf(qosAlarmDeviceNew.qosKpiValue);
        }else if (qosAlarmDeviceNew.qosKpiValueText!=null){
            this.qosKpiValue = qosAlarmDeviceNew.qosKpiValueText;
        }

    }
}