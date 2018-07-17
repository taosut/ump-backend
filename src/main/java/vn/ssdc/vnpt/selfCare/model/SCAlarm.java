package vn.ssdc.vnpt.selfCare.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import vn.ssdc.vnpt.alarm.model.Alarm;
import vn.ssdc.vnpt.devices.model.DeviceGroup;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by THANHLX on 11/23/2017.
 */
public class SCAlarm {
    public String id;
    public String deviceId;
    public long alarmTypeId;
    public String alarmTypeName;
    public String alarmName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Date raisedTime;
    public String status;
    public String description;
    public String severity;
    @ApiModelProperty(example = "Group Name 1, Group Name 2, Group Name 3, Group Name 4")
    public String deviceGroupNames;

    public SCAlarm(Alarm alarm) {
      this.id = alarm._uid;
      this.deviceId = alarm.deviceId;
      this.alarmTypeId = alarm.alarmTypeId;
      this.alarmTypeName = alarm.alarmTypeName;
      this.alarmName = alarm.alarmName;
      this.raisedTime = new Date(alarm.raised);
      this.status = alarm.status;
      this.description = alarm.description;
      this.severity = alarm.severity;
      Set<String> deviceGroupNames = new HashSet<>();
      for(DeviceGroup deviceGroup : alarm.deviceGroups){
        deviceGroupNames.add(deviceGroup.name);
      }
      this.deviceGroupNames = StringUtils.join(deviceGroupNames,",");
    }
}
