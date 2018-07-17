/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import java.io.Serializable;

/**
 *
 * @author kiendt
 */
public class QosAlarmDetail implements Serializable {

    public long qosKpiId;
    public String qosKpiIndex;
    public String qosKpiType;
    public String deviceId;
    //
    public String serialNumber;
    //
    public long deviceGroups;
    public Long qosKpiValue;
    public String qosKpiValueText;
    public String qosKpiSeverity;
    public String qosKpiCondition;
    //
    //1.SUM
    //2.AVG
    //3.MAX
    //4.MIN
    public long qosDeviceGroupType;
    //

    @JestId
    public String _id;

    @SerializedName("@timestamp")
    public String timestamp;
}
