/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.qos.model;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import java.io.Serializable;
import java.util.Set;

/**
 *
 * @author kiendt
 */
public class QosKpiDataELK implements Serializable {

    public String deviceId;
    public Long deviceGroupId;
    public Long graphId;
    public Long kpiId;
    public String kpiIndex;
    public Object value;
    public String textValue;

    public String manufacture;
    public String modelName;
    public String firmwareVersion;
    public String serialNumber;

    @JestId
    public String _id;

    @SerializedName("@timestamp")
    public String timestamp;

    @Override
    public String toString() {
        return "QosKpiDataELK{" + "deviceId=" + deviceId + ", deviceGroupId=" + deviceGroupId + ", graphId=" + graphId + ", kpiId=" + kpiId + ", kpiIndex=" + kpiIndex + ", value=" + value + ", textValue=" + textValue + ", manufacture=" + manufacture + ", modelName=" + modelName + ", firmwareVersion=" + firmwareVersion + ", _id=" + _id + ", timestamp=" + timestamp + '}';
    }

    public String toMessage() {
        return new Gson().toJson(this);
    }

}
