package vn.ssdc.vnpt.selfCare.model.searchForm;

import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import vn.ssdc.vnpt.alarm.model.AlarmType;
import vn.ssdc.vnpt.selfCare.model.SCAlarmSetting;

/**
 * Created by THANHLX on 12/11/2017.
 */
public class SCAlarmSettingSearchForm {
    public Integer limit;
    public Integer page;
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
    @ApiModelProperty(example = "Realtime | Changes | Passive")
    public String monitoringType;
    public Long deviceGroupId;

}
