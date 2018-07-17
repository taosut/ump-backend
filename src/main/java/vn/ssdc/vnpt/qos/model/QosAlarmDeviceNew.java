/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

import java.io.Serializable;
import java.util.Set;

/**
 *
 * @author kiendt
 */
public class QosAlarmDeviceNew implements Serializable {
    public long qosKpiId;
    public String qosKpiIndex;
    public String qosKpiType;
    public String deviceId;
    //
    public String serialNumber;
    //
    public Set<Long> deviceGroups;
    public Long qosKpiValue;
    public String qosKpiValueText;
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

    @JestId
    public String _id;

    @SerializedName("@timestamp")
    public String timestamp;
}