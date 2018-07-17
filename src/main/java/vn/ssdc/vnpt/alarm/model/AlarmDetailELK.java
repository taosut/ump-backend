/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.alarm.model;

import com.google.gson.annotations.SerializedName;
import io.searchbox.annotations.JestId;
import java.util.Set;
import vn.ssdc.vnpt.devices.model.DeviceGroup;

/**
 *
 * @author Admin
 */
public class AlarmDetailELK {

    public long alarm_type_id;
    public String alarm_type;
    public String alarm_type_name;
    public String device_id;
    public String device_groups;
    public long raised;

    @JestId
    public String _id;

    @SerializedName("@timestamp")
    public String timestamp;

    @Override
    public String toString() {
        return "AlarmDetailELK{" + "alarm_type_id=" + alarm_type_id + ", alarm_type=" + alarm_type + ", alarm_type_name=" + alarm_type_name + ", device_id=" + device_id + ", device_groups=" + device_groups + ", raised=" + raised + ", _id=" + _id + ", timestamp=" + timestamp + '}';
    }
    
    
}
