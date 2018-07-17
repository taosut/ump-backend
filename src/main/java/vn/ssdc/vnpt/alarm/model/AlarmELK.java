/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.alarm.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import java.io.Serializable;
import java.util.Set;
import vn.ssdc.vnpt.devices.model.DeviceGroup;

/**
 *
 * @author Admin
 */
public class AlarmELK implements Serializable {

    public String device_id;
    public long alarm_type_id;
    public String alarm_type_name;
    public String alarmName;
    public long raised;
    public String status;
    public String description;
    public String severity;
    public String device_groups;

    @JestId
    public String _id;

    @SerializedName("@timestamp")
    public String timestamp;

    @Override
    public String toString() {
        return "AlarmELK{" + "device_id=" + device_id + ", alarm_type_id=" + alarm_type_id + ", alarm_type_name=" + alarm_type_name + ", alarm_name=" + alarmName + ", raised=" + raised + ", status=" + status + ", description=" + description + ", severity=" + severity + ", device_groups=" + device_groups + ", _id=" + _id + ", timestamp=" + timestamp + '}';
    }

}
