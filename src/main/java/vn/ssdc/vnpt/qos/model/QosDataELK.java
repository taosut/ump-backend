package vn.ssdc.vnpt.qos.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;

import java.io.Serializable;

public class QosDataELK implements Serializable {

    public long qosKpiId;
    public String qosKpiIndex;
    public String qosKpiType;
    public String deviceId;
    public long deviceGroups;
    public String kpiSeverity;
    public String qosKpiValue;
    //
    public String status;
    //
    public String serialNumber;
    public String condition;
    public String qosKpiValueText;
    //
    public long kpiSeverityNumber;
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
