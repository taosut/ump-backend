package vn.ssdc.vnpt.selfCare.model;

import io.swagger.annotations.ApiModelProperty;
import org.apache.xpath.operations.Bool;
import vn.ssdc.vnpt.alarm.model.AlarmType;
import vn.ssdc.vnpt.devices.model.DeviceGroup;

import java.util.Map;
import java.util.Set;

/**
 * Created by THANHLX on 12/11/2017.
 */
public class SCAlarmSetting {

    public Long id;
    @ApiModelProperty(example = "REQUEST_FAIL | CONFIGURATION_FAIL | UPDATE_FIRMWARE_FAIL | REBOOT_FAIL | FACTORY_RESET_FAIL | PARAMETER_VALUE")
    public String type;
    @ApiModelProperty(example = "Request failed | Configuration device failed | Update firmware failed | Reboot fail | Factory reset failed | ...")
    public String name;
    @ApiModelProperty(example = "Info | Warning | Major | Minor | Critical")
    public String severity;
    public Boolean isNotified;
    public Boolean isMonitored;
    public Integer aggregatedVolume;
    @ApiModelProperty(example = "OFF | EMAIL | SMS")
    public String notifyAggregated;
    public Map<String, String> parameterValues;
    @ApiModelProperty(example = "Realtime | Changes | Passive")
    public String monitoringType;
    public Integer intervalTime;

    @Override
    public String toString() {
        return "SCAlarmSetting{" + "id=" + id + ", type=" + type + ", name=" + name + ", severity=" + severity + ", isNotified=" + isNotified + ", isMonitored=" + isMonitored + ", aggregatedVolume=" + aggregatedVolume + ", notifyAggregated=" + notifyAggregated + ", parameterValues=" + parameterValues + ", monitoringType=" + monitoringType + ", intervalTime=" + intervalTime + '}';
    }

}
